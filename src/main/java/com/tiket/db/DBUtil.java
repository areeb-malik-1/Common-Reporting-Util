package com.tiket.db;

import com.google.gson.Gson;
import com.tiket.email.Convertor;
import com.tiket.io.PropertiesReader;
import com.tiket.model.*;
import com.tiket.email.EmailProcessor;
import com.tiket.email.RawReport;
import com.tiket.email.Report;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.File;
import java.nio.file.Files;
import java.sql.*;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

@Slf4j
public class DBUtil {

    private static final Properties PROPERTIES = PropertiesReader.read("src/main/resources/config.properties");
    private static final String DB_URL = PROPERTIES.getProperty("db.url").trim();
    private static final String DB_USERNAME = PROPERTIES.getProperty("db.username");
    private static final String DB_PASSWORD = PROPERTIES.getProperty("db.password");
    private static final String DB_STATEMENT = "SELECT * FROM results";

    private static final Gson gson = new Gson();

    public static void main(String[] args) {

        String vertical = System.getProperty("vertical").toLowerCase().strip().replaceAll(" ", "");
        String tribe = System.getProperty("tribe").toLowerCase().strip().replaceAll(" ", "");
        String module = System.getProperty("module").toLowerCase().strip().replaceAll(" ", "");
        Platform platform = Platform.parse(System.getProperty("platform"));
        TestType testType = TestType.parse(System.getProperty("testtype"));
        Environment environment = Environment.parse(System.getProperty("environment"));
        String runID = System.getProperty("runID").strip();

        log.info("Running with following params");
        log.info("vertical: " + vertical);
        log.info("tribe: " + tribe);
        log.info("module: " + module);
        log.info("platform: " + platform);
        log.info("testtype: " + testType);
        log.info("environment: " + environment);
        log.info("runID: " + runID);

        Report report = createReport(vertical, tribe, module, platform, testType, environment, runID);
        log.info(gson.toJson(report));

        String htmlReportContent = createHtmlReportFile(report);
        EmailProcessor.processEmail(htmlReportContent);
    }

    @SneakyThrows
    private static String createHtmlReportFile(Report report) {

        final File reportDirectory = new File("./report");
        if(!reportDirectory.exists()) {
            Files.createDirectories(reportDirectory.toPath());
        }

        final File reportFile = new File("./report/report.html");
        Files.deleteIfExists(reportFile.toPath());
        Files.createFile(reportFile.toPath());

        String content = Convertor.fromJson(gson.toJson(report));
        Files.write(reportFile.toPath(), content.getBytes());

        return content;
    }

    private static Report createReport(
            String vertical,
            String tribe,
            String module,
            Platform platform,
            TestType testtype,
            Environment environment,
            String runID
    ) {
        List<DBEntry> latestEntries = getLatestEntries(vertical, tribe, module, platform, testtype, environment, runID);

        RawReport rawReport = new RawReport();
        latestEntries.stream()
                .peek(dbEntry -> log.debug(dbEntry.toString()))
                .forEach(rawReport::add);
        log.debug(gson.toJson(rawReport));
        Report report = new Report();
        return report.fromRawReport(rawReport);
    }

    private static List<DBEntry> getLatestEntries(
            String vertical,
            String tribe,
            String module,
            Platform platform,
            TestType testtype,
            Environment environment,
            String runID
    ) {
        Comparator<DBEntry> sortByTestRailID = Comparator.comparing(DBEntry::testRailID);
        Comparator<DBEntry> sortByTimestamp = Comparator.comparing(DBEntry::timestamp);
        Comparator<DBEntry> dbEntryComparator = sortByTestRailID.thenComparing(sortByTimestamp);
        List<DBEntry> entries = getFilteredEntries(vertical, tribe, module, platform, testtype, environment, runID)
                .sorted(dbEntryComparator)
                .toList();
        log.debug("sorted entries: " + gson.toJson(entries));
        entries.forEach(e -> log.debug("sortedEntry: " + e.testRailID()));
        List<DBEntry> latestEntries = new ArrayList<>();
        for(int i=0; i<entries.size()-1; i++) {
            DBEntry entryI = entries.get(i);
            DBEntry entryJ = entries.get(i+1);
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

    private static Stream<DBEntry> getFilteredEntries(
            String vertical,
            String tribe,
            String module,
            Platform platform,
            TestType testtype,
            Environment environment,
            String runID
    ) {
        Stream<DBEntry> latestEntries = allEntries(runID);
        Predicate<DBEntry> matchVertical = e -> vertical.equalsIgnoreCase("all") || e.verticalName().replaceAll(" ", "").equalsIgnoreCase(vertical);
        Predicate<DBEntry> matchTribe = e -> tribe.equalsIgnoreCase("all") || e.tribeName().replaceAll(" ", "").equalsIgnoreCase(tribe);
        Predicate<DBEntry> matchModule = e -> module.equalsIgnoreCase("all") || e.moduleName().replaceAll(" ", "").equalsIgnoreCase(module);
        Predicate<DBEntry> matchPlatform = e -> platform.equals(Platform.ALL) || e.platform().equals(platform);
        Predicate<DBEntry> matchTestType = e -> testtype.equals(TestType.ALL) || e.testType().equals(testtype);
        Predicate<DBEntry> matchEnvironment = e -> environment.equals(Environment.ALL) || e.environment().equals(environment);
        Predicate<DBEntry> matchDate = e -> System.currentTimeMillis() - e.timestamp() < 1000 * 60 * 60 * 24;
        return latestEntries
                .filter(matchVertical)
                .filter(matchTribe)
                .filter(matchModule)
                .filter(matchPlatform)
                .filter(matchTestType)
                .filter(matchEnvironment)
                .filter(matchDate);
    }

    private static Stream<DBEntry> allEntries(String runID) {
        List<DBEntry> entries = new ArrayList<>();
        ResultSet resultSet = executeQuery(runID);
        while (true) {
            try {
                if (!resultSet.next()) break;
                DBEntry entry = new DBEntry(
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
