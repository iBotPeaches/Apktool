/**
 *  Copyright 2010 Ryszard Wiśniewski <brut.alll@gmail.com>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package brut.util;

import brut.common.BrutException;
import java.io.*;
import java.util.Arrays;

import org.apache.commons.io.IOUtils;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public class OS {
    public static void rmdir(File dir) throws BrutException {
        if (! dir.exists()) {
            return;
        }
        File[] files = dir.listFiles();
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            if (file.isDirectory()) {
                rmdir(file);
            } else {
                file.delete();
            }
        }
        dir.delete();
    }
        
    public static void rmfile(String file) throws BrutException {
    	File del = new File(file);
    	del.delete();
    }

    public static void rmdir(String dir) throws BrutException {
        rmdir(new File(dir));
    }

    public static void cpdir(File src, File dest) throws BrutException {
        dest.mkdirs();
        File[] files = src.listFiles();
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            File destFile = new File(dest.getPath() + File.separatorChar
                + file.getName());
            if (file.isDirectory()) {
                cpdir(file, destFile);
                continue;
            }
            try {
                InputStream in = new FileInputStream(file);
                OutputStream out = new FileOutputStream(destFile);
                IOUtils.copy(in, out);
                in.close();
                out.close();
            } catch (IOException ex) {
                throw new BrutException("Could not copy file: " + file, ex);
            }
        }
    }

    public static void cpdir(String src, String dest) throws BrutException {
        cpdir(new File(src), new File(dest));
    }

    public static void exec(String[] cmd) throws BrutException {
        Process ps = null;
        try {
            ps = Runtime.getRuntime().exec(cmd);

            new StreamForwarder(ps.getInputStream(), System.err).start();
            new StreamForwarder(ps.getErrorStream(), System.err).start();
            if (ps.waitFor() != 0) {
                throw new BrutException(
                    "could not exec command: " + Arrays.toString(cmd));
            }
        } catch (IOException ex) {
            throw new BrutException(
                "could not exec command: " + Arrays.toString(cmd), ex);
        } catch (InterruptedException ex) {
            throw new BrutException(
                "could not exec command: " + Arrays.toString(cmd), ex);
        }
    }

    public static File createTempDirectory() throws BrutException {
        try {
            File tmp = File.createTempFile("BRUT", null);
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

        public StreamForwarder(InputStream in, OutputStream out) {
            mIn = in;
            mOut = out;
        }

        @Override
        public void run() {
            try {
                BufferedReader in = new BufferedReader(
                    new InputStreamReader(mIn));
                BufferedWriter out = new BufferedWriter(
                    new OutputStreamWriter(mOut));
                String line;
                while ((line = in.readLine()) != null) {
                    out.write(line);
                    out.newLine();
                }
                out.flush();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        private final InputStream mIn;
        private final OutputStream mOut;
    }
}
