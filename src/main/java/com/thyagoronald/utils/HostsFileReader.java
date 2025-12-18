package com.thyagoronald.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class HostsFileReader {
    public static List<String> readHosts(String filePath) throws IOException {
        return Files.lines(Paths.get(filePath))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .collect(Collectors.toList());
    }
}
