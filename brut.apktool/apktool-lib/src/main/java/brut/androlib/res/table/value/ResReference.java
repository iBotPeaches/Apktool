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
package brut.androlib.res.table.value;

import brut.androlib.exceptions.AndrolibException;
import brut.androlib.exceptions.UndefinedResObjectException;
import brut.androlib.res.table.ResEntry;
import brut.androlib.res.table.ResEntrySpec;
import brut.androlib.res.table.ResId;
import brut.androlib.res.table.ResPackage;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.util.Objects;

public class ResReference extends ResItem {
    private final ResPackage mPackage;
    private final ResId mResId;
    private final boolean mAsAttr;

    public ResReference(ResPackage pkg, ResId resId) {
        this(pkg, resId, false);
    }

    public ResReference(ResPackage pkg, ResId resId, boolean asAttr) {
        super(asAttr ? TYPE_ATTRIBUTE : TYPE_REFERENCE);
        assert pkg != null && resId != null;
        mPackage = pkg;
        mResId = resId;
        mAsAttr = asAttr;
    }

    public ResPackage getPackage() {
        return mPackage;
    }

    public ResId getResId() {
        return mResId;
    }

    public ResEntrySpec resolve() throws AndrolibException {
        if (mPackage != null && mResId != ResId.NULL) {
            try {
                return mPackage.getTable().resolve(mResId);
            } catch (UndefinedResObjectException ignored) {
            }
        }

        return null;
    }

    public ResEntry resolveEntry() throws AndrolibException {
        if (mPackage != null && mResId != ResId.NULL) {
            try {
                return mPackage.getTable().resolveEntry(mResId);
            } catch (UndefinedResObjectException ignored) {
            }
        }

        return null;
    }

    @Override
    public String toXmlTextValue() throws AndrolibException {
        ResEntrySpec spec = resolve();
        if (spec == null) {
            // @null is a special primitive, not a true reference, but we have
            // to fall back to it if we can't resolve the reference.
            return "@null";
        }

        boolean includePackage = mPackage.getGroup() != spec.getPackage().getGroup();
        boolean includeType = !mAsAttr || !spec.getTypeSpec().getName().equals("attr");
        return (mAsAttr ? "?" : "@")
             + (includePackage ? spec.getPackage().getName() + ":" : "")
             + (includeType ? spec.getTypeSpec().getName() + "/" : "")
             + spec.getName();
    }

    @Override
    public void serializeToValuesXml(XmlSerializer serial, ResEntry entry) throws AndrolibException, IOException {
        String typeName = entry.getType().getName();

        // A bag type with a reference value must be an <item> tag.
        // Otherwise, when the decoded app is rebuilt, the reference will be lost.
        boolean asItem = entry.getType().isBagType();

        // Only set body if not @null or the entry is a <string> tag.
        // @null is the default value for all item types except string.
        // Note: We never set @null to <id> tags.
        boolean needsBody = resolve() != null || typeName.equals("string");

        String tagName = asItem ? "item" : typeName;
        serial.startTag(null, tagName);
        if (asItem) {
            serial.attribute(null, "type", typeName);
        }
        serial.attribute(null, "name", entry.getName());
        if (needsBody) {
            serial.text(toXmlTextValue());
        }
        serial.endTag(null, tagName);
    }

    @Override
    public String toString() {
        return String.format("ResReference{pkg=%s, id=%s, type=%s}", mPackage, mResId, mAsAttr ? "attr" : "ref");
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof ResReference) {
            ResReference other = (ResReference) obj;
            return mPackage.equals(other.mPackage)
                && mResId == other.mResId
                && mAsAttr == other.mAsAttr;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mPackage, mResId, mAsAttr);
    }
}
