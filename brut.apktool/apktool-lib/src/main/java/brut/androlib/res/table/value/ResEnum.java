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

import android.util.TypedValue;
import brut.androlib.Config;
import brut.androlib.exceptions.AndrolibException;
import brut.androlib.res.table.ResConfig;
import brut.androlib.res.table.ResEntry;
import brut.androlib.res.table.ResEntrySpec;
import brut.androlib.res.table.ResId;
import brut.androlib.res.table.ResPackage;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

public class ResEnum extends ResAttribute {
    private static final Logger LOGGER = Logger.getLogger(ResEnum.class.getName());

    private final Symbol[] mSymbols;
    private Map<Integer, Symbol[]> mSymbolsCache;
    private Map<Integer, String> mFormatsCache;

    public ResEnum(ResReference parent, int type, int min, int max, int l10n, Symbol[] symbols) {
        super(parent, type, min, max, l10n);
        mSymbols = symbols;
    }

    @Override
    public void resolveKeys() throws AndrolibException {
        ResPackage pkg = mParent.getPackage();
        Config config = pkg.getTable().getConfig();
        boolean skipUnresolved = config.getDecodeResolve() == Config.DecodeResolve.LAZY;

        for (Symbol symbol : mSymbols) {
            ResReference key = symbol.getKey();
            if (key.resolve() != null) {
                continue;
            }

            ResId entryId = key.getId();

            // #2836 - Skip item if the resource cannot be resolved.
            if (skipUnresolved || entryId.getPackageId() != pkg.getId()) {
                LOGGER.warning(String.format(
                    "null enum reference: key=%s, value=%s", key, symbol.getValue()));
                continue;
            }

            pkg.addEntrySpec(entryId, ResEntrySpec.DUMMY_PREFIX + entryId);
            pkg.addEntry(entryId, ResConfig.DEFAULT, ResCustom.ID);
        }
    }

    @Override
    protected Symbol[] getSymbolsForValue(ResItem value) throws AndrolibException {
        if (!isSymbolValueType(value)) {
            return null;
        }

        int data = ((ResPrimitive) value).getData();
        return getSymbols(data);
    }

    private boolean isSymbolValueType(ResItem value) throws AndrolibException {
        if (!(value instanceof ResPrimitive)) {
            return false;
        }

        int type = ((ResPrimitive) value).getType();
        return type == TypedValue.TYPE_INT_DEC || type == TypedValue.TYPE_INT_HEX;
    }

    private Symbol[] getSymbols(int data) throws AndrolibException {
        if (mSymbolsCache == null) {
            // Lazily establish a symbols cache for performance.
            mSymbolsCache = new HashMap<>();
        } else if (mSymbolsCache.containsKey(data)) {
            return mSymbolsCache.get(data);
        }

        Symbol[] symbols = new Symbol[mSymbols.length];
        int symbolsCount = 0;

        for (Symbol symbol : mSymbols) {
            if (symbol.getValue().getData() == data) {
                symbols[symbolsCount++] = symbol;
            }
        }

        if (symbolsCount == 0) {
            symbols = null;
        } else if (symbolsCount < symbols.length) {
            symbols = Arrays.copyOf(symbols, symbolsCount);
        }

        mSymbolsCache.put(data, symbols);
        return symbols;
    }

    @Override
    protected String formatValueToSymbols(ResItem value) throws AndrolibException {
        if (!isSymbolValueType(value)) {
            return null;
        }

        int data = ((ResPrimitive) value).getData();
        if (mFormatsCache == null) {
            // Lazily establish a formats cache for performance.
            mFormatsCache = new HashMap<>();
        } else if (mFormatsCache.containsKey(data)) {
            return mFormatsCache.get(data);
        }

        Symbol[] symbols = getSymbols(data);
        String formatted = null;

        if (symbols != null) {
            for (Symbol symbol : symbols) {
                ResEntrySpec keySpec = symbol.getKey().resolve();
                if (keySpec == null) {
                    continue;
                }

                formatted = keySpec.getName();

                // fill_parent is deprecated since API 8 but appears first.
                // Keep looking for match_parent and use it instead if found.
                if (data == -1 && formatted.equals("fill_parent")) {
                    continue;
                }
                break;
            }
        }

        mFormatsCache.put(data, formatted);
        return formatted;
    }

    @Override
    protected void serializeSymbolsToValuesXml(XmlSerializer serial, ResEntry entry)
            throws AndrolibException, IOException {
        for (Symbol symbol : mSymbols) {
            ResEntrySpec keySpec = symbol.getKey().resolve();
            if (keySpec == null) {
                continue;
            }

            serial.startTag(null, "enum");
            serial.attribute(null, "name", keySpec.getName());
            serial.attribute(null, "value", symbol.getValue().encodeAsResXmlAttrValue());
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
