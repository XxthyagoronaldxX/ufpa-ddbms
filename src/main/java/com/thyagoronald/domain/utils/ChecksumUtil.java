package com.thyagoronald.domain.utils;

import java.util.zip.CRC32;

public class ChecksumUtil {
    private ChecksumUtil() {
    }

    public static long generateChecksum(String data) {
        CRC32 crc = new CRC32();
        crc.update(data.getBytes());
        return crc.getValue();
    }

    public static boolean validateChecksum(String data, long checksum) {
        return generateChecksum(data) == checksum;
    }
}
