package com.thyagoronald.utils;

public class HttpResponse {
    private HttpResponse() {
    }

    public static String buildResponse(int statusCode, String statusMessage, String body) {
        StringBuilder response = new StringBuilder();
        response.append("HTTP/1.1 ").append(statusCode).append(" ").append(statusMessage).append("\r\n");
        response.append("Content-Length: ").append(body.length()).append("\r\n");
        response.append("Content-Type: text/plain\r\n");
        response.append("\r\n");
        response.append(body);
        return response.toString();
    }
}
