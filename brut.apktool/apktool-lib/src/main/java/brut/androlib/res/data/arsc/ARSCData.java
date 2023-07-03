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
package brut.androlib.res.data.arsc;

import brut.androlib.exceptions.AndrolibException;
import brut.androlib.res.data.ResPackage;

import java.util.logging.Logger;

public class ARSCData {
    private final ResPackage[] mPackages;
    private final FlagsOffset[] mFlagsOffsets;

    public ARSCData(ResPackage[] packages, FlagsOffset[] flagsOffsets) {
        mPackages = packages;
        mFlagsOffsets = flagsOffsets;
    }

    public FlagsOffset[] getFlagsOffsets() {
        return mFlagsOffsets;
    }

    public ResPackage[] getPackages() {
        return mPackages;
    }

    public ResPackage getOnePackage() throws AndrolibException {
        if (mPackages.length == 0) {
            throw new AndrolibException("Arsc file contains zero packages");
        } else if (mPackages.length != 1) {
            int id = findPackageWithMostResSpecs();
            LOGGER.info("Arsc file contains multiple packages. Using package "
                + mPackages[id].getName() + " as default.");

            return mPackages[id];
        }
        return mPackages[0];
    }

    public int findPackageWithMostResSpecs() {
        int count = mPackages[0].getResSpecCount();
        int id = 0;

        for (int i = 0; i < mPackages.length; i++) {
            if (mPackages[i].getResSpecCount() >= count) {
                count = mPackages[i].getResSpecCount();
                id = i;
            }
        }
        return id;
    }

    private static final Logger LOGGER = Logger.getLogger(ARSCData.class.getName());
}
