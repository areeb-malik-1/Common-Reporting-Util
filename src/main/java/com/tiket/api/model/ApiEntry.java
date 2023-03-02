package com.tiket.api.model;

import com.tiket.common.model.Environment;
import com.tiket.common.model.Status;
import com.tiket.common.model.TestType;

public record ApiEntry (
    Status status,
    String verticalName,
    String tribeName,
    String moduleName,
    TestType testType,
    Environment environment,
    String testID,
    long timestamp
) {
}
