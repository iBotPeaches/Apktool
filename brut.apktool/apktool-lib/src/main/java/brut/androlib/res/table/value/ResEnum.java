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

import brut.androlib.Config;
import brut.androlib.exceptions.AndrolibException;
import brut.androlib.res.table.ResEntry;
import brut.androlib.res.table.ResEntrySpec;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

public class ResEnum extends ResAttribute {
    private static final Logger LOGGER = Logger.getLogger(ResEnum.class.getName());

    private final Symbol[] mSymbols;
    private final Map<Integer, String> mFormatCache;

    public ResEnum(ResReference parent, int type, int min, int max, int l10n, Symbol[] symbols) {
        super(parent, type, min, max, l10n);
        mSymbols = symbols;
        mFormatCache = new HashMap<>();
    }

    @Override
    protected String formatValueToSymbols(ResItem value) throws AndrolibException {
        if (!(value instanceof ResPrimitive)) {
            return null;
        }

        int data = ((ResPrimitive) value).getData();
        String formatted = mFormatCache.get(data);
        if (formatted != null) {
            return formatted;
        }

        for (Symbol symbol : mSymbols) {
            if (symbol.getValue().getData() != data) {
                continue;
            }

            ResReference key = symbol.getKey();
            ResEntrySpec keySpec = key.resolve();
            if (keySpec != null) {
                formatted = keySpec.getName();
                mFormatCache.put(data, formatted);

                // fill_parent is deprecated since API 8 but appears first.
                // Keep looking for match_parent and use it instead if found.
                if (data == -1 && formatted.equals("fill_parent")) {
                    continue;
                }
            }
            break;
        }

        return formatted;
    }

    @Override
    protected void serializeSymbolsToValuesXml(XmlSerializer serial, ResEntry entry)
            throws AndrolibException, IOException {
        Config config = mParent.getPackage().getTable().getConfig();
        boolean removeUnresolved = config.getDecodeResolve() == Config.DecodeResolve.REMOVE;

        for (Symbol symbol : mSymbols) {
            ResReference key = symbol.getKey();
            ResEntrySpec keySpec = key.resolve();
            ResPrimitive value = symbol.getValue();

            String name;
            if (keySpec != null) {
                name = keySpec.getName();
            } else {
                // #2836 - Support skipping items if the resource cannot be identified.
                if (removeUnresolved) {
                    LOGGER.warning(String.format(
                        "null enum reference: key=%s, value=%s", key, value));
                    continue;
                }

                name = ResEntrySpec.MISSING_PREFIX + key.getId();
            }

            serial.startTag(null, "enum");
            serial.attribute(null, "name", name);
            serial.attribute(null, "value", value.encodeAsResXmlAttrValue());
            serial.endTag(null, "enum");
        }
    }

    @Override
    public String toString() {
        return String.format("ResEnum{parent=%s, type=0x%04x, min=%d, max=%d, l10n=%d, symbols=%s}",
            mParent, mType, mMin, mMax, mL10n, mSymbols);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof ResEnum) {
            ResEnum other = (ResEnum) obj;
            return Objects.equals(mParent, other.mParent)
                    && mType == other.mType
                    && mMin == other.mMin
                    && mMax == other.mMax
                    && mL10n == other.mL10n
                    && Objects.equals(mSymbols, other.mSymbols);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mParent, mType, mMin, mMax, mL10n, mSymbols);
    }
}
