package brut.androlib;

import com.android.tools.smali.baksmali.BaksmaliOptions;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import brut.androlib.exceptions.AndrolibException;
import brut.androlib.res.data.ResResSpec;
import brut.androlib.res.data.ResTable;
import brut.androlib.res.data.ResPackage;
import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.Map;

class BaksmaliConfig extends BaksmaliOptions {
    private final static Logger LOGGER = Logger.getLogger(BaksmaliConfig.class.getName());

    // Extending BaksmaliOptions to implement the custom logic used to sort/group the different
    // resourceIds found in the apk. This change allows for referencing/annotating the smali files
    // anytime a system resource is used as well, instead of just the resources provided provided
    // by the apk itself.
    @Override
    public void loadResourceIds(Map<String, File> resourceFiles) throws SAXException, IOException {
        for (Map.Entry<String, File> entry: resourceFiles.entrySet()) {
            try {
                SAXParserFactory parserFactory = SAXParserFactory.newInstance();
                parserFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
                SAXParser parser = parserFactory.newSAXParser();

                final String prefix = entry.getKey();
                parser.parse(entry.getValue(), new DefaultHandler() {
                    @Override
                    public void startElement(String uri, String localName, String qName, Attributes attr) throws SAXException {
                        if (qName.equals("public")) {
                            String resourceType = attr.getValue("type");
                            String resourceName = attr.getValue("name").replace('.', '_');
                            Integer resourceId = Integer.decode(attr.getValue("id"));
                            String qualifiedResourceName = String.format("%s.%s.%s", prefix, resourceType, resourceName);
                            resourceIds.put(resourceId, qualifiedResourceName);
                        }
                    }
                });
            } catch (ParserConfigurationException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    // This method processes resource references in two stages:
    // 1. It parses the 'framework-res' package to load all public system resource references.
    // 2. It associates the APK's resources with their respective identifiers by mapping the
    //    APK's base file name (outDir) to its 'public.xml' file. This mapping stores a reference
    //    to each resource in the form of:
    //
    //        ApkName.res_type.res_name
    //
    //    Theyre then added to the list of system resources, and are subsequently used
    //    to annotate the Smali output everytime a resource is referenced
    //    during the disassembly of the dex files, and will look something like:
    //
    //        # base.string.app_name
    //        # base.layout.activity_main
    public void loadResourceIds(ResTable resTable, File outDir) throws SAXException, IOException, AndrolibException {
        this.loadFrameworkResources(resTable);
        Map<String, File> resourceFiles = new HashMap<>();
        resourceFiles.put(outDir.getName(), new File(outDir, "res/values/public.xml"));
        loadResourceIds(resourceFiles);
    }

    private void loadFrameworkResources(ResTable resTable) throws AndrolibException {
        try {
            LOGGER.info("Parsing framework resource ids...");
            ResPackage pkg = resTable.getPackage(1);

            for (ResResSpec spec : pkg.listResSpecs()) {
                String resourceId = String.format("0x%08x", spec.getId().id);
                String qualifiedResourceName = String.format("Android.%s.%s", spec.getType().getName(), spec.getName());
                resourceIds.put(Integer.decode(resourceId), qualifiedResourceName);
            }
        } catch (AndrolibException ex) {
            throw new AndrolibException("Could not parse framework resources", ex);
        }
    }

}
