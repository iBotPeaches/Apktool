/**
 *  Copyright 2014 Ryszard Wiśniewski <brut.alll@gmail.com>
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

import brut.common.BrutException;
import brut.directory.*;
import java.io.*;
import java.net.URL;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import org.custommonkey.xmlunit.ElementQualifier;
import org.w3c.dom.Element;
import org.xmlpull.v1.*;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public abstract class TestUtils {

    public static Map<String, String> parseStringsXml(File file)
            throws BrutException {
        try {
            XmlPullParser xpp = XmlPullParserFactory.newInstance()
                    .newPullParser();
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
        } catch (IOException ex) {
            throw new BrutException(ex);
        } catch (XmlPullParserException ex) {
            throw new BrutException(ex);
        }
    }

    /*
     * TODO: move to brut.util.Jar - it's not possible for now, because below
     * implementation uses brut.dir. I think I should merge all my projects to
     * single brut.common .
     */
    public static void copyResourceDir(Class class_, String dirPath, File out)
            throws BrutException {
        if (!out.exists()) {
            out.mkdirs();
        }
        copyResourceDir(class_, dirPath, new FileDirectory(out));
    }

    public static void copyResourceDir(Class class_, String dirPath,
                                       Directory out) throws BrutException {
        if (class_ == null) {
            class_ = Class.class;
        }

        URL dirURL = class_.getClassLoader().getResource(dirPath);
        if (dirURL != null && dirURL.getProtocol().equals("file")) {
            DirUtil.copyToDir(new FileDirectory(dirURL.getFile()), out);
            return;
        }

        if (dirURL == null) {
            String className = class_.getName().replace(".", "/") + ".class";
            dirURL = class_.getClassLoader().getResource(className);
        }

        if (dirURL.getProtocol().equals("jar")) {
            String jarPath;
            try {
                jarPath = URLDecoder.decode(
                        dirURL.getPath().substring(5,
                                dirURL.getPath().indexOf("!")), "UTF-8");
            } catch (UnsupportedEncodingException ex) {
                throw new BrutException(ex);
            }
            DirUtil.copyToDir(new FileDirectory(jarPath), out);
        }
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

            return controlType.equals(testType)
                    && control.getAttribute("name").equals(
                    test.getAttribute("name"));
        }
    }
}
