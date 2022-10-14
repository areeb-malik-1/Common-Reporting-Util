package com.tiket.model;

public record DBEntry(
        Status status,
        String verticalName,
        String tribeName,
        String moduleName,
        Platform platform,
        TestType testType,
        Environment environment,
        String testRailID,
        String runID,
        long timestamp
) {
}