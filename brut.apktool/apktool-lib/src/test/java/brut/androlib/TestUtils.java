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
package brut.androlib;

import brut.androlib.exceptions.AndrolibException;
import brut.androlib.res.Framework;
import brut.androlib.res.xml.ResXmlPatcher;
import brut.common.BrutException;
import brut.directory.DirUtil;
import brut.directory.Directory;
import brut.directory.FileDirectory;
import brut.util.OS;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public abstract class TestUtils {

    public static Map<String, String> parseStringsXml(File file) throws BrutException {
        try {
            Document doc = getDocumentFromFile(file);
            XPath xPath = XPathFactory.newInstance().newXPath();
            String expression = "/resources/string[@name]";
            NodeList nodes = (NodeList) xPath.evaluate(expression, doc, XPathConstants.NODESET);

            Map<String, String> map = new HashMap<>();

            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                NamedNodeMap attrs = node.getAttributes();
                Node nameAttr = attrs.getNamedItem("name");
                map.put(nameAttr.getNodeValue(), node.getTextContent());
            }

            return map;
        } catch (XPathExpressionException ex) {
            throw new BrutException(ex);
        }
    }

    public static Document getDocumentFromFile(File file) throws BrutException {
        try {
            return ResXmlPatcher.loadDocument(file);
        } catch (IOException | SAXException | ParserConfigurationException ex) {
            throw new BrutException(ex);
        }
    }

    public static void copyResourceDir(Class<?> class_, String dirPath, File out) throws BrutException {
        if (!out.exists()) {
            out.mkdirs();
        }
        copyResourceDir(class_, dirPath, new FileDirectory(out));
    }

    public static void copyResourceDir(Class<?> class_, String dirPath, Directory out) throws BrutException {
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
        File framework = new File(getFrameworkDirectory(), "1.apk");

        if (Files.exists(framework.toPath())) {
            OS.rmfile(framework.getAbsolutePath());
        }
    }

    public static byte[] readHeaderOfFile(File file, int size) throws IOException {
        byte[] buffer = new byte[size];
        InputStream inputStream = Files.newInputStream(file.toPath());
        if (inputStream.read(buffer) != buffer.length) {
            throw new IOException("File size too small for buffer length: " + size);
        }
        inputStream.close();

        return buffer;
    }

    static File getFrameworkDirectory() throws AndrolibException {
        Config config = Config.getDefaultConfig();
        Framework framework = new Framework(config);
        return framework.getFrameworkDirectory();
    }

    public static String replaceNewlines(String value) {
        return value.replace("\n", "").replace("\r", "");
    }
}
