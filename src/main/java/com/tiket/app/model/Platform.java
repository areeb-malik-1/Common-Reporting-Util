package com.tiket.app.model;

import org.apache.commons.lang3.NotImplementedException;

public enum Platform {

    ALL, IOS, ANDROID;

    public static Platform parse(String platform) {
        switch (platform.toLowerCase()) {
            case "android" -> {return ANDROID;}
            case "ios" -> {return IOS;}
            case "all" -> {return ALL;}
            default -> throw new NotImplementedException(String.format("Given platform: %s is not supported", platform));
        }
    }
}
