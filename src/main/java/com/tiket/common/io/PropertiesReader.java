package com.tiket.common.io;

import lombok.SneakyThrows;

import java.io.FileReader;
import java.util.Properties;

public class PropertiesReader {

    @SneakyThrows
    public static Properties read(String propertiesFile) {
        FileReader reader = new FileReader(propertiesFile);
        Properties properties = new Properties();
        properties.load(reader);
        return properties;
    }
}