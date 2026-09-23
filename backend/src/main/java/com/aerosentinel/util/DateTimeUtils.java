package com.aerosentinel.util;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

public class DateTimeUtils {

    public static String nowIsoUtc() {
        return DateTimeFormatter.ISO_INSTANT.format(Instant.now());
    }

    public static Instant parseIso(String isoString) {
        return Instant.parse(isoString);
    }
}
