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

import java.io.IOException;

public class ResourcesInfo implements YamlSerializable {
    private Integer mPackageId;
    private String mPackageName;
    private Boolean mSparseEntries;
    private Boolean mCompactEntries;
    private Boolean mKeepRawValues;

    public ResourcesInfo() {
        clear();
    }

    public void clear() {
        mPackageId = null;
        mPackageName = null;
        mSparseEntries = null;
        mCompactEntries = null;
        mKeepRawValues = null;
    }

    public boolean isEmpty() {
        return mPackageId == null
            && mPackageName == null
            && mSparseEntries == null
            && mCompactEntries == null
            && mKeepRawValues == null;
    }

    @Override
    public void onEntry(YamlPullParser parser) throws IOException {
        switch (parser.getKey()) {
            case "packageId":
                mPackageId = parser.getInt();
                break;
            case "packageName":
                mPackageName = parser.getString();
                break;
            case "sparseEntries":
                mSparseEntries = parser.getBool();
                break;
            case "compactEntries":
                mCompactEntries = parser.getBool();
                break;
            case "keepRawValues":
                mKeepRawValues = parser.getBool();
                break;
        }
    }

    @Override
    public void serialize(YamlSerializer serial) throws IOException {
        if (mPackageId != null) {
            serial.writeInt("packageId", mPackageId);
        }
        if (mPackageName != null) {
            serial.writeString("packageName", mPackageName);
        }
        if (mSparseEntries != null) {
            serial.writeBool("sparseEntries", mSparseEntries);
        }
        if (mCompactEntries != null) {
            serial.writeBool("compactEntries", mCompactEntries);
        }
        if (mKeepRawValues != null) {
            serial.writeBool("keepRawValues", mKeepRawValues);
        }
    }

    public int getPackageId() {
        return mPackageId != null ? mPackageId : -1;
    }

    public void setPackageId(int packageId) {
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

    public boolean isKeepRawValues() {
        return mKeepRawValues != null ? mKeepRawValues : false;
    }

    public void setKeepRawValues(boolean keepRawValues) {
        mKeepRawValues = keepRawValues;
    }
}
