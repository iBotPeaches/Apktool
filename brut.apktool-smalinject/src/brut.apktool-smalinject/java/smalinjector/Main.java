package smalinjector;

import brut.androlib.AndrolibException;
import brut.androlib.res.AndrolibResources;
import brut.androlib.res.util.ExtMXSerializer;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class Main {

    public static void main(String[] args) {
        Map<String, File> map = new HashMap<String, File>() {
            {
                put("LIB_RES", new File("brut.apktool-smalinject/assets/public/publicLib.xml"));
                put("PRO_RES", new File("brut.apktool-smalinject/assets/public/publicProject.xml"));
            }
        };

        MergePublicValues smalinjector = new MergePublicValues();
        try {
            smalinjector.loadPublicTypes(map);
            smalinjector.mergePublicValues();
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
        try {
            smalinjector.generateReplaceIdMap(smalinjector.updateIdMap);
        } catch (IOException e) {
            e.printStackTrace();
        }
        AndrolibResources androlibResources = new AndrolibResources();
        ExtMXSerializer xmlSerializer = androlibResources.getResXmlSerializer();
        try {
            smalinjector.generatePublicXml(smalinjector.newProPublicValues, xmlSerializer);
        } catch (AndrolibException e) {
            e.printStackTrace();
        }
    }
}
