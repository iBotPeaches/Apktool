package brut.androlib.meta;

import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.representer.Representer;

/**
 * Created by rover12421 on 1/7/16.
 */
public class StringExRepresent extends Representer {
    public StringExRepresent() {
        RepresentStringEx representStringEx = new RepresentStringEx();
        multiRepresenters.put(String.class, representStringEx);
        representers.put(String.class, representStringEx);
    }

    private class RepresentStringEx extends RepresentString {

        @Override
        public Node representData(Object data) {
            return super.representData(YamlStringEscapeUtils.escapeString(data.toString()));
        }
    }
}
