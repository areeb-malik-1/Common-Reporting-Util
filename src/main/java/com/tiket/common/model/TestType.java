package com.tiket.common.model;

import org.apache.commons.lang3.NotImplementedException;

public enum TestType {

    ALL, SANITY, REGRESSION;

    public static TestType parse(String testType) {
        switch (testType.toLowerCase()) {
            case "all" -> {return ALL;}
            case "sanity" -> {return SANITY;}
            case "regression" -> {return REGRESSION;}
            default -> throw new NotImplementedException(String.format("Given test type: %s is not supported", testType));
        }
    }
}
