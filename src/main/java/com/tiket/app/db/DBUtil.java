package com.tiket.app.db;

import com.google.gson.Gson;
import com.tiket.app.model.AppEntry;
import com.tiket.app.model.Platform;
import com.tiket.app.reporting.AppReport;
import com.tiket.app.reporting.RawAppReport;
import com.tiket.common.db.CommonDBUtil;
import com.tiket.common.email.EmailProcessor;
import com.tiket.common.io.PropertiesReader;
import com.tiket.common.model.Environment;
import com.tiket.common.model.Status;
import com.tiket.common.model.TestType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.function.Predicate;
import java.util.stream.Stream;

@Slf4j
public class DBUtil {

    private static final Properties PROPERTIES = PropertiesReader.read("src/main/resources/app.config.properties");
    private static final String DB_URL = PROPERTIES.getProperty("db.url").trim();
    private static final String DB_USERNAME = PROPERTIES.getProperty("db.username");
    private static final String DB_PASSWORD = PROPERTIES.getProperty("db.password");
    private static final String DB_STATEMENT = """
            SELECT * 
            FROM public.results AS "RESULTS"
            WHERE "RESULTS"."timestamp" > %s
            """.formatted(System.currentTimeMillis()-1000*60*60*24);

    private static final Gson gson = new Gson();

    public static void main(String[] args) throws ClassNotFoundException {

        Class.forName("com.mysql.jdbc.Driver");

        String project = System.getProperty("project");
        String vertical = System.getProperty("vertical").toLowerCase().strip().replaceAll(" ", "");
        String tribe = System.getProperty("tribe").toLowerCase().strip().replaceAll(" ", "");
        String module = System.getProperty("module").toLowerCase().strip().replaceAll(" ", "");
        Platform platform = Platform.parse(System.getProperty("platform"));
        TestType testType = TestType.parse(System.getProperty("testtype"));
        Environment environment = Environment.parse(System.getProperty("environment"));
        String runID = System.getProperty("runID").strip();

        log.info("Running with following params");
        log.info("project: " + project);
        log.info("vertical: " + vertical);
        log.info("tribe: " + tribe);
        log.info("module: " + module);
        log.info("platform: " + platform);
        log.info("testtype: " + testType);
        log.info("environment: " + environment);
        log.info("runID: " + runID);

        AppReport appReport = createReport(vertical, tribe, module, platform, testType, environment, runID);
        log.info(gson.toJson(appReport));

        String htmlReportContent = CommonDBUtil.createHtmlReportFile(appReport);
        EmailProcessor.processEmail(htmlReportContent);
    }

    private static AppReport createReport(
            String vertical,
            String tribe,
            String module,
            Platform platform,
            TestType testtype,
            Environment environment,
            String runID
    ) {
        List<AppEntry> latestEntries = getLatestEntries(vertical, tribe, module, platform, testtype, environment, runID);
        RawAppReport rawAppReport = new RawAppReport();
        latestEntries.stream()
                .peek(dbEntry -> log.debug(dbEntry.toString()))
                .forEach(rawAppReport::add);
        log.debug(gson.toJson(rawAppReport));
        AppReport appReport = new AppReport();
        return appReport.fromRawReport(rawAppReport);
    }

    private static List<AppEntry> getLatestEntries(
            String vertical,
            String tribe,
            String module,
            Platform platform,
            TestType testtype,
            Environment environment,
            String runID
    ) {
        Comparator<AppEntry> sortByTestRailID = Comparator.comparing(AppEntry::testRailID);
        Comparator<AppEntry> sortByTimestamp = Comparator.comparing(AppEntry::timestamp);
        Comparator<AppEntry> dbEntryComparator = sortByTestRailID.thenComparing(sortByTimestamp);
        List<AppEntry> entries = getFilteredEntries(vertical, tribe, module, platform, testtype, environment, runID)
                .sorted(dbEntryComparator)
                .toList();
        log.debug("sorted entries: " + gson.toJson(entries));
        entries.forEach(e -> log.debug("sortedEntry: " + e.testRailID()));
        List<AppEntry> latestEntries = new ArrayList<>();
        for(int i=0; i<entries.size()-1; i++) {
            AppEntry entryI = entries.get(i);
            AppEntry entryJ = entries.get(i+1);
            String testI = entryI.testRailID();
            String testJ = entryJ.testRailID();
            if(!testI.equalsIgnoreCase(testJ)) {
                latestEntries.add(entryI);
                log.debug("i: " + i + ", i+1: " + (i+1) );
                log.debug("ei: " + entryI.testRailID());
                log.debug("ej: " + entryJ.testRailID());
            }
        }
        if(!entries.get(entries.size()-1).equals(entries.get(entries.size()-2))) {
            latestEntries.add(entries.get(entries.size()-1));
        }
        log.debug("latest entries: " + gson.toJson(latestEntries));
        return latestEntries;
    }

    private static Stream<AppEntry> getFilteredEntries(
            String vertical,
            String tribe,
            String module,
            Platform platform,
            TestType testtype,
            Environment environment,
            String runID
    ) {
        Stream<AppEntry> latestEntries = allEntries(runID);
        Predicate<AppEntry> matchVertical = e -> vertical.equalsIgnoreCase("all") || e.verticalName().replaceAll(" ", "").equalsIgnoreCase(vertical);
        Predicate<AppEntry> matchTribe = e -> tribe.equalsIgnoreCase("all") || e.tribeName().replaceAll(" ", "").equalsIgnoreCase(tribe);
        Predicate<AppEntry> matchModule = e -> module.equalsIgnoreCase("all") || e.moduleName().replaceAll(" ", "").equalsIgnoreCase(module);
        Predicate<AppEntry> matchPlatform = e -> platform.equals(Platform.ALL) || e.platform().equals(platform);
        Predicate<AppEntry> matchTestType = e -> testtype.equals(TestType.ALL) || e.testType().equals(testtype);
        Predicate<AppEntry> matchEnvironment = e -> environment.equals(Environment.ALL) || e.environment().equals(environment);
        Predicate<AppEntry> matchDate = e -> System.currentTimeMillis() - e.timestamp() < 1000 * 60 * 60 * 24;
        return latestEntries
                .filter(matchVertical)
                .filter(matchTribe)
                .filter(matchModule)
                .filter(matchPlatform)
                .filter(matchTestType)
                .filter(matchEnvironment)
                .filter(matchDate);
    }

    private static Stream<AppEntry> allEntries(String runID) {
        List<AppEntry> entries = new ArrayList<>();
        ResultSet resultSet = executeQuery(runID);
        while (true) {
            try {
                if (!resultSet.next()) break;
                AppEntry entry = new AppEntry(
                        Status.parse(resultSet.getString("status").trim()),
                        resultSet.getString("vertical_name").trim(),
                        resultSet.getString("tribe_name").trim(),
                        resultSet.getString("module_name").trim(),
                        Platform.parse(resultSet.getString("platform").trim()),
                        TestType.parse(resultSet.getString("test_type").trim()),
                        Environment.parse(resultSet.getString("environment").trim()),
                        resultSet.getString("testrail_id").trim(),
                        resultSet.getString("run_id").trim(),
                        resultSet.getLong("timestamp")
                );
                entries.add(entry);
            } catch (SQLException e) {
                log.error(ExceptionUtils.getStackTrace(e));
                throw new RuntimeException(e);
            }

        }
        log.debug(gson.toJson(entries));
        return entries.stream();
    }

    private static ResultSet executeQuery(String runID) {
        try(Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD)) {
            PreparedStatement statement = connection.prepareStatement(DB_STATEMENT);
            //statement.setString(1, runID);
            return statement.executeQuery();
        } catch (SQLException e) {
            log.error(ExceptionUtils.getStackTrace(e));
            throw new RuntimeException(e);
        }
    }
}
