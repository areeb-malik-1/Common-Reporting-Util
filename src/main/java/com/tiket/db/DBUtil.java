package com.tiket.db;

import com.google.gson.Gson;
import com.tiket.io.PropertiesReader;
import com.tiket.model.*;
import com.tiket.report.RawReport;
import com.tiket.report.Report;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.BasicConfigurator;

import java.sql.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

@Slf4j
public class DBUtil {

    private static final Properties PROPERTIES = PropertiesReader.read("src/main/resources/config.properties");
    private static final String DB_URL = PROPERTIES.getProperty("db.url");
    private static final String DB_USERNAME = PROPERTIES.getProperty("db.username");
    private static final String DB_PASSWORD = PROPERTIES.getProperty("db.password");
    private static final String DB_STATEMENT = "SELECT * FROM results";

    private static final Gson gson = new Gson();

    public static void main(String[] args) {

        BasicConfigurator.configure();

        String vertical = System.getProperty("vertical");
        String tribe = System.getProperty("tribe");
        String module = System.getProperty("module");
        Platform platform = Platform.parse(System.getProperty("platform"));
        TestType testType = TestType.parse(System.getProperty("testtype"));
        Environment environment = Environment.parse(System.getProperty("environment"));
        String runID = System.getProperty("runID");

        Report report = createReport(vertical, tribe, module, platform, testType, environment, runID);
        System.out.println(gson.toJson(report));
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
        List<DBEntry> latestEntries = getLatestEntries(runID);
        List<DBEntry> filteredEntries = latestEntries.stream()
                .filter(e -> vertical.equalsIgnoreCase("all") || e.verticalName().equalsIgnoreCase(vertical))
                .filter(e -> tribe.equalsIgnoreCase("all") || e.tribeName().equalsIgnoreCase(tribe))
                .filter(e -> module.equalsIgnoreCase("all") || e.moduleName().equalsIgnoreCase(module))
                .filter(e -> platform.equals(Platform.ALL) || e.platform().equals(platform))
                .filter(e -> testtype.equals(TestType.ALL) || e.testType().equals(testtype))
                .filter(e -> environment.equals(Environment.ALL) || e.environment().equals(environment))
                .toList();

        RawReport rawReport = new RawReport();
        filteredEntries.stream()
                .peek(System.out::println)
                .forEach(rawReport::add);
        System.out.println(gson.toJson(rawReport));
        Report report = new Report();
        return report.fromRawReport(rawReport);
    }

    private static List<DBEntry> getLatestEntries(String runID) {
        List<DBEntry> entries = allEntries(runID).stream()
                .sorted(Comparator.comparing(DBEntry::testRailID))
                .sorted(Comparator.comparing(DBEntry::timestamp))
                .toList();
        System.out.println("sorted entries: " + gson.toJson(entries));
        List<DBEntry> latestEntries = new ArrayList<>();
        for(int i=0; i<entries.size()-1; i++) {
            DBEntry entryI = entries.get(i);
            DBEntry entryJ = entries.get(i+1);
            String testI = entryI.testRailID();
            String testJ = entryJ.testRailID();
            if(!testI.equals(testJ)) {
                latestEntries.add(entryI);
            }
        }
        if(!entries.get(entries.size()-1).equals(entries.get(entries.size()-2))) {
            latestEntries.add(entries.get(entries.size()-1));
        }
        System.out.println("latest entries: " + gson.toJson(latestEntries));
        return latestEntries;
    }

    private static List<DBEntry> allEntries(String runID) {
        List<DBEntry> entries = new ArrayList<>();
        ResultSet resultSet = executeQuery(runID);
        while (true) {
            try {
                if (!resultSet.next()) break;
                DBEntry entry = new DBEntry(
                        Status.parse(resultSet.getString("status").trim()),
                        resultSet.getString("vertical").trim(),
                        resultSet.getString("tribe").trim(),
                        resultSet.getString("module").trim(),
                        Platform.parse(resultSet.getString("platform").trim()),
                        TestType.parse(resultSet.getString("testtype").trim()),
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
        System.out.println(gson.toJson(entries));
        return entries;
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
