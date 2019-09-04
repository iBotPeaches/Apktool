package smalinjector;

import brut.androlib.AndrolibException;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import org.xmlpull.v1.XmlSerializer;
import smalinjector.model.PublicValue;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

public class MergePublicValues {

    public Map<String, List<PublicValue>> originLibPublicMap = new HashMap<>();
    public Map<String, List<PublicValue>> originProPublicMap = new HashMap<>();
    public Map<Integer, Integer> updateIdMap = new HashMap<>();
    public List<PublicValue> newProPublicValues = new ArrayList<>();

    private List<PublicValue> originLibPublicValues = new ArrayList<>();
    private List<PublicValue> originProPublicValues = new ArrayList<>();

    private SortedSet<String> publicTypes = new TreeSet<>();

    private void loadResourceIds(Map<String, File> resourceFiles) throws ParserConfigurationException, SAXException, IOException {
        for (Map.Entry<String, File> entry : resourceFiles.entrySet()) {
            SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
            saxParser.parse(entry.getValue(), new DefaultHandler() {
                @Override
                public void startElement(String uri, String localName, String qName, Attributes attributes) {
                    if (qName.equals("public")) {
                        String resourceType = attributes.getValue("type");
                        String resourceName = attributes.getValue("name");
                        Integer resourceId = Integer.decode(attributes.getValue("id"));

                        publicTypes.add(resourceType);

                        PublicValue publicValue = new PublicValue(resourceId, resourceName, resourceType);
                        if (entry.getKey().equals("LIB_RES")) {
                            if (!originLibPublicValues.contains(publicValue)) {
                                originLibPublicValues.add(new PublicValue(resourceId, resourceName, resourceType));
                            }
                        } else {
                            if (!originProPublicValues.contains(publicValue)) {
                                originProPublicValues.add(new PublicValue(resourceId, resourceName, resourceType));
                            }
                        }
                    }
                }
            });
        }
        originLibPublicValues.sort(Comparator.comparingInt(o -> o.id));
        originProPublicValues.sort(Comparator.comparingInt(o -> o.id));
    }

    private Map<String, List<PublicValue>> loadPublicTypes(List<PublicValue> publicValues) {
        List<PublicValue> tempPublicValues = new ArrayList<>();
        Map<String, List<PublicValue>> originResourcesType = new HashMap<>();
        for (int i = 0; i < publicValues.size(); i++) {
            String currType = publicValues.get(i).type;

            if (i == publicValues.size() - 1) {
                tempPublicValues.add(publicValues.get(i));
                originResourcesType.put(currType, tempPublicValues);
                break;
            }

            String nextType = publicValues.get(i + 1).type;
            tempPublicValues.add(publicValues.get(i));
            if (!currType.equals(nextType)) {
                originResourcesType.put(currType, tempPublicValues);
                tempPublicValues = new ArrayList<>();
            }
        }
        return originResourcesType;
    }

    public void loadPublicTypes(Map<String, File> publicFiles) throws IOException, SAXException, ParserConfigurationException {
        loadResourceIds(publicFiles);
        originLibPublicMap = loadPublicTypes(originLibPublicValues);
        originProPublicMap = loadPublicTypes(originProPublicValues);
    }

    public void mergePublicValues() {
        for (String publicType : publicTypes) {

            boolean proHasResType = originProPublicMap.containsKey(publicType);
            boolean libHasResType = originLibPublicMap.containsKey(publicType);

            // Kiểm tra 2 file lib và project có cùng type
            if (proHasResType && libHasResType) {
                List<PublicValue> tempOriginProPublicValues = originProPublicMap.get(publicType);
                List<PublicValue> tempOriginLibPublicValues = originLibPublicMap.get(publicType);

                // Thêm tất cả các phần tử type hiện tại của project public
                newProPublicValues.addAll(tempOriginProPublicValues);
                // Lấy giá trị id cuối cùng type hiện tại của project public
                Integer newResId = tempOriginProPublicValues.get(tempOriginProPublicValues.size() - 1).id;
                // Tăng lên 1 nếu có phần từ mới thì update id này cho nó
                newResId++;
                // Kiểm tra lib public
                for (PublicValue libPublicValue : tempOriginLibPublicValues) {
                    boolean breakNestedLoop = false;
                    for (PublicValue proPublicValue : tempOriginProPublicValues) {
                        // Kiểm 2 phần tủ trùng tên
                        if (proPublicValue.equals(libPublicValue)) {
                            // Tiếp tục kiểm tra phẩn tử mới
                            breakNestedLoop = true;
                            // Nếu id của lib khác id của project, cập nhật id mới cho lib
                            if (!libPublicValue.id.equals(proPublicValue.id)) {
                                updateIdMap.put(libPublicValue.id, proPublicValue.id);
                            }
                            break;
                        }

                    }
                    if (breakNestedLoop) {
                        continue;
                    }
                    // Kiểm tra và update map id phục vụ cập nhật smali
                    if (!libPublicValue.id.equals(newResId)) {
                        updateIdMap.put(libPublicValue.id, newResId);
                    }
                    // In danh sách các item được thêm mới
                    System.out.println(convertEntityToXML(libPublicValue));
                    // Thêm vào trong danh sách project public mới, dùng file này thay thế file public hiện tại trong project
                    newProPublicValues.add(new PublicValue(newResId, libPublicValue.name, libPublicValue.type));
                    newResId++;
                }
                continue;
            }

            // Nếu chỉ 1 trong 2 file có type đó thì tự động thêm vào danh sách project public mới mà ko cần kiểm tra
            if (proHasResType) {
                newProPublicValues.addAll(originProPublicMap.get(publicType));
                continue;
            }

            if (libHasResType) {
                newProPublicValues.addAll(originLibPublicMap.get(publicType));
            }
        }
    }

    void generatePublicXml(List<PublicValue> newProResIds,
                                  XmlSerializer serial) throws AndrolibException {
        try {
            OutputStream outStream = new FileOutputStream("brut.apktool-smalinject/assets/public/publicMerged.xml");
            serial.setOutput(outStream, null);
            serial.startDocument(null, null);
            serial.startTag(null, "resources");

            for (PublicValue spec : newProResIds) {
                serial.startTag(null, "public");
                serial.attribute(null, "type", spec.type);
                serial.attribute(null, "name", spec.name);
                serial.attribute(null, "id", String.format("0x%08x", spec.id));
                serial.endTag(null, "public");
            }

            serial.endTag(null, "resources");
            serial.endDocument();
            serial.flush();
            outStream.close();
        } catch (IOException ex) {
            throw new AndrolibException("Could not generate public.xml file", ex);
        }
    }

    void generateReplaceIdMap(Map<Integer, Integer> updateResMap) throws IOException {
        FileOutputStream outStream = new FileOutputStream("brut.apktool-smalinject/assets/public/mapId.txt");
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Integer, Integer> entry : updateResMap.entrySet()) {
            sb.append(entry.getKey()).append(" ").append(entry.getValue()).append("\n");
        }
        outStream.write(sb.toString().getBytes());
        outStream.close();
    }

    private String convertEntityToXML(PublicValue entity) {
        return String.format("    <public type=\"%s\" name=\"%s\" id=\"%s\" />", entity.type, entity.name, "0x" + Integer.toHexString(entity.id));
    }
}
