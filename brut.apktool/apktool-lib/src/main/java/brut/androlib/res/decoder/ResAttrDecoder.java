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
package brut.androlib.res.decoder;

import brut.androlib.exceptions.AndrolibException;
import brut.androlib.exceptions.UndefinedResObjectException;
import brut.androlib.res.data.ResID;
import brut.androlib.res.data.ResPackage;
import brut.androlib.res.data.ResResSpec;
import brut.androlib.res.data.value.ResAttr;
import brut.androlib.res.data.value.ResScalarValue;

public class ResAttrDecoder {
    public String decode(int type, int value, String rawValue, int attrResId)
            throws AndrolibException {
        ResScalarValue resValue = mCurrentPackage.getValueFactory().factory(
                type, value, rawValue);

        String decoded = null;
        if (attrResId > 0) {
            try {
                ResAttr attr = (ResAttr) getCurrentPackage().getResTable()
                        .getResSpec(attrResId).getDefaultResource().getValue();

                decoded = attr.convertToResXmlFormat(resValue);
            } catch (UndefinedResObjectException | ClassCastException ex) {
                // ignored
            }
        }

        return decoded != null ? decoded : resValue.encodeAsResXmlAttr();
    }

    public String decodeManifestAttr(int attrResId)
        throws AndrolibException {

        if (attrResId != 0) {
            int attrId = attrResId;

            // See also: brut.androlib.res.data.ResTable.getResSpec
            if (attrId >> 24 == 0) {
                ResPackage pkg = getCurrentPackage();
                int packageId = pkg.getId();
                int pkgId = (packageId == 0 ? 2 : packageId);
                attrId = (0xFF000000 & (pkgId << 24)) | attrId;
            }

            // Retrieve the ResSpec in a package by id
            ResID resId = new ResID(attrId);
            ResPackage pkg = getCurrentPackage();
            if (pkg.hasResSpec(resId)) {
                ResResSpec resResSpec = pkg.getResSpec(resId);
                if (resResSpec != null) {
                    return resResSpec.getName();
                }
            }
        }

        return null;
    }

    public ResPackage getCurrentPackage() throws AndrolibException {
        if (mCurrentPackage == null) {
            throw new AndrolibException("Current package not set");
        }
        return mCurrentPackage;
    }

    public void setCurrentPackage(ResPackage currentPackage) {
        mCurrentPackage = currentPackage;
    }

    private ResPackage mCurrentPackage;
}
