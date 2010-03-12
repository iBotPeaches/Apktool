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

package brut.androlib.res.decoder;

import brut.androlib.*;
import brut.androlib.res.AndrolibResources;
import brut.androlib.res.data.ResPackage;
import brut.androlib.res.data.value.ResAttr;
import java.io.IOException;
import org.xmlpull.mxp1_serializer.MXSerializer;
import org.xmlpull.v1.XmlSerializer;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public class ResXmlSerializer extends MXSerializer {
    private final static String RES_NAMESPACE =
        "http://schemas.android.com/apk/res/android";

    private ResPackage mCurrentPackage;
    private boolean mDecodingEnabled = true;

    @Override
    public XmlSerializer attribute(String namespace, String name, String value)
            throws IOException, IllegalArgumentException, IllegalStateException
            {
        if (! mDecodingEnabled) {
            return super.attribute(namespace, name, value);
        }
        if (namespace == null || namespace.isEmpty()) {
            return super.attribute(namespace, name,
                AndrolibResources.escapeForResXml(value)
            );
        }
        String pkgName = RES_NAMESPACE.equals(namespace) ?
            "android" : mCurrentPackage.getName();

        try {
            ResAttr attr = (ResAttr) mCurrentPackage.getResTable()
                .getValue(pkgName, "attr", name);
            value = attr.convertToResXmlFormat(
                mCurrentPackage.getValueFactory().factory(value));
        } catch (AndrolibException ex) {
            throw new IllegalArgumentException(String.format(
                "could not decode attribute: ns=%s, name=%s, value=%s",
                getPrefix(namespace, false), name, value), ex);
        }

        if (value == null) {
            return this;
        }

//        if ("id".equals(name) && value.startsWith("@id")) {
        if (value.startsWith("@id")) {
            value = "@+id" + value.substring(3);
        }
        return super.attribute(namespace, name, value);
    }

    @Override
    public XmlSerializer text(String text) throws IOException {
        if (mDecodingEnabled) {
            text = AndrolibResources.escapeForResXml(text);
        }
        return super.text(text);
    }

    @Override
    public XmlSerializer text(char[] buf, int start, int len) throws IOException {
        if (mDecodingEnabled) {
            return this.text(new String(buf, start, len));
        }
        return super.text(buf, start, len);
    }

    @Override
    public void startDocument(String encoding, Boolean standalone) throws
            IOException, IllegalArgumentException, IllegalStateException {
        super.startDocument(encoding != null ? encoding : "UTF-8", standalone);
        super.out.write("\n");
        super.setPrefix("android", RES_NAMESPACE);
    }

    public void setCurrentPackage(ResPackage package_) {
        mCurrentPackage = package_;
    }

    public boolean setDecodingEnabled(boolean escapeRefs) {
        boolean oldVal = mDecodingEnabled;
        this.mDecodingEnabled = escapeRefs;
        return oldVal;
    }
}
