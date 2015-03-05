/*
 * [The "BSD licence"]
 * Copyright (c) 2010 Ben Gruver
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.jf.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConsoleUtil {
    /**
     * Attempt to find the width of the console. If it can't get the width, return a default of 80
     * @return The current console width
     */
    public static int getConsoleWidth() {
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            try {
                return attemptMode();
            } catch (Exception ex) {
            }
        } else {
            try {
                return attemptStty();
            } catch (Exception ex) {
            }
        }

        return 80;
    }

    private static int attemptStty() {
        String output = attemptCommand(new String[]{"sh", "-c", "stty size < /dev/tty"});
        if (output == null) {
            return 80;
        }

        String[] vals = output.split(" ");
        if (vals.length < 2) {
            return 80;
        }
        return Integer.parseInt(vals[1]);
    }

    private static int attemptMode() {
        String output = attemptCommand(new String[]{"mode", "con"});
        if (output == null) {
            return 80;
        }

        Pattern pattern = Pattern.compile("Columns:[ \t]*(\\d+)");
        Matcher m = pattern.matcher(output);
        if (!m.find()) {
            return 80;
        }

        return Integer.parseInt(m.group(1));
    }

    private static String attemptCommand(String[] command) {
        StringBuffer buffer = null;

        try {

            Process p = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line;

            while ((line = reader.readLine()) != null) {
                if (buffer == null) {
                    buffer = new StringBuffer();
                }

                buffer.append(line);
            }

            if (buffer != null) {
                return buffer.toString();
            }
            return null;
        } catch (Exception ex) {
            return null;
        }
    }
}
