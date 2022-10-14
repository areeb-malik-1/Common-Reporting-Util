package com.tiket.model;

import org.apache.commons.lang3.NotImplementedException;

public enum Status {
    PASS, FAIL, SKIP;

    public static Status parse(String status) {
        switch (status.toLowerCase()) {
            case "pass" -> {return PASS;}
            case "fail" -> {return FAIL;}
            case "skip" -> {return SKIP;}
            default -> throw new NotImplementedException(String.format("Given status: %s is not supported", status));
        }
    }
}