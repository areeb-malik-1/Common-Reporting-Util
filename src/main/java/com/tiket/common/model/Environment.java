package com.tiket.common.model;

import org.apache.commons.lang3.NotImplementedException;

public enum Environment {
    ALL, STAGING, PRODUCTION, GK2;

    public static Environment parse(String env) {
        return switch (env.toLowerCase()) {
            case "all" -> ALL;
            case "staging" -> STAGING;
            case "production" -> PRODUCTION;
            case "gk2" -> GK2;
            default -> throw new NotImplementedException(String.format("Given environment: %s is not supported", env));
        };
    }
}