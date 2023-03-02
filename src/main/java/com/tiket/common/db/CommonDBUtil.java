package com.tiket.common.db;

import com.google.gson.Gson;
import com.tiket.common.email.Convertor;
import lombok.SneakyThrows;

import java.io.File;
import java.nio.file.Files;

public class CommonDBUtil {

    private static final Gson gson = new Gson();

    @SneakyThrows
    public static <T> String createHtmlReportFile(T report) {

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
}
