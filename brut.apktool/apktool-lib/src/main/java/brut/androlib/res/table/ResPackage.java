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
import java.util.Set;

public class ResPackage {
    private final ResPackageGroup mGroup;
    private final Map<Integer, ResTypeSpec> mTypeSpecs;
    private final Map<Pair<Integer, ResConfig>, ResType> mTypes;
    private final Map<ResId, ResEntrySpec> mEntrySpecs;
    private final Map<Pair<ResId, ResConfig>, ResEntry> mEntries;
    private final Map<String, ResOverlayable> mOverlayables;
    private final Map<ResId, ResId> mAliases;
    private final Set<String> mNameRegistry;

    public ResPackage(ResPackageGroup owner) {
        assert owner != null;
        mGroup = owner;
        mTypeSpecs = new HashMap<>();
        mTypes = new HashMap<>();
        mEntrySpecs = new HashMap<>();
        mEntries = new HashMap<>();
        mOverlayables = new HashMap<>();
        mAliases = new HashMap<>();
        mNameRegistry = new HashSet<>();
    }

    public ResTable getTable() {
        return mGroup.getTable();
    }

    public ResPackageGroup getGroup() {
        return mGroup;
    }

    public int getId() {
        return mGroup.getId();
    }

    public String getName() {
        return mGroup.getName();
    }

    public boolean hasTypeSpec(int typeId) {
        return mTypeSpecs.containsKey(typeId);
    }

    public ResTypeSpec getTypeSpec(int typeId) throws UndefinedResObjectException {
        ResTypeSpec typeSpec = mTypeSpecs.get(typeId);
        if (typeSpec == null) {
            throw new UndefinedResObjectException(
                String.format("type spec: pkgId=0x%02x, typeId=0x%02x", getId(), typeId));
        }
        return typeSpec;
    }

    public ResTypeSpec addTypeSpec(int typeId, String typeName) throws AndrolibException {
        ResTypeSpec typeSpec = mTypeSpecs.get(typeId);
        if (typeSpec != null) {
            throw new AndrolibException(
                String.format("Repeated type spec: pkgId=0x%02x, typeId=0x%02x, typeName=%s",
                    getId(), typeId, typeName));
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

    public boolean hasType(int typeId) {
        return hasType(typeId, ResConfig.DEFAULT);
    }

    public boolean hasType(int typeId, ResConfig config) {
        Pair<Integer, ResConfig> typeKey = Pair.of(typeId, config);
        return mTypes.containsKey(typeKey);
    }

    public ResType getType(int typeId) throws UndefinedResObjectException {
        return getType(typeId, ResConfig.DEFAULT);
    }

    public ResType getType(int typeId, ResConfig config) throws UndefinedResObjectException {
        Pair<Integer, ResConfig> typeKey = Pair.of(typeId, config);
        ResType type = mTypes.get(typeKey);
        if (type == null) {
            throw new UndefinedResObjectException(
                String.format("type: pkgId=0x%02x, typeId=0x%02x, config=%s", getId(), typeId, config));
        }
        return type;
    }

    public ResType addType(int typeId) throws UndefinedResObjectException {
        return addType(typeId, ResConfig.DEFAULT);
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

    public boolean hasEntrySpec(int typeId, int entryId) {
        ResId resId = ResId.of(getId(), typeId, entryId);
        if (mAliases.containsKey(resId)) {
            resId = mAliases.get(resId);
            typeId = resId.typeId();
            entryId = resId.entryId();
        }

        return mEntrySpecs.containsKey(resId);
    }

    public ResEntrySpec getEntrySpec(int typeId, int entryId) throws UndefinedResObjectException {
        ResId resId = ResId.of(getId(), typeId, entryId);
        if (mAliases.containsKey(resId)) {
            resId = mAliases.get(resId);
            typeId = resId.typeId();
            entryId = resId.entryId();
        }

        ResEntrySpec entrySpec = mEntrySpecs.get(resId);
        if (entrySpec == null) {
            throw new UndefinedResObjectException(
                String.format("entry spec: pkgId=0x%02x, typeId=0x%02x, entryId=0x%04x", getId(), typeId, entryId));
        }
        return entrySpec;
    }

    public ResEntrySpec addEntrySpec(int typeId, int entryId, String name) throws AndrolibException {
        ResId resId = ResId.of(getId(), typeId, entryId);
        if (mAliases.containsKey(resId)) {
            resId = mAliases.get(resId);
            typeId = resId.typeId();
            entryId = resId.entryId();
        }

        ResEntrySpec entrySpec = mEntrySpecs.get(resId);
        if (entrySpec != null) {
            throw new AndrolibException(
                String.format("Repeated entry spec: pkgId=0x%02x, typeId=0x%02x, entryId=0x%04x",
                    getId(), typeId, entryId));
        }

        ResTypeSpec typeSpec = getTypeSpec(typeId);

        // Some apps had their entry names obfuscated or collapsed to a single value in the key string pool.
        // Enforce uniqueness by forcing a rename when that happens.
        if (name != null && mNameRegistry.contains(typeSpec.getName() + "/" + name)) {
            name = "";
        }

        entrySpec = new ResEntrySpec(typeSpec, entryId, name);
        mEntrySpecs.put(resId, entrySpec);

        // Register the name to enforce uniqueness.
        mNameRegistry.add(typeSpec.getName() + "/" + entrySpec.getName());

        return entrySpec;
    }

    public int getEntrySpecCount() {
        return mEntrySpecs.size();
    }

    public Collection<ResEntrySpec> listEntrySpecs() {
        return mEntrySpecs.values();
    }

    public boolean hasEntry(int typeId, int entryId) {
        return hasEntry(typeId, entryId, ResConfig.DEFAULT);
    }

    public boolean hasEntry(int typeId, int entryId, ResConfig config) {
        ResId resId = ResId.of(getId(), typeId, entryId);
        if (mAliases.containsKey(resId)) {
            resId = mAliases.get(resId);
            typeId = resId.typeId();
            entryId = resId.entryId();
        }

        Pair<ResId, ResConfig> entryKey = Pair.of(resId, config);
        return mEntries.containsKey(entryKey);
    }

    public ResEntry getEntry(int typeId, int entryId) throws UndefinedResObjectException {
        return getEntry(typeId, entryId, ResConfig.DEFAULT);
    }

    public ResEntry getEntry(int typeId, int entryId, ResConfig config) throws UndefinedResObjectException {
        ResId resId = ResId.of(getId(), typeId, entryId);
        if (mAliases.containsKey(resId)) {
            resId = mAliases.get(resId);
            typeId = resId.typeId();
            entryId = resId.entryId();
        }

        Pair<ResId, ResConfig> entryKey = Pair.of(resId, config);
        ResEntry entry = mEntries.get(entryKey);
        if (entry == null) {
            throw new UndefinedResObjectException(
                String.format("entry: pkgId=0x%02x, typeId=0x%02x, entryId=0x%04x, config=%s",
                    getId(), typeId, entryId, config));
        }
        return entry;
    }

    public ResEntry addEntry(int typeId, int entryId, ResValue value) throws AndrolibException {
        return addEntry(typeId, entryId, ResConfig.DEFAULT, value);
    }

    public ResEntry addEntry(int typeId, int entryId, ResConfig config, ResValue value) throws AndrolibException {
        ResId resId = ResId.of(getId(), typeId, entryId);
        if (mAliases.containsKey(resId)) {
            resId = mAliases.get(resId);
            typeId = resId.typeId();
            entryId = resId.entryId();
        }

        Pair<ResId, ResConfig> entryKey = Pair.of(resId, config);
        ResEntry entry = mEntries.get(entryKey);
        if (entry != null) {
            throw new AndrolibException(
                String.format("Repeated entry: pkgId=0x%02x, typeId=0x%02x, entryId=0x%04x, config=%s",
                    getId(), typeId, entryId, config));
        }

        ResEntrySpec entrySpec = getEntrySpec(typeId, entryId);
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
            throw new UndefinedResObjectException(
                String.format("overlayable: pkgId=0x%02x, name=%s", getId(), name));
        }
        return overlayable;
    }

    public ResOverlayable addOverlayable(String name, String actor) throws AndrolibException {
        ResOverlayable overlayable = mOverlayables.get(name);
        if (overlayable != null) {
            throw new AndrolibException(
                String.format("Repeated overlayable: pkgId=0x%02x, name=%s", getId(), name));
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

    public boolean isAlias(ResId resId) {
        return mAliases.containsKey(resId);
    }

    public ResId resolveAlias(ResId aliasId) throws UndefinedResObjectException {
        ResId resId = mAliases.get(aliasId);
        if (resId == null) {
            throw new UndefinedResObjectException(
                String.format("alias: pkgId=0x%02x, aliasId=%s", getId(), aliasId));
        }
        return resId;
    }

    public void addAlias(ResId aliasId, ResId finalId) throws AndrolibException {
        if (mAliases.containsKey(aliasId)) {
            throw new AndrolibException(
                String.format("Repeated alias: pkgId=0x%02x, aliasId=%s", getId(), aliasId));
        }

        mAliases.put(aliasId, finalId);
    }

    public int getAliasCount() {
        return mAliases.size();
    }

    public Map<ResId, ResId> getAliases() {
        return mAliases;
    }

    @Override
    public String toString() {
        return String.format("ResPackage{id=0x%02x, name=%s}", getId(), getName());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof ResPackage) {
            ResPackage other = (ResPackage) obj;
            return mGroup.equals(other.mGroup);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return mGroup.hashCode();
    }
}
