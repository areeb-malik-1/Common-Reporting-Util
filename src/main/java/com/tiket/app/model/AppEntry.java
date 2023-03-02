package com.tiket.app.model;

import com.tiket.common.model.Environment;
import com.tiket.common.model.Status;
import com.tiket.common.model.TestType;

public record AppEntry(
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