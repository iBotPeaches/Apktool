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
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

public class ResFlags extends ResAttribute {
    private static final Logger LOGGER = Logger.getLogger(ResFlags.class.getName());

    private final Symbol[] mSymbols;
    private final Symbol[] mZeroFlags;
    private final Symbol[] mFlags;
    private final Map<Integer, String> mFormatCache;

    public ResFlags(ResReference parent, int type, int min, int max, int l10n, Symbol[] symbols) {
        super(parent, type, min, max, l10n);
        mSymbols = symbols;
        mFormatCache = new HashMap<>();

        Symbol[] zeroFlags = new Symbol[symbols.length];
        int zeroFlagsCount = 0;
        Symbol[] flags = new Symbol[symbols.length];
        int flagsCount = 0;

        for (Symbol symbol : symbols) {
            ResPrimitive value = symbol.getValue();

            if (value.getData() == 0) {
                zeroFlags[zeroFlagsCount++] = symbol;
            } else {
                flags[flagsCount++] = symbol;
            }
        }

        mZeroFlags = zeroFlagsCount < zeroFlags.length
            ? Arrays.copyOf(zeroFlags, zeroFlagsCount) : zeroFlags;
        mFlags = flagsCount < flags.length ? Arrays.copyOf(flags, flagsCount) : flags;

        // We establish a priority list for the flags. This can never be completely
        // accurate to the source, but it's a best-guess approach.
        Comparator<Symbol> byBitCount = Comparator.comparingInt(
            (Symbol symbol) -> Integer.bitCount(symbol.getValue().getData()));
        Comparator<Symbol> byRawValue = Comparator.comparingInt(
            (Symbol symbol) -> symbol.getValue().getData());
        Arrays.sort(mFlags, byBitCount.reversed().thenComparing(byRawValue));
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

        Symbol[] symbols;
        int count = 0;
        if (data == 0) {
            symbols = mZeroFlags;
            count = symbols.length;

            if (count == 0) {
                return null;
            }
        } else {
            symbols = new Symbol[mFlags.length];
            int mask = 0;

            for (Symbol symbol : mFlags) {
                int flag = symbol.getValue().getData();

                if ((data & flag) != flag || (mask & flag) == flag) {
                    continue;
                }

                symbols[count++] = symbol;
                mask |= flag;

                if (mask == data) {
                    break;
                }
            }

            if (count == 0) {
                LOGGER.warning("Invalid flags value: " + value);
                return null;
            }
        }

        // Render the flags as a format.
        Config config = mParent.getPackage().getTable().getConfig();
        boolean removeUnresolved = config.getDecodeResolve() == Config.DecodeResolve.REMOVE;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            Symbol symbol = symbols[i];

            // Filter out redundant flags.
            if (count > 2) {
                int mask = 0;

                // Combine the other flags.
                for (int j = 0; j < count; j++) {
                    Symbol other = symbols[j];

                    if (j != i) {
                        mask |= other.getValue().getData();
                    }
                }

                // Skip if it doesn't add at least one unique bit.
                if ((symbol.getValue().getData() & ~mask) == 0) {
                    continue;
                }
            }

            // Append the flag to the format.
            ResReference key = symbol.getKey();
            ResEntrySpec keySpec = key.resolve();
            if (sb.length() > 0) {
                sb.append('|');
            }
            if (keySpec != null) {
                sb.append(keySpec.getName());
            } else {
                // #2836 - Support skipping items if the resource cannot be identified.
                if (removeUnresolved) {
                    return null;
                }

                sb.append(ResEntrySpec.MISSING_PREFIX + key.getId());
            }
        }

        formatted = sb.toString();
        mFormatCache.put(data, formatted);
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
                        "null flag reference: key=%s, value=%s", key, value));
                    continue;
                }

                name = ResEntrySpec.MISSING_PREFIX + key.getId();
            }

            serial.startTag(null, "flag");
            serial.attribute(null, "name", name);
            serial.attribute(null, "value", value.encodeAsResXmlAttrValue());
            serial.endTag(null, "flag");
        }
    }

    @Override
    public String toString() {
        return String.format("ResFlags{parent=%s, type=0x%04x, min=%d, max=%d, l10n=%d, symbols=%s}",
            mParent, mType, mMin, mMax, mL10n, mSymbols);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof ResFlags) {
            ResFlags other = (ResFlags) obj;
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
