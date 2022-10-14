package com.tiket.model;

import org.apache.commons.lang3.NotImplementedException;

public enum Environment {
    ALL, STAGING, PRODUCTION;

    public static Environment parse(String env) {
        switch (env.toLowerCase()) {
            case "all" -> {return ALL;}
            case "staging" -> {return STAGING;}
            case "production" -> {return PRODUCTION;}
            default -> throw new NotImplementedException(String.format("Given environment: %s is not supported", env));
        }
    }
}