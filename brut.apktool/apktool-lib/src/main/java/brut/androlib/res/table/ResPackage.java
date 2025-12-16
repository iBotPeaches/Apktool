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

import brut.androlib.exceptions.AndrolibException;
import brut.androlib.exceptions.UndefinedResObjectException;
import brut.androlib.res.table.value.ResValue;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;

public class ResPackage {
    private static final Logger LOGGER = Logger.getLogger(ResPackage.class.getName());

    private final ResTable mTable;
    private final int mId;
    private final String mName;
    private final Map<Integer, ResTypeSpec> mTypeSpecs;
    private final Map<Pair<Integer, ResConfig>, ResType> mTypes;
    private final Map<ResId, ResEntrySpec> mEntrySpecs;
    private final Map<Integer, Set<String>> mEntryNames;
    private final Map<Pair<ResId, ResConfig>, ResEntry> mEntries;
    private final Map<String, ResOverlayable> mOverlayables;

    public ResPackage(ResTable table, int id, String name) {
        mTable = table;
        mId = id;
        mName = name;
        mTypeSpecs = new HashMap<>();
        mTypes = new HashMap<>();
        mEntrySpecs = new HashMap<>();
        mEntryNames = new HashMap<>();
        mEntries = new HashMap<>();
        mOverlayables = new HashMap<>();
    }

    public ResTable getTable() {
        return mTable;
    }

    public int getId() {
        return mId;
    }

    public String getName() {
        return mName;
    }

    public boolean hasTypeSpec(int typeId) {
        return mTypeSpecs.containsKey(typeId);
    }

    public ResTypeSpec getTypeSpec(int typeId) throws UndefinedResObjectException {
        ResTypeSpec typeSpec = mTypeSpecs.get(typeId);
        if (typeSpec == null) {
            throw new UndefinedResObjectException(String.format(
                "type spec: typeId=0x%02x", typeId));
        }
        return typeSpec;
    }

    public ResTypeSpec addTypeSpec(int typeId, String typeName) throws AndrolibException {
        ResTypeSpec typeSpec = mTypeSpecs.get(typeId);
        if (typeSpec != null) {
            throw new AndrolibException(String.format(
                "Repeated type spec: typeId=0x%02x, typeName=%s", typeId, typeName));
        }

        typeSpec = new ResTypeSpec(this, typeId, typeName);
        mTypeSpecs.put(typeId, typeSpec);
        return typeSpec;
    }

    public int getTypeSpecCount() {
        return mTypeSpecs.size();
    }

    public Collection<ResTypeSpec> listTypeSpecs() {
        return mTypeSpecs.values();
    }

    public boolean hasType(int typeId, ResConfig config) {
        Pair<Integer, ResConfig> typeKey = Pair.of(typeId, config);
        return mTypes.containsKey(typeKey);
    }

    public ResType getType(int typeId, ResConfig config) throws UndefinedResObjectException {
        Pair<Integer, ResConfig> typeKey = Pair.of(typeId, config);
        ResType type = mTypes.get(typeKey);
        if (type == null) {
            throw new UndefinedResObjectException(String.format(
                "type: typeId=0x%02x, config=%s", typeId, config));
        }
        return type;
    }

    public ResType addType(int typeId, ResConfig config) throws UndefinedResObjectException {
        Pair<Integer, ResConfig> typeKey = Pair.of(typeId, config);
        ResType type = mTypes.get(typeKey);
        if (type != null) {
            // We can safely skip adding existing types.
            return type;
        }

        ResTypeSpec typeSpec = getTypeSpec(typeId);
        type = new ResType(typeSpec, config);
        mTypes.put(typeKey, type);
        return type;
    }

    public int getTypeCount() {
        return mTypes.size();
    }

    public Collection<ResType> listTypes() {
        return mTypes.values();
    }

    public boolean hasEntrySpec(ResId id) {
        return mEntrySpecs.containsKey(id);
    }

    public ResEntrySpec getEntrySpec(ResId id) throws UndefinedResObjectException {
        ResEntrySpec entrySpec = mEntrySpecs.get(id);
        if (entrySpec == null) {
            throw new UndefinedResObjectException(String.format("entry spec: id=%s", id));
        }
        return entrySpec;
    }

    public ResEntrySpec addEntrySpec(ResId id, String name) throws AndrolibException {
        ResEntrySpec entrySpec = mEntrySpecs.get(id);
        if (entrySpec != null) {
            throw new AndrolibException(String.format("Repeated entry spec: id=%s", id));
        }

        int typeId = id.getTypeId();
        ResTypeSpec typeSpec = getTypeSpec(typeId);

        // Obfuscation can cause specs to be generated using existing names.
        // Enforce uniqueness by renaming the spec when that happens.
        Set<String> entryNames = mEntryNames.get(typeId);
        if (entryNames == null) {
            entryNames = new HashSet<>();
            mEntryNames.put(typeId, entryNames);
        } else if (entryNames.contains(name)) {
            // Clear the name to force a rename.
            name = "";
        }

        entrySpec = new ResEntrySpec(typeSpec, id, name);
        mEntrySpecs.put(id, entrySpec);

        // Record the name to enforce uniqueness.
        entryNames.add(entrySpec.getName());

        return entrySpec;
    }

    public int getEntrySpecCount() {
        return mEntrySpecs.size();
    }

    public Collection<ResEntrySpec> listEntrySpecs() {
        return mEntrySpecs.values();
    }

    public boolean hasEntry(ResId id, ResConfig config) {
        return mEntries.containsKey(Pair.of(id, config));
    }

    public ResEntry getDefaultEntry(ResId id) throws UndefinedResObjectException {
        return getEntry(id, ResConfig.DEFAULT);
    }

    public ResEntry getEntry(ResId id, ResConfig config) throws UndefinedResObjectException {
        Pair<ResId, ResConfig> entryKey = Pair.of(id, config);
        ResEntry entry = mEntries.get(entryKey);
        if (entry == null) {
            throw new UndefinedResObjectException(String.format(
                "entry: id=%s, config=%s", id, config));
        }
        return entry;
    }

    public ResEntry addEntry(ResId id, ResConfig config, ResValue value) throws AndrolibException {
        Pair<ResId, ResConfig> entryKey = Pair.of(id, config);
        ResEntry entry = mEntries.get(entryKey);
        if (entry != null) {
            throw new AndrolibException(String.format(
                "Repeated entry: id=%s, config=%s", id, config));
        }

        ResEntrySpec entrySpec = getEntrySpec(id);
        int typeId = id.getTypeId();
        Pair<Integer, ResConfig> typeKey = Pair.of(typeId, config);
        ResType type = mTypes.get(typeKey);
        if (type == null) {
            // We can safely create the type if it's missing.
            ResTypeSpec typeSpec = getTypeSpec(typeId);
            type = new ResType(typeSpec, config);
            mTypes.put(typeKey, type);
        }

        entry = new ResEntry(type, entrySpec, value);
        mEntries.put(entryKey, entry);
        return entry;
    }

    public int getEntryCount() {
        return mEntries.size();
    }

    public Collection<ResEntry> listEntries() {
        return mEntries.values();
    }

    public boolean hasOverlayable(String name) {
        return mOverlayables.containsKey(name);
    }

    public ResOverlayable getOverlayable(String name) throws UndefinedResObjectException {
        ResOverlayable overlayable = mOverlayables.get(name);
        if (overlayable == null) {
            throw new UndefinedResObjectException(String.format("overlayable: name=%s", name));
        }
        return overlayable;
    }

    public ResOverlayable addOverlayable(String name, String actor) throws AndrolibException {
        ResOverlayable overlayable = mOverlayables.get(name);
        if (overlayable != null) {
            throw new AndrolibException(String.format("Repeated overlayable: name=%s", name));
        }

        overlayable = new ResOverlayable(this, name, actor);
        mOverlayables.put(name, overlayable);
        return overlayable;
    }

    public int getOverlayableCount() {
        return mOverlayables.size();
    }

    public Collection<ResOverlayable> listOverlayables() {
        return mOverlayables.values();
    }

    @Override
    public String toString() {
        return String.format("ResPackage{id=0x%02x, name=%s}", mId, mName);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof ResPackage) {
            ResPackage other = (ResPackage) obj;
            return Objects.equals(mTable, other.mTable)
                    && mId == other.mId
                    && Objects.equals(mName, other.mName);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mTable, mId, mName);
    }
}
