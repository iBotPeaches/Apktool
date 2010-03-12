/*
 *  Copyright 2010 Ryszard Wiśniewski <brut.alll@gmail.com>.
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
 *  under the License.
 */

package brut.androlib.res.decoder;

import brut.androlib.AndrolibException;
import brut.androlib.res.data.*;
import brut.androlib.res.data.value.ResValue;
import brut.androlib.res.data.value.ResValueFactory;
import brut.androlib.res.jni.*;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public class JniPackageDecoder {
    public ResPackage decode(JniPackage jniPkg, ResTable resTable)
            throws AndrolibException {
        ResPackage pkg = new ResPackage(resTable, jniPkg.id, jniPkg.name);
        ResValueFactory valueFactory = pkg.getValueFactory();

        JniConfig[] jniConfigs = jniPkg.configs;
        for (int i = 0; i < jniConfigs.length; i++) {
            JniConfig jniConfig = jniConfigs[i];

            ResConfigFlags flags = new ResConfigFlags(jniConfig);
            ResConfig config;
            if (pkg.hasConfig(flags)) {
                config = pkg.getConfig(flags);
            } else {
                config = new ResConfig(flags);
                pkg.addConfig(config);
            }

            JniEntry[] jniEntries = jniConfig.entries;
            for (int j = 0; j < jniEntries.length; j++) {
                JniEntry jniEntry = jniEntries[j];

                ResType type;
                String typeName = jniEntry.type;
                if (pkg.hasType(typeName)) {
                    type = pkg.getType(typeName);
                } else {
                    type = new ResType(typeName, resTable, pkg);
                    pkg.addType(type);
                }

                ResID resID = new ResID(jniEntry.resID);
                ResResSpec spec;
                if (pkg.hasResSpec(resID)) {
                    spec = pkg.getResSpec(resID);
                } else {
                    spec = new ResResSpec(resID, jniEntry.name, pkg, type);
                    pkg.addResSpec(spec);
                    type.addResSpec(spec);
                }

                ResValue value = valueFactory.factory(jniEntry);
                ResResource res =
                    new ResResource(config, spec, value);

                config.addResource(res);
                spec.addResource(res);
                pkg.addResource(res);
            }
        }

        return pkg;
    }
}
