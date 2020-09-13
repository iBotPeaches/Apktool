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
package brut.androlib.meta;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.introspector.PropertyUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;

public class MetaInfo {
    public String version;
    public String apkFileName;
    public boolean isFrameworkApk;
    public UsesFramework usesFramework;
    public  Map<String, String> sdkInfo;
    public PackageInfo packageInfo;
    public VersionInfo versionInfo;
    public boolean compressionType;
    public boolean sharedLibrary;
    public boolean sparseResources;
    public Map<String, String> unknownFiles;
    public Collection<String> doNotCompress;

    private static Yaml getYaml() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

        StringExRepresent representer = new StringExRepresent();
        PropertyUtils propertyUtils = representer.getPropertyUtils();
        propertyUtils.setSkipMissingProperties(true);

        return new Yaml(new StringExConstructor(), representer, options);
    }

    public void save(Writer output) {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        getYaml().dump(this, output);
    }

    public void save(File file) throws IOException {
        try(
                FileOutputStream fos = new FileOutputStream(file);
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
                Writer writer = new BufferedWriter(outputStreamWriter)
        ) {
            save(writer);
        }
    }

    public static MetaInfo load(InputStream is) {
        return getYaml().loadAs(is, MetaInfo.class);
    }
}
