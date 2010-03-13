/*
 *  Copyright 2010 Ryszard Wiśniewski <brut.alll@gmail.com>.
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
 *  under the License.
 */

package brut.androlib.res.data;

import brut.androlib.AndrolibException;
import brut.androlib.err.UndefinedResObject;
import java.util.*;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public final class ResType {
    private final String mName;
//    private final TypeConfig mConfig;
    private final Map<String, ResResSpec> mResSpecs =
        new LinkedHashMap<String, ResResSpec>();

    private final ResTable mResTable;
    private final ResPackage mPackage;

    public ResType(String name, ResTable resTable,
            ResPackage package_) {
        this.mName = name;
        this.mResTable = resTable;
        this.mPackage = package_;
    }

    public String getName() {
        return mName;
    }

    public Set<ResResSpec> listResSpecs() {
        return new LinkedHashSet<ResResSpec>(mResSpecs.values());
    }

    public ResResSpec getResSpec(String name) throws AndrolibException {
        ResResSpec spec = mResSpecs.get(name);
        if (spec == null) {
            throw new UndefinedResObject(String.format(
                "resource spec: %s/%s", getName(), name));
        }
        return spec;
    }
//
//    public void decode(AndrolibResources andRes, ResXmlSerializer serial,
//            Directory in, Directory out) throws AndrolibException {
//        decodeFiles(andRes, serial, in, out);
//        decodeValues(andRes, serial, out);
//    }
//
//    public void decodeFiles(AndrolibResources andRes, ResXmlSerializer serial,
//            Directory in, Directory out) throws AndrolibException {
//        if (! mConfig.isFile) {
//            return;
//        }
//
//        ResFileDecoder decoder = andRes.getResFileDecoder(serial);
//
//        for (ResResSpec res : listResources()) {
//            for (ResResource value : res.listResources()) {
//                if (! value.isStrVal()) {
//                    continue;
//                }
//                String fileName = value.getStrVal();
//                if (! fileName.startsWith("res/")) {
//                    throw new AndrolibException(
//                        "Invalid res file location: " + fileName);
//                }
//                decoder.decode(in, fileName.substring(4), out,
//                    value.getFilePath());
//            }
//        }
//    }
//
//    public void decodeValues(AndrolibResources andRes, ResXmlSerializer serial,
//            Directory out) throws AndrolibException {
//        if (! mConfig.isValues) {
//            return;
//        }
//
//        boolean oldEscapeRefs = serial.setEscapeRefs(false);
//
//        for (Entry<ResConfigFlags, Set<ResResource>> entry :
//                groupValuesByConfig().entrySet()) {
//            ResConfigFlags config = entry.getKey();
//            String filePath = "values" + config.getQualifiers() + "/"
//                + mConfig.valuesFileName + ".xml";
//
//            try {
//                serial.setOutput(out.getFileOutput(filePath), null);
//                serial.startDocument(null, null);
//                serial.startTag(null, "resources");
//
//                for (ResResource value : entry.getValue()) {
//                    if (mName.equals("array")) {
//                        decodeArrayValue(serial, value);
//                    } else {
//                        serial.startTag(null, mConfig.valuesTagName);
//                        serial.attribute(
//                            null, "name", value.getResSpec().getName());
//
//                        if (mName.equals("style")) {
//                            decodeStyleValue(serial, value);
//                        } else if (mName.equals("attr")) {
//                            decodeAttrValue(serial, value);
//                        } else {
//                            decodeSimpleValue(serial, value);
//                        }
//
//                        serial.endTag(null, mConfig.valuesTagName);
//                    }
//                }
//
//                serial.endTag(null, "resources");
//                serial.endDocument();
//            } catch (IOException ex) {
//                throw new AndrolibException(
//                    "Could not decode values for:" + filePath, ex);
//            } catch (DirectoryException ex) {
//                throw new AndrolibException(
//                    "Could not decode values for:" + filePath, ex);
//            }
//        }
//
//        serial.setEscapeRefs(oldEscapeRefs);
//    }
//
//    private void decodeSimpleValue(ResXmlSerializer serial, ResResource value)
//            throws IOException, AndrolibException {
//        serial.text(value.toResXMlText());
//    }
//
//    private void decodeArrayValue(ResXmlSerializer serial, ResResource value)
//            throws IOException, AndrolibException {
//        String arrType = value.getBag().getType();
//        String tagName = (arrType != null ? arrType + "-" : "") + "array";
//        serial.startTag(null, tagName);
//        serial.attribute(
//            null, "name", value.getResSpec().getName());
//        for (ResResource item : value.getBag().getItems().values()) {
//            serial.startTag(null, "item");
//            serial.text(item.getStrVal());
//            serial.endTag(null, "item");
//        }
//        serial.endTag(null, tagName);
//    }
//
//    private void decodeAttrValue(ResXmlSerializer serial, ResResource value)
//            throws IOException, AndrolibException {
//
//    }
//
//    private void decodeStyleValue(ResXmlSerializer serial, ResResource value)
//            throws IOException, AndrolibException {
//        ResBag bag = value.getBag();
//        if (bag.getParent().id != 0) {
//            serial.attribute(null, "parent", bag.getParentRes()
//                .getResReference(false, mPackage.getName()));
//
//            for (Entry<ResID, ResResource> entry : bag.getItems().entrySet()) {
//                serial.startTag(null, "item");
//
//                ResResSpec attr = mResTable.getResSpec(entry.getKey());
//                serial.attribute(null, "name", attr.getResReference(false, mPackage.getName(), true).substring(1));
//                serial.text(attr.getDefaultResource().decodeAttrValue(entry.getValue().getAsString(), false, mPackage.getName()));
//                serial.endTag(null, "item");
//            }
//        }
//    }

    public void addResSpec(ResResSpec spec)
            throws AndrolibException {
        if (mResSpecs.put(spec.getName(), spec) != null) {
            throw new AndrolibException(String.format(
                "Multiple res specs: %s/%s", getName(), spec.getName()));
        }
    }

    @Override
    public String toString() {
        return mName;
    }
//
//    private Map<ResConfigFlags, Set<ResResource>> groupValuesByConfig() {
//        Map<ResConfigFlags, Set<ResResource>> grouped =
//            new LinkedHashMap<ResConfigFlags, Set<ResResource>>();
//
//        for (ResResSpec res : listResSpecs()) {
//            for (ResResource value : res.listResources()) {
//                ResConfigFlags config = value.getConfig().getFlags();
//                Set<ResResource> values = grouped.get(config);
//                if (values == null) {
//                    values = new LinkedHashSet<ResResource>();
//                    grouped.put(config, values);
//                }
//                values.add(value);
//            }
//        }
//
//        return grouped;
//    }
//
//
//    public static ResType factory(String name, ResTable resTable,
//            ResPackage package_) throws AndrolibException {
//        if (typesConfig == null) {
//            loadTypesConfig();
//        }
//        TypeConfig config = typesConfig.get(name);
//        if (config == null) {
//            throw new AndrolibException("Invalid type name: " + name);
//        }
//        return new ResType(name, config, resTable, package_);
//    }
//
//    private static void loadTypesConfig() {
//        typesConfig = new HashMap<String, TypeConfig>();
//        typesConfig.put("anim", new FileTypeConfig());
//        typesConfig.put("drawable", new FileTypeConfig());
//        typesConfig.put("layout", new FileTypeConfig());
//        typesConfig.put("menu", new FileTypeConfig());
//        typesConfig.put("raw", new FileTypeConfig());
//        typesConfig.put("xml", new FileTypeConfig());
//
//        typesConfig.put("attr", new ValuesTypeConfig("attrs", "attr"));
//        typesConfig.put("dimen", new ValuesTypeConfig("dimens", "dimen"));
//        typesConfig.put("string", new ValuesTypeConfig("strings", "string"));
//        typesConfig.put("integer", new ValuesTypeConfig("integers", "integer"));
//        typesConfig.put("array", new ValuesTypeConfig("arrays", "array"));
//        typesConfig.put("style", new ValuesTypeConfig("styles", "style"));
//
//        typesConfig.put("color", new TypeConfig(true, true, "colors", "color"));
//
//        typesConfig.put("bool", new TypeConfig());
//        typesConfig.put("id", new TypeConfig());
////        typesConfig.put("integer", new TypeConfig());
//        typesConfig.put("plurals", new TypeConfig());
//    }
//
//    private static Map<String, TypeConfig> typesConfig;
//
//
//    public static class TypeConfig {
//        public final boolean isFile;
//        public final boolean isValues;
//        public final String valuesFileName;
//        public final String valuesTagName;
//
//        public TypeConfig() {
//            this(false, false, null, null);
//        }
//
//        public TypeConfig(boolean isFile, boolean isValues,
//                String valuesFileName, String valuesTagName) {
//            this.isFile = isFile;
//            this.isValues = isValues;
//            this.valuesFileName = valuesFileName;
//            this.valuesTagName = valuesTagName;
//        }
//    }
//
//    public static class FileTypeConfig extends TypeConfig {
//        public FileTypeConfig() {
//            super(true, false, null, null);
//        }
//    }
//
//    public static class ValuesTypeConfig extends TypeConfig {
//        public ValuesTypeConfig(String fileName, String tagName) {
//            super(false, true, fileName, tagName);
//        }
//    }
}
