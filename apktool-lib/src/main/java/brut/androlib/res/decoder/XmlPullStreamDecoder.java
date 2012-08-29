/**
 *  Copyright 2011 Ryszard Wiśniewski <brut.alll@gmail.com>
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

package brut.androlib.res.decoder;

import brut.androlib.AndrolibException;
import brut.androlib.res.data.ResTable;
import brut.androlib.res.util.ExtXmlSerializer;
import java.io.*;
import java.util.logging.Logger;
import org.xmlpull.v1.*;
import org.xmlpull.v1.wrapper.*;
import org.xmlpull.v1.wrapper.classic.StaticXmlSerializerWrapper;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public class XmlPullStreamDecoder implements ResStreamDecoder {
    public XmlPullStreamDecoder(XmlPullParser parser,
            ExtXmlSerializer serializer) {
        this.mParser = parser;
        this.mSerial = serializer;
    }

    public void decode(InputStream in, OutputStream out)
            throws AndrolibException {
        try {
            XmlPullWrapperFactory factory = XmlPullWrapperFactory.newInstance();
            XmlPullParserWrapper par = factory.newPullParserWrapper(mParser);
            final ResTable resTable = ((AXmlResourceParser)mParser).getAttrDecoder().getCurrentPackage().getResTable();
            
            XmlSerializerWrapper ser = new StaticXmlSerializerWrapper(mSerial, factory){
                boolean hideSdkInfo = false;
                @Override
                public void event(XmlPullParser pp) throws XmlPullParserException, IOException {
                    int type = pp.getEventType();
                    
                        if (type == XmlPullParser.START_TAG) {
                            if ("uses-sdk".equalsIgnoreCase(pp.getName())) {
                                try {
                                    hideSdkInfo = parseAttr(pp);
                                    if(hideSdkInfo) {
                                        return;
                                    }
                                } catch (AndrolibException e) {}
                            }
                        } else if (hideSdkInfo && type == XmlPullParser.END_TAG && 
                                "uses-sdk".equalsIgnoreCase(pp.getName())) {
                            return;
                        }
                        super.event(pp);
                    }
                
                private boolean parseAttr(XmlPullParser pp) throws AndrolibException {
                    ResTable restable = resTable;
                    for (int i = 0; i < pp.getAttributeCount(); i++) {
                        final String a_ns = "http://schemas.android.com/apk/res/android";
                        String ns = pp.getAttributeNamespace (i);
                        if (a_ns.equalsIgnoreCase(ns)) {
                            String name = pp.getAttributeName (i);
                            String value = pp.getAttributeValue (i);
                            if (name != null && value != null) {
                                if (name.equalsIgnoreCase("minSdkVersion") || 
                                        name.equalsIgnoreCase("targetSdkVersion") || 
                                        name.equalsIgnoreCase("maxSdkVersion")) {
                                    restable.addSdkInfo(name, value);
                                } else {
                                    restable.clearSdkInfo();
                                    return false;//Found unknown flags
                                }
                            }
                        } else {
                            resTable.clearSdkInfo();
                            return false;//Found unknown flags
                        }
                    }
                    return true;
                }
            };

            par.setInput(in, null);
            ser.setOutput(out, null);

            while (par.nextToken() != XmlPullParser.END_DOCUMENT) {
                ser.event(par);
            }
            ser.flush();
        } catch (XmlPullParserException ex) {
            throw new AndrolibException("Could not decode XML", ex);
        } catch (IOException ex) {
            throw new AndrolibException("Could not decode XML", ex);
        }
    }

    public void decodeManifest(InputStream in, OutputStream out)
            throws AndrolibException {
        mOptimizeForManifest = true;
        try {
            decode(in, out);
        } finally {
            mOptimizeForManifest = false;
        }
    }

    private final XmlPullParser mParser;
    private final ExtXmlSerializer mSerial;

    private boolean mOptimizeForManifest = false;

    private final static Logger LOGGER =
        Logger.getLogger(XmlPullStreamDecoder.class.getName());
}
