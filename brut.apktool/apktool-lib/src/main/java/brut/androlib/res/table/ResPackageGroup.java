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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

public class ResPackageGroup {
    private final ResTable mTable;
    private final int mId;
    private final String mName;
    private final List<ResPackage> mPackages;

    public ResPackageGroup(ResTable table, int id, String name) {
        assert table != null && id >= 0 && name != null;
        mTable = table;
        mId = id;
        mName = name;
        mPackages = new ArrayList<>();
        mPackages.add(new ResPackage(this));
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

    public int getPackageCount() {
        return mPackages.size();
    }

    public Collection<ResPackage> listPackages() {
        return mPackages;
    }

    public ResPackage getBasePackage() {
        return mPackages.get(0);
    }

    public Collection<ResPackage> listSubPackages() {
        return mPackages.subList(1, mPackages.size());
    }

    public ResPackage addSubPackage() {
        ResPackage pkg = new ResPackage(this);
        mPackages.add(pkg);
        return pkg;
    }

    public boolean hasTypeSpec(int typeId) {
        for (ResPackage pkg : mPackages) {
            if (pkg.hasTypeSpec(typeId)) {
                return true;
            }
        }
        return false;
    }

    public ResTypeSpec getTypeSpec(int typeId) throws UndefinedResObjectException {
        for (ResPackage pkg : mPackages) {
            try {
                return pkg.getTypeSpec(typeId);
            } catch (UndefinedResObjectException ignored) {
            }
        }
        throw new UndefinedResObjectException(
            String.format("type spec: pkgId=0x%02x, typeId=0x%02x", mId, typeId));
    }

    public boolean hasType(int typeId) {
        return hasType(typeId, ResConfig.DEFAULT);
    }

    public boolean hasType(int typeId, ResConfig config) {
        for (ResPackage pkg : mPackages) {
            if (pkg.hasType(typeId, config)) {
                return true;
            }
        }
        return false;
    }

    public ResType getType(int typeId) throws UndefinedResObjectException {
        return getType(typeId, ResConfig.DEFAULT);
    }

    public ResType getType(int typeId, ResConfig config) throws UndefinedResObjectException {
        for (ResPackage pkg : mPackages) {
            try {
                return pkg.getType(typeId, config);
            } catch (UndefinedResObjectException ignored) {
            }
        }
        throw new UndefinedResObjectException(
            String.format("type: pkgId=0x%02x, typeId=0x%02x, config=%s", mId, typeId, config));
    }

    public boolean hasEntrySpec(int typeId, int entryId) {
        for (ResPackage pkg : mPackages) {
            if (pkg.hasEntrySpec(typeId, entryId)) {
                return true;
            }
        }
        return false;
    }

    public ResEntrySpec getEntrySpec(int typeId, int entryId) throws UndefinedResObjectException {
        for (ResPackage pkg : mPackages) {
            try {
                return pkg.getEntrySpec(typeId, entryId);
            } catch (UndefinedResObjectException ignored) {
            }
        }
        throw new UndefinedResObjectException(
            String.format("entry spec: pkgId=0x%02x, typeId=0x%02x, entryId=0x%04x", mId, typeId, entryId));
    }

    public Iterable<ResEntrySpec> listEntrySpecs() {
        return () -> new Iterator<ResEntrySpec>() {
            private Iterator<ResEntrySpec> current = Collections.emptyIterator();
            private int index = 0;

            @Override
            public boolean hasNext() {
                while (!current.hasNext() && index < mPackages.size()) {
                    current = mPackages.get(index++).listEntrySpecs().iterator();
                }
                return current.hasNext();
            }

            @Override
            public ResEntrySpec next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return current.next();
            }
        };
    }

    public boolean hasEntry(int typeId, int entryId) {
        return hasEntry(typeId, entryId, ResConfig.DEFAULT);
    }

    public boolean hasEntry(int typeId, int entryId, ResConfig config) {
        for (ResPackage pkg : mPackages) {
            if (pkg.hasEntry(typeId, entryId, config)) {
                return true;
            }
        }
        return false;
    }

    public ResEntry getEntry(int typeId, int entryId) throws UndefinedResObjectException {
        return getEntry(typeId, entryId, ResConfig.DEFAULT);
    }

    public ResEntry getEntry(int typeId, int entryId, ResConfig config) throws UndefinedResObjectException {
        for (ResPackage pkg : mPackages) {
            try {
                return pkg.getEntry(typeId, entryId, config);
            } catch (UndefinedResObjectException ignored) {
            }
        }
        throw new UndefinedResObjectException(
            String.format("entry: pkgId=0x%02x, typeId=0x%02x, entryId=0x%04x, config=%s",
                mId, typeId, entryId, config));
    }

    public Iterable<ResEntry> listEntries() {
        return () -> new Iterator<ResEntry>() {
            private Iterator<ResEntry> current = Collections.emptyIterator();
            private int index = 0;

            @Override
            public boolean hasNext() {
                while (!current.hasNext() && index < mPackages.size()) {
                    current = mPackages.get(index++).listEntries().iterator();
                }
                return current.hasNext();
            }

            @Override
            public ResEntry next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return current.next();
            }
        };
    }

    @Override
    public String toString() {
        return String.format("ResPackageGroup{id=0x%02x, name=%s}", mId, mName);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof ResPackageGroup) {
            ResPackageGroup other = (ResPackageGroup) obj;
            return mId == other.mId
                && mName.equals(other.mName);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mId, mName);
    }
}
