package com.tiket.api.db;

import com.google.gson.Gson;
import com.tiket.api.model.ApiEntry;
import com.tiket.api.reporting.ApiReport;
import com.tiket.api.reporting.RawApiReport;
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

    private static final Properties PROPERTIES = PropertiesReader.read("src/main/resources/api.config.properties");
    private static final String DB_URL = PROPERTIES.getProperty("db.url").trim();
    private static final String DB_USERNAME = PROPERTIES.getProperty("db.username");
    private static final String DB_PASSWORD = PROPERTIES.getProperty("db.password");
    private static final String DB_STATEMENT = """
            SELECT * 
            FROM public.results AS "RESULTS"
            WHERE "RESULTS"."timestamp" > %s
            """.formatted(System.currentTimeMillis()-1000*60*60*24);

    private static final Gson gson = new Gson();

    public static void main(String[] args) {

        String project = System.getProperty("project");
        String vertical = System.getProperty("vertical").toLowerCase().strip().replaceAll(" ", "");
        String tribe = System.getProperty("tribe").toLowerCase().strip().replaceAll(" ", "");
        String module = System.getProperty("module").toLowerCase().strip().replaceAll(" ", "");
        TestType testType = TestType.parse(System.getProperty("testtype"));
        Environment environment = Environment.parse(System.getProperty("environment"));

        log.info("Running with following params");
        log.info("project: " + project);
        log.info("vertical: " + vertical);
        log.info("tribe: " + tribe);
        log.info("module: " + module);
        log.info("testtype: " + testType);
        log.info("environment: " + environment);

        ApiReport apiReport = createReport(vertical, tribe, module, testType, environment);
        log.info(gson.toJson(apiReport));

        String htmlReportContent = CommonDBUtil.createHtmlReportFile(apiReport);
        EmailProcessor.processEmail(htmlReportContent);
    }

    private static ApiReport createReport(
            String vertical,
            String tribe,
            String module,
            TestType testType,
            Environment environment
    ) {
        List<ApiEntry> latestEntries = getLatestEntries(vertical, tribe, module, testType, environment);
        RawApiReport rawApiReport = new RawApiReport();
        latestEntries.stream()
                .peek(dbEntry -> log.debug(dbEntry.toString()))
                .forEach(rawApiReport::add);
        log.debug(gson.toJson(rawApiReport));
        ApiReport apiReport = new ApiReport();
        return apiReport.fromRawReport(rawApiReport);
    }
    
    private static List<ApiEntry> getLatestEntries(
            String vertical,
            String tribe,
            String module,
            TestType testType,
            Environment environment
    ) {
        Comparator<ApiEntry> sortByTestRailID = Comparator.comparing(ApiEntry::testID);
        Comparator<ApiEntry> sortByTimestamp = Comparator.comparing(ApiEntry::timestamp);
        Comparator<ApiEntry> dbEntryComparator = sortByTestRailID.thenComparing(sortByTimestamp);
        List<ApiEntry> entries = getFilteredEntries(vertical, tribe, module, testType, environment)
                .sorted(dbEntryComparator)
                .toList();
        log.debug("sorted entries: " + gson.toJson(entries));
        entries.forEach(e -> log.debug("sortedEntry: " + e.testID()));
        List<ApiEntry> latestEntries = new ArrayList<>();
        for(int i=0; i<entries.size()-1; i++) {
            ApiEntry entryI = entries.get(i);
            ApiEntry entryJ = entries.get(i+1);
            String testI = entryI.testID();
            String testJ = entryJ.testID();
            if(!testI.equalsIgnoreCase(testJ)) {
                latestEntries.add(entryI);
                log.debug("i: " + i + ", i+1: " + (i+1) );
                log.debug("ei: " + entryI.testID());
                log.debug("ej: " + entryJ.testID());
            }
        }
        if(!entries.get(entries.size()-1).equals(entries.get(entries.size()-2))) {
            latestEntries.add(entries.get(entries.size()-1));
        }
        log.debug("latest entries: " + gson.toJson(latestEntries));
        return latestEntries;
    }
    
    private static Stream<ApiEntry> getFilteredEntries(
            String vertical,
            String tribe,
            String module,
            TestType testType,
            Environment environment
    ) {

        Stream<ApiEntry> latestEntries = allEntries();
        Predicate<ApiEntry> matchVertical = e -> vertical.equalsIgnoreCase("all") || e.verticalName().replaceAll(" ", "").equalsIgnoreCase(vertical);
        Predicate<ApiEntry> matchTribe = e -> tribe.equalsIgnoreCase("all") || e.tribeName().replaceAll(" ", "").equalsIgnoreCase(tribe);
        Predicate<ApiEntry> matchModule = e -> module.equalsIgnoreCase("all") || e.moduleName().replaceAll(" ", "").equalsIgnoreCase(module);
        Predicate<ApiEntry> matchTestType = e -> testType.equals(TestType.ALL) || e.testType().equals(testType);
        Predicate<ApiEntry> matchEnvironment = e -> environment.equals(Environment.ALL) || e.environment().equals(environment);
        Predicate<ApiEntry> matchDate = e -> System.currentTimeMillis() - e.timestamp() < 1000 * 60 * 60 * 24;
        return latestEntries
                .filter(matchVertical)
                .filter(matchTribe)
                .filter(matchModule)
                .filter(matchTestType)
                .filter(matchEnvironment)
                .filter(matchDate);
    }

    private static Stream<ApiEntry> allEntries() {
        List<ApiEntry> entries = new ArrayList<>();
        ResultSet resultSet = executeQuery();
        while (true) {
            try {
                if (!resultSet.next()) break;
                ApiEntry entry = new ApiEntry(
                        Status.parse(resultSet.getString("status").trim()),
                        resultSet.getString("vertical_name").trim(),
                        resultSet.getString("tribe_name").trim(),
                        resultSet.getString("module_name").trim(),
                        TestType.parse(resultSet.getString("test_type").trim()),
                        Environment.parse(resultSet.getString("environment").trim()),
                        resultSet.getString("test_id").trim(),
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

    private static ResultSet executeQuery() {
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
