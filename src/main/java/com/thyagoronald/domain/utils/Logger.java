package com.thyagoronald.domain.utils;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {
    private static final String LOG_FILE = "app.log";
    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void info(String message) {
        String logMsg = String.format("[%s] [INFO] %s", dtf.format(LocalDateTime.now()), message);
        System.out.println(logMsg);
        writeToFile(logMsg);
    }

    public static void error(String message) {
        String logMsg = String.format("[%s] [ERROR] %s", dtf.format(LocalDateTime.now()), message);
        System.err.println(logMsg);
        writeToFile(logMsg);
    }

    private static void writeToFile(String message) {
        try (PrintWriter out = new PrintWriter(new FileWriter(LOG_FILE, true))) {
            out.println(message);
        } catch (IOException e) {
            System.err.println("[LOGGER ERROR] Falha ao escrever no arquivo de log: " + e.getMessage());
        }
    }
}
