package brut.androlib.meta;

import org.yaml.snakeyaml.constructor.AbstractConstruct;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.Tag;

/**
 * Created by rover12421 on 1/7/16.
 */
public class StringExConstructor extends Constructor {
    public StringExConstructor() {
        this.yamlConstructors.put(Tag.STR, new ConstructStringEx());
    }

    private class ConstructStringEx extends AbstractConstruct {
        public Object construct(Node node) {
            String val = (String) constructScalar((ScalarNode)node);
            return YamlStringEscapeUtils.unescapeString(val);
        }
    }
}