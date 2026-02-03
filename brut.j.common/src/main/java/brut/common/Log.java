/*
 *  Copyright (C) 2010 Ryszard Wiśniewski <brut.alll@gmail.com>
 *  Copyright (C) 2010 Connor Tumbleson <connor.tumbleson@gmail.com>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package brut.common;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Log {
    private static final ConcurrentMap<String, Logger> sCache = new ConcurrentHashMap<>();

    private Log() {
        // Private constructor for utility class.
    }

    private static void log(Level level, String tag, String message) {
        Logger logger = sCache.computeIfAbsent(tag, Logger::getLogger);
        if (logger.isLoggable(level)) {
            logger.log(level, message);
        }
    }

    private static void log(Level level, String tag, String message, Object... args) {
        Logger logger = sCache.computeIfAbsent(tag, Logger::getLogger);
        if (logger.isLoggable(level)) {
            logger.log(level, String.format(message, args));
        }
    }

    public static void d(String tag, String message) {
        log(Level.FINE, tag, message);
    }

    public static void d(String tag, String message, Object... args) {
        log(Level.FINE, tag, message, args);
    }

    public static void i(String tag, String message) {
        log(Level.INFO, tag, message);
    }

    public static void i(String tag, String message, Object... args) {
        log(Level.INFO, tag, message, args);
    }

    public static void w(String tag, String message) {
        log(Level.WARNING, tag, message);
    }

    public static void w(String tag, String message, Object... args) {
        log(Level.WARNING, tag, message, args);
    }

    public static void e(String tag, String message) {
        log(Level.SEVERE, tag, message);
    }

    public static void e(String tag, String message, Object... args) {
        log(Level.SEVERE, tag, message, args);
    }
}
