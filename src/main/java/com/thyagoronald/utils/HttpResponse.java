package com.thyagoronald.utils;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class HttpResponse {
    private static final Map<Integer, String> STATUS_MESSAGES = Map.of(
            200, "OK",
            400, "Bad Request",
            404, "Not Found",
            500, "Internal Server Error");

    private HttpResponse() {
    }

    public static String buildResponse(int statusCode, String body) {
        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
        StringBuilder response = new StringBuilder();
        response.append("HTTP/1.1 ").append(statusCode).append(" ");
        response.append(STATUS_MESSAGES.getOrDefault(statusCode, "")).append("\r\n");
        response.append("Content-Length: ").append(bodyBytes.length).append("\r\n");
        response.append("Content-Type: text/plain\r\n");
        response.append("Connection: ").append("close").append("\r\n");
        response.append("\r\n");
        response.append(body);
        return response.toString();
    }
}
