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
package brut.androlib.meta;

import brut.yaml.*;

public class ResourcesInfo implements YamlSerializable {
    private String mPackageId;
    private String mPackageName;
    private Boolean mSparseEntries;
    private Boolean mCompactEntries;

    public ResourcesInfo() {
        clear();
    }

    public void clear() {
        mPackageId = null;
        mPackageName = null;
        mSparseEntries = null;
        mCompactEntries = null;
    }

    public boolean isEmpty() {
        return mPackageId == null && mPackageName == null
                && mSparseEntries == null && mCompactEntries == null;
    }

    @Override
    public void readItem(YamlReader reader) {
        YamlLine line = reader.getLine();
        switch (line.getKey()) {
            case "packageId": {
                mPackageId = line.getValue();
                break;
            }
            case "packageName": {
                mPackageName = line.getValue();
                break;
            }
            case "sparseEntries": {
                mSparseEntries = line.getValueBool();
                break;
            }
            case "compactEntries": {
                mCompactEntries = line.getValueBool();
                break;
            }
        }
    }

    @Override
    public void write(YamlWriter writer) {
        if (mPackageId != null) {
            writer.writeString("packageId", mPackageId);
        }
        if (mPackageName != null) {
            writer.writeString("packageName", mPackageName);
        }
        if (mSparseEntries != null) {
            writer.writeBool("sparseEntries", mSparseEntries);
        }
        if (mCompactEntries != null) {
            writer.writeBool("compactEntries", mCompactEntries);
        }
    }

    public String getPackageId() {
        return mPackageId;
    }

    public void setPackageId(String packageId) {
        mPackageId = packageId;
    }

    public String getPackageName() {
        return mPackageName;
    }

    public void setPackageName(String packageName) {
        mPackageName = packageName;
    }

    public boolean isSparseEntries() {
        return mSparseEntries != null ? mSparseEntries : false;
    }

    public void setSparseEntries(boolean sparseEntries) {
        mSparseEntries = sparseEntries;
    }

    public boolean isCompactEntries() {
        return mCompactEntries != null ? mCompactEntries : false;
    }

    public void setCompactEntries(boolean compactEntries) {
        mCompactEntries = compactEntries;
    }
}
