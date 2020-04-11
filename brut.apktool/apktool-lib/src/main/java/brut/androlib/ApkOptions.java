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
package brut.androlib;

import java.util.Collection;

public class ApkOptions {
    public boolean forceBuildAll = false;
    public boolean forceDeleteFramework = false;
    public boolean debugMode = false;
    public boolean verbose = false;
    public boolean copyOriginalFiles = false;
    public boolean updateFiles = false;
    public boolean isFramework = false;
    public boolean resourcesAreCompressed = false;
    public boolean useAapt2 = false;
    public boolean noCrunch = false;
    public int forceApi = 0;
    public Collection<String> doNotCompress;

    public String frameworkFolderLocation = null;
    public String frameworkTag = null;
    public String aaptPath = "";

    public int aaptVersion = 1; // default to v1

    public boolean isAapt2() {
        return this.useAapt2 || this.aaptVersion == 2;
    }
}
