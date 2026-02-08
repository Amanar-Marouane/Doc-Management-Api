package com.example.backend.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppLogger {
    private static final Logger log = LoggerFactory.getLogger(AppLogger.class);

    // Basic lines and separators
    public static void line() {
        log.info("--------------------------------------------------");
    }

    // Logging methods with timestamps
    public static void log(String msg) {
        log.info(msg);
    }

    public static void info(String msg) {
        log.info(msg);
    }

    public static void warn(String msg) {
        log.warn(msg);
    }

    public static void error(String msg) {
        log.error(msg);
    }

    public static void success(String msg) {
        log.info(msg);
    }

    public static void alert(String msg) {
        log.warn(msg);
    }

    public static void debug(String msg) {
        log.debug(msg);
    }

    // Fancy header/footer for sections
    public static void header(String title) {
        line();
        log.info(">>> {} <<<", title.toUpperCase());
        line();
    }

    public static void footer(String title) {
        line();
        log.info("<<< END OF {} >>>", title.toUpperCase());
        line();
    }
}
