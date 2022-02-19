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
package brut.androlib.meta;

import org.yaml.snakeyaml.constructor.AbstractConstruct;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.Tag;
import java.util.ArrayList;
import java.util.List;

public class ClassSafeConstructor extends Constructor {
    protected final List<Class<?>> allowableClasses = new ArrayList<>();

    public ClassSafeConstructor() {
        this.yamlConstructors.put(Tag.STR, new ConstructStringEx());

        this.allowableClasses.add(MetaInfo.class);
        this.allowableClasses.add(PackageInfo.class);
        this.allowableClasses.add(UsesFramework.class);
        this.allowableClasses.add(VersionInfo.class);
    }

    protected Object newInstance(Node node) {
        if (this.yamlConstructors.containsKey(node.getTag()) || this.allowableClasses.contains(node.getType())) {
            return super.newInstance(node);
        }
        throw new YAMLException("Invalid Class attempting to be constructed: " + node.getTag());
    }

    protected Object finalizeConstruction(Node node, Object data) {
        if (this.yamlConstructors.containsKey(node.getTag()) || this.allowableClasses.contains(node.getType())) {
            return super.finalizeConstruction(node, data);
        }

        return this.newInstance(node);
    }

    private class ConstructStringEx extends AbstractConstruct {
        public Object construct(Node node) {
            String val = constructScalar((ScalarNode) node);
            return YamlStringEscapeUtils.unescapeString(val);
        }
    }
}
