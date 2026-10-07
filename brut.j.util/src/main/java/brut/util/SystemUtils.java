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
package brut.util;

import brut.common.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import com.google.common.io.ByteStreams;

public final class SystemUtils {
    private static final String OS_NAME = System.getProperty("os.name").toLowerCase(Locale.ROOT);
    private static final boolean IS_64BIT;
    static {
        if (isWindows()) {
            String arch = System.getenv("PROCESSOR_ARCHITEW6432");
            if (arch == null || arch.isEmpty()) {
                arch = System.getenv("PROCESSOR_ARCHITECTURE");
            }
            IS_64BIT = arch != null && arch.endsWith("64");
        } else {
            String bitness = System.getProperty("sun.arch.data.model");
            IS_64BIT = bitness != null && bitness.equals("64");
        }
    }

    private SystemUtils() {}

    public static String getOSName() {
        return OS_NAME;
    }

    public static boolean isWindows() {
        return OS_NAME.contains("win");
    }

    public static boolean isMac() {
        return OS_NAME.contains("mac");
    }

    public static boolean isUnix() {
        return OS_NAME.contains("nix") || OS_NAME.contains("nux")
            || OS_NAME.contains("aix") || OS_NAME.contains("sunos");
    }

    public static boolean is64Bit() {
        return IS_64BIT;
    }

    public static void execute(String[] cmd) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(cmd);
        Process process = builder.start();
        process.getOutputStream().close(); // nothing to send, avoid hanging on stdin reads
        StreamForwarder out = new StreamForwarder(process.getInputStream(), line -> Log.i(Log.ROOT, line));
        StreamForwarder err = new StreamForwarder(process.getErrorStream(), line -> Log.w(Log.ROOT, line));
        out.start();
        err.start();
        try {
            int exitValue = process.waitFor();
            // Let the forwarders flush remaining output before returning or throwing.
            out.join(5000);
            err.join(5000);
            if (exitValue != 0) {
                throw new IOException("Command failed with exit code " + exitValue + ": " + Arrays.toString(cmd));
            }
        } finally {
            process.destroyForcibly();
        }
    }

    private static class StreamForwarder extends Thread {
        private final InputStream mIn;
        private final Consumer<String> mConsumer;

        public StreamForwarder(InputStream in, Consumer<String> consumer) {
            setDaemon(true);
            mIn = in;
            mConsumer = consumer;
        }

        @Override
        public void run() {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(mIn, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    mConsumer.accept(line);
                }
            } catch (IOException ex) {
                Log.w(Log.ROOT, "Stream forwarding failed: " + ex);
            }
        }
    }

    public static String executeAndReturn(String[] cmd) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(cmd);
        builder.redirectErrorStream(true);
        Process process = builder.start();
        FutureTask<String> output = new FutureTask<>(() -> {
            try (InputStream in = process.getInputStream()) {
                return new String(ByteStreams.toByteArray(in), StandardCharsets.UTF_8);
            }
        });
        Thread thread = new Thread(output);
        thread.setDaemon(true);
        thread.start();
        try {
            boolean finished = process.waitFor(15, TimeUnit.SECONDS);
            if (!finished) {
                throw new IOException("Command timed out after 15s: " + Arrays.toString(cmd));
            }
            return output.get(5, TimeUnit.SECONDS);
        } catch (ExecutionException ex) {
            throw new IOException("Failed reading output of: " + Arrays.toString(cmd), ex.getCause());
        } catch (TimeoutException ex) {
            throw new IOException("Output reader did not terminate for: " + Arrays.toString(cmd), ex);
        } finally {
            process.destroyForcibly();
        }
    }
}
