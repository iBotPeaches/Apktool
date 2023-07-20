package brut.androlib.apk;

import brut.androlib.exceptions.AndrolibException;

public interface YamlSerializable {
    void readItem(YamlReader reader) throws AndrolibException;
    void write(YamlWriter writer);
}
