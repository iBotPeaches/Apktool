/*
 *  Copyright (C) 2010 Ryszard Wiśniewski <brut.alll@gmail.com>
 *  Copyright (C) 2010 Connor Tumbleson <connor.tumbleson@gmail.com>
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
package brut.androlib;

import brut.androlib.res.AndrolibResources;
import brut.common.BrutException;
import brut.directory.DirUtil;
import brut.directory.Directory;
import brut.directory.FileDirectory;
import brut.util.OS;
import org.custommonkey.xmlunit.ElementQualifier;
import org.w3c.dom.Element;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.*;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public abstract class TestUtils {

    public static Map<String, String> parseStringsXml(File file)
            throws BrutException {
        try {
            XmlPullParser xpp = XmlPullParserFactory.newInstance().newPullParser();
            xpp.setInput(new FileReader(file));

            int eventType;
            String key = null;
            Map<String, String> map = new HashMap<String, String>();
            while ((eventType = xpp.next()) != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        if ("string".equals(xpp.getName())) {
                            int attrCount = xpp.getAttributeCount();
                            for (int i = 0; i < attrCount; i++) {
                                if ("name".equals(xpp.getAttributeName(i))) {
                                    key = xpp.getAttributeValue(i);
                                    break;
                                }
                            }
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        if ("string".equals(xpp.getName())) {
                            key = null;
                        }
                        break;
                    case XmlPullParser.TEXT:
                        if (key != null) {
                            map.put(key, xpp.getText());
                        }
                        break;
                }
            }

            return map;
        } catch (IOException | XmlPullParserException ex) {
            throw new BrutException(ex);
        }
    }

    public static void copyResourceDir(Class class_, String dirPath, File out) throws BrutException {
        if (!out.exists()) {
            out.mkdirs();
        }
        copyResourceDir(class_, dirPath, new FileDirectory(out));
    }

    public static void copyResourceDir(Class class_, String dirPath, Directory out) throws BrutException {
        if (class_ == null) {
            class_ = Class.class;
        }

        URL dirURL = class_.getClassLoader().getResource(dirPath);
        if (dirURL != null && dirURL.getProtocol().equals("file")) {
            try {
                DirUtil.copyToDir(new FileDirectory(dirURL.getFile()), out);
            } catch (UnsupportedEncodingException ex) {
                throw new BrutException(ex);
            }
            return;
        }

        if (dirURL == null) {
            String className = class_.getName().replace(".", "/") + ".class";
            dirURL = class_.getClassLoader().getResource(className);
        }

        if (dirURL.getProtocol().equals("jar")) {
            String jarPath;
            try {
                jarPath = URLDecoder.decode(dirURL.getPath().substring(5, dirURL.getPath().indexOf("!")), "UTF-8");
                DirUtil.copyToDir(new FileDirectory(jarPath), out);
            } catch (UnsupportedEncodingException ex) {
                throw new BrutException(ex);
            }
        }
    }

    public static void cleanFrameworkFile() throws BrutException {
        File framework = new File(getFrameworkDir(), "1.apk");

        if (Files.exists(framework.toPath())) {
            OS.rmfile(framework.getAbsolutePath());
        }
    }

    public static byte[] readHeaderOfFile(File file, int size) throws IOException {
        byte[] buffer = new byte[size];
        InputStream inputStream = new FileInputStream(file);
        if (inputStream.read(buffer) != buffer.length) {
            throw new IOException("File size too small for buffer length: " + size);
        }
        inputStream.close();

        return buffer;
    }

    static File getFrameworkDir() throws AndrolibException {
        AndrolibResources androlibResources = new AndrolibResources();
        androlibResources.apkOptions = new ApkOptions();
        return androlibResources.getFrameworkDir();
    }

    public static class ResValueElementQualifier implements ElementQualifier {

        @Override
        public boolean qualifyForComparison(Element control, Element test) {
            String controlType = control.getTagName();
            if ("item".equals(controlType)) {
                controlType = control.getAttribute("type");
            }

            String testType = test.getTagName();
            if ("item".equals(testType)) {
                testType = test.getAttribute("type");
            }

            return controlType.equals(testType) && control.getAttribute("name").equals(test.getAttribute("name"));
        }
    }

    public static String replaceNewlines(String value) {
        return value.replace("\n", "").replace("\r", "");
    }
}
