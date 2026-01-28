package com.thyagoronald.domain.utils;

public class ProtocolUtil {
    private ProtocolUtil() {
    }

    public static String getData(String dataLine, String prefix) {
        return dataLine.replace(prefix, "").trim();
    }
}
