package brut.androlib.meta;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.introspector.PropertyUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;

/**
 * Created by rover12421 on 1/6/16.
 */
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
                ){
            save(writer);
        }
    }

    public static MetaInfo load(InputStream is) {
        return getYaml().loadAs(is, MetaInfo.class);
    }

    public static MetaInfo load(File file) throws IOException {
        try (
                InputStream fis = new FileInputStream(file)
        ){
            return load(fis);
        }
    }
}
