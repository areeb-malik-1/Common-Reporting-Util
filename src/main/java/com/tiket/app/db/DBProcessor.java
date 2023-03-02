package com.tiket.app.db;

import com.tiket.common.io.PropertiesReader;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class DBProcessor {

    private static final ExecutorService pool = Executors.newCachedThreadPool();
    private static final Properties PROPERTIES = PropertiesReader.read("src/main/resources/app.config.properties");
    private static final String DB_URL = PROPERTIES.getProperty("db.url");
    private static final String DB_USERNAME = PROPERTIES.getProperty("db.username");
    private static final String DB_PASSWORD = PROPERTIES.getProperty("db.password");
    private static final String DB_STATEMENT = "INSERT INTO results(status, vertical, tribe, module, platform, testtype, environment, testrail_id, run_id, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    public static CompletableFuture<Boolean> sendResultAsync(int i) {
        return CompletableFuture.supplyAsync(() -> sendResult(i), pool);
    }

    private static boolean sendResult(int i) {

        try(Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD)) {
            PreparedStatement statement = connection.prepareStatement(DB_STATEMENT);
            statement.setString(1, "skip");
            statement.setString(2, "flight");
            statement.setString(3, "home");
            statement.setString(4, "dummy_module");
            statement.setString(5, "android");
            statement.setString(6, "sanity");
            statement.setString(7, "staging");
            statement.setString(8, "1234");
            statement.setString(9, "4321");
            statement.setLong(10, System.currentTimeMillis());

            log.info("Executed" + i);
            return statement.execute();
        } catch (SQLException e) {
            log.error(ExceptionUtils.getStackTrace(e));
        }

        return false;
    }

    public static void main(String[] args) {
        for(int i=0; i<11; i++) {
            sendResultAsync(i);
        }
        pool.shutdown();
    }
}
