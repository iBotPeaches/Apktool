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
package brut.androlib;

import brut.androlib.meta.ApkInfo;
import brut.androlib.res.table.ResId;
import brut.androlib.res.table.ResPackage;
import brut.androlib.res.table.ResTable;
import brut.androlib.res.table.value.ResPrimitive;
import brut.androlib.res.table.value.ResReference;
import brut.androlib.res.table.value.ResStyle;
import org.junit.Test;

/**
 * Regression tests: split APKs may reference types that are missing from their partial resource tables.
 * We should not abort decoding when we can't inject dummy entry specs for those references.
 */
public class ResolveKeysMissingTypeSpecTest {
    private static final int PKG_ID_APP = 0x7f;
    private static final int TYPE_ID_MISSING = 0x04;

    private static ResPackage newEmptyAppPackage(Config config) throws Exception {
        ApkInfo apkInfo = new ApkInfo();
        ResTable table = new ResTable(apkInfo, config);
        return table.addPackageGroup(PKG_ID_APP, "app").getBasePackage();
    }

    @Test
    public void styleResolveKeys_skipsMissingTypeSpec() throws Exception {
        Config config = new Config("TEST");
        ResPackage pkg = newEmptyAppPackage(config);

        ResReference parent = new ResReference(pkg, ResId.of(PKG_ID_APP, 0x01, 0x0000));
        ResReference key = new ResReference(pkg, ResId.of(PKG_ID_APP, TYPE_ID_MISSING, 0x0001));
        ResStyle.Item item = new ResStyle.Item(key, ResPrimitive.FALSE);
        ResStyle style = new ResStyle(parent, new ResStyle.Item[]{item});

        style.resolveKeys();
    }
}
