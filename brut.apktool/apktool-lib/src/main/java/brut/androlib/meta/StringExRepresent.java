/**
 *  Copyright (C) 2018 Ryszard Wiśniewski <brut.alll@gmail.com>
 *  Copyright (C) 2018 Connor Tumbleson <connor.tumbleson@gmail.com>
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

import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.representer.Representer;

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
