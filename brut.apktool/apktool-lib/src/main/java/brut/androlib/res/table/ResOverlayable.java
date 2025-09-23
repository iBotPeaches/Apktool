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
package brut.androlib.res.table;

import brut.androlib.exceptions.UndefinedResObjectException;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.List;
import java.util.logging.Logger;

public class ResOverlayable {
    private static final Logger LOGGER = Logger.getLogger(ResOverlayable.class.getName());

    private static final int FLAG_NONE = 0;
    private static final int FLAG_PUBLIC = 1 << 0; // 0x0001
    private static final int FLAG_SYSTEM_PARTITION = 1 << 1; // 0x0002
    private static final int FLAG_VENDOR_PARTITION = 1 << 2; // 0x0004
    private static final int FLAG_PRODUCT_PARTITION = 1 << 3; // 0x0008
    private static final int FLAG_SIGNATURE = 1 << 4; // 0x0010
    private static final int FLAG_ODM_PARTITION = 1 << 5; // 0x0020
    private static final int FLAG_OEM_PARTITION = 1 << 6; // 0x0040
    private static final int FLAG_ACTOR_SIGNATURE = 1 << 7; // 0x0080
    private static final int FLAG_CONFIG_SIGNATURE = 1 << 8; // 0x0100

    private static final int[] FLAG_MASKS = {
        FLAG_PUBLIC, FLAG_SYSTEM_PARTITION, FLAG_VENDOR_PARTITION, FLAG_PRODUCT_PARTITION,
        FLAG_SIGNATURE, FLAG_ODM_PARTITION, FLAG_OEM_PARTITION, FLAG_ACTOR_SIGNATURE,
        FLAG_CONFIG_SIGNATURE
    };
    private static final String[] FLAG_NAMES = {
        "public", "system", "vendor", "product", "signature", "odm", "oem", "actor",
        "config_signature"
    };

    private final ResPackage mPackage;
    private final String mName;
    private final String mActor;
    private final List<Policy> mPolicies;

    public ResOverlayable(ResPackage pkg, String name, String actor) {
        mPackage = pkg;
        mName = name;
        mActor = actor;
        mPolicies = new ArrayList<>();
    }

    public ResPackage getPackage() {
        return mPackage;
    }

    public String getName() {
        return mName;
    }

    public String getActor() {
        return mActor;
    }

    public void addPolicy(int flags, ResId[] entries) {
        mPolicies.add(new Policy(flags, entries));
    }

    public void serializeToXml(XmlSerializer serial) throws IOException {
        if (mPolicies.isEmpty()) {
            return;
        }

        serial.startTag(null, "overlayable");
        serial.attribute(null, "name", mName);
        if (mActor != null && !mActor.isEmpty()) {
            serial.attribute(null, "actor", mActor);
        }

        for (Policy policy : mPolicies) {
            String type = renderType(policy.getFlags());
            ResEntrySpec[] entrySpecs = resolveEntries(policy.getEntries());
            if (type == null || entrySpecs == null) {
                continue;
            }

            serial.startTag(null, "policy");
            serial.attribute(null, "type", type);

            for (ResEntrySpec entrySpec : entrySpecs) {
                serial.startTag(null, "item");
                serial.attribute(null, "type", entrySpec.getTypeName());
                serial.attribute(null, "name", entrySpec.getName());
                serial.endTag(null, "item");
            }

            serial.endTag(null, "policy");
        }

        serial.endTag(null, "overlayable");
    }

    private String renderType(int flags) {
        if (flags == FLAG_NONE) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < FLAG_MASKS.length; i++) {
            if ((flags & FLAG_MASKS[i]) != 0) {
                if (sb.length() > 0) {
                    sb.append('|');
                }
                sb.append(FLAG_NAMES[i]);
            }
        }
        if (sb.length() == 0) {
            return null;
        }
        return sb.toString();
    }

    private ResEntrySpec[] resolveEntries(ResId[] entries) {
        if (entries == null || entries.length == 0) {
            return null;
        }

        ResEntrySpec[] entrySpecs = new ResEntrySpec[entries.length];
        int entrySpecsCount = 0;

        for (int i = 0; i < entries.length; i++) {
            ResId id = entries[i];
            if (id == ResId.NULL) {
                continue;
            }

            ResEntrySpec entrySpec;
            try {
                entrySpec = mPackage.getEntrySpec(id);
            } catch (UndefinedResObjectException ignored) {
                LOGGER.warning("Unresolved overlayable entry ID: " + id);
                continue;
            }

            entrySpecs[entrySpecsCount++] = entrySpec;
        }

        if (entrySpecsCount < entrySpecs.length) {
            entrySpecs = Arrays.copyOf(entrySpecs, entrySpecsCount);
        }

        return entrySpecs;
    }

    @Override
    public String toString() {
        return String.format("ResOverlayable{pkg=%s, name=%s, actor=%s, policies=%s}",
            mPackage, mName, mActor, mPolicies);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof ResOverlayable) {
            ResOverlayable other = (ResOverlayable) obj;
            return Objects.equals(mPackage, other.mPackage)
                    && Objects.equals(mName, other.mName)
                    && Objects.equals(mActor, other.mActor);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mPackage, mName, mActor);
    }

    private static class Policy {
        private final int mFlags;
        private final ResId[] mEntries;

        public Policy(int flags, ResId[] entries) {
            mFlags = flags;
            mEntries = entries;
        }

        public int getFlags() {
            return mFlags;
        }

        public ResId[] getEntries() {
            return mEntries;
        }
    }
}
