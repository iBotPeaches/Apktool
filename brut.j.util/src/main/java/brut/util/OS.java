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

import brut.common.BrutException;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class OS {

    private static final Logger LOGGER = Logger.getLogger("");

    public static void rmdir(File dir) throws BrutException {
        if (! dir.exists()) {
            return;
        }
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                rmdir(file);
            } else {
                //noinspection ResultOfMethodCallIgnored
                file.delete();
            }
        }
        //noinspection ResultOfMethodCallIgnored
        dir.delete();
    }

    public static void rmfile(String file) {
    	File del = new File(file);
        //noinspection ResultOfMethodCallIgnored
        del.delete();
    }

    public static void rmdir(String dir) throws BrutException {
        rmdir(new File(dir));
    }

    public static void cpdir(File src, File dest) throws BrutException {
        //noinspection ResultOfMethodCallIgnored
        dest.mkdirs();
        File[] files = src.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            File destFile = new File(dest.getPath() + File.separatorChar + file.getName());
            if (file.isDirectory()) {
                cpdir(file, destFile);
                continue;
            }
            try {
                try (InputStream in = Files.newInputStream(file.toPath())) {
                    try (OutputStream out = Files.newOutputStream(destFile.toPath())) {
                        IOUtils.copy(in, out);
                    }
                }
            } catch (IOException ex) {
                throw new BrutException("Could not copy file: " + file, ex);
            }
        }
    }

    public static void exec(String[] cmd) throws BrutException {
        Process ps;
        int exitValue;

        try {
            ProcessBuilder builder = new ProcessBuilder(cmd);
            ps = builder.start();

            new StreamForwarder(ps.getErrorStream(), "ERROR").start();
            new StreamForwarder(ps.getInputStream(), "OUTPUT").start();

            exitValue = ps.waitFor();
            if (exitValue != 0) {
                throw new BrutException("could not exec (exit code = " + exitValue + "): " + Arrays.toString(cmd));
            }
        } catch (IOException ex) {
            throw new BrutException("could not exec: " + Arrays.toString(cmd), ex);
        } catch (InterruptedException ex) {
            throw new BrutException("could not exec : " + Arrays.toString(cmd), ex);
        }
    }

    public static String execAndReturn(String[] cmd) {
        ExecutorService executor = Executors.newCachedThreadPool();
        try {
            ProcessBuilder builder = new ProcessBuilder(cmd);
            builder.redirectErrorStream(true);

            Process process = builder.start();
            StreamCollector collector = new StreamCollector(process.getInputStream());
            executor.execute(collector);

            process.waitFor(15, TimeUnit.SECONDS);
            executor.shutdownNow();
            if (! executor.awaitTermination(5, TimeUnit.SECONDS)) {
                System.err.println("Stream collector did not terminate.");
            }
            return collector.get();
        } catch (IOException | InterruptedException e) {
            return null;
        }
    }

    public static File createTempDirectory() throws BrutException {
        try {
            File tmp = File.createTempFile("BRUT", null);
            tmp.deleteOnExit();
            if (!tmp.delete()) {
                throw new BrutException("Could not delete tmp file: " + tmp.getAbsolutePath());
            }
            if (!tmp.mkdir()) {
                throw new BrutException("Could not create tmp dir: " + tmp.getAbsolutePath());
            }
            return tmp;
        } catch (IOException ex) {
            throw new BrutException("Could not create tmp dir", ex);
        }
    }

    static class StreamForwarder extends Thread {

        StreamForwarder(InputStream is, String type) {
            mIn = is;
            mType = type;
        }

        @Override
        public void run() {
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(mIn));
                String line;
                while ((line = br.readLine()) != null) {
                    if (mType.equals("OUTPUT")) {
                        LOGGER.info(line);
                    } else {
                        LOGGER.warning(line);
                    }
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        private final InputStream mIn;
        private final String mType;
    }

    static class StreamCollector implements Runnable {
        private final StringBuilder buffer = new StringBuilder();
        private final InputStream inputStream;

        public StreamCollector(InputStream inputStream) {
            super();
            this.inputStream = inputStream;
        }

        @Override
        public void run() {
            String line;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                while ((line = reader.readLine()) != null) {
                    buffer.append(line).append('\n');
                }
            } catch (IOException ignored) {}
        }

        public String get() {
            return buffer.toString();
        }
    }
}
