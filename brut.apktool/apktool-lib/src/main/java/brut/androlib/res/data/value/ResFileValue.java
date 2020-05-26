/*
 *  Copyright (C) 2010 Ryszard Wiśniewski <brut.alll@gmail.com>
 *  Copyright (C) 2010 Connor Tumbleson <connor.tumbleson@gmail.com>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package brut.androlib.res.data.value;

import brut.androlib.AndrolibException;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public class ResFileValue extends ResIntBasedValue {
    private final String mPath;

    public ResFileValue(String path, int rawIntValue) {
        super(rawIntValue);
        this.mPath = path;
    }

    public String getPath() {
        return mPath;
    }

    public String getStrippedPath() throws AndrolibException {
        if (mPath.startsWith("res/")) {
            return mPath.substring(4);
        }
        if (mPath.startsWith("r/") || mPath.startsWith("R/")) {
            return mPath.substring(2);
        }
        throw new AndrolibException("File path does not start with \"res/\" or \"r/\": " + mPath);
    }

    @Override
    public String toString() {
        return mPath;
    }
}
