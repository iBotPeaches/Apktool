package smalinjector;

import brut.androlib.AndrolibException;
import brut.androlib.res.AndrolibResources;
import brut.androlib.res.util.ExtMXSerializer;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class Smalinjector {

    public static Map<Integer, Integer> prepare2Inject(String publicLibPath, String publicProjectPath, String outputMergedPublicFilePath) {
        Map<String, File> map = new HashMap<String, File>() {
            {
                put("LIB_RES", new File(publicLibPath));
                put("PRO_RES", new File(publicProjectPath));
            }
        };

        MergePublicValues smalinjector = new MergePublicValues();
        try {
            smalinjector.loadPublicTypes(map);
            smalinjector.mergePublicValues();
            AndrolibResources androlibResources = new AndrolibResources();
            ExtMXSerializer xmlSerializer = androlibResources.getResXmlSerializer();
            smalinjector.generatePublicXml(smalinjector.newProPublicValues, xmlSerializer, outputMergedPublicFilePath);
            smalinjector.generateReplaceIdMap(smalinjector.updateIdMap);
        } catch (AndrolibException | SAXException | ParserConfigurationException | IOException e) {
            e.printStackTrace();
        }

        return smalinjector.updateIdMap;
    }

    public static void main(String[] args) {
        Map<Integer, Integer> integerIntegerMap = prepare2Inject(
                "E:\\RESEARCH\\Apktool\\brut.apktool-smalinject\\assets\\public\\publicLib.xml",
                "E:\\RESEARCH\\Apktool\\brut.apktool-smalinject\\assets\\public\\publicProject.xml",
                "E:\\RESEARCH\\Apktool\\brut.apktool-smalinject\\assets\\public\\publicMerged.xml");
        System.out.println(integerIntegerMap.size());
    }
}
