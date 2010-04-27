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
package brut.androlib.src;

import brut.androlib.AndrolibException;
import java.util.ListIterator;
import org.jf.dexlib.Code.Opcode;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public class DebugInjector {

    public static void inject(ListIterator<String> it, StringBuilder out)
            throws AndrolibException {
        new DebugInjector(it, out).inject();
    }

    private DebugInjector(ListIterator<String> it, StringBuilder out) {
        mIt = it;
        mOut = out;
    }

    private void inject() throws AndrolibException {
        String definition = nextAndAppend();
        if (definition.contains(" abstract ")) {
            nextAndAppend();
            return;
        }
        injectParameters(definition);

        boolean end = false;
        while (!end) {
            end = step();
        }
    }

    private void injectParameters(String definition) throws AndrolibException {
        int pos = definition.indexOf('(');
        if (pos == -1) {
            throw new AndrolibException();
        }
        int pos2 = definition.indexOf(')', pos);
        if (pos2 == -1) {
            throw new AndrolibException();
        }
        String params = definition.substring(pos + 1, pos2);

        int i = definition.contains(" static ") ? 0 : 1;
        int argc = TypeName.listFromInternalName(params).size() + i;
        while(i < argc) {
            mOut.append(".parameter \"p").append(i).append("\"\n");
            i++;
        }
    }

    private boolean step() {
        String line = next();
        if (line.isEmpty()) {
            return false;
        }

        switch (line.charAt(0)) {
            case '#':
                return false;
            case ':':
                append(line);
                return false;
            case '.':
                return processDirective(line);
            default:
                return processInstruction(line);
        }
    }

    private boolean processDirective(String line) {
        String line2 = line.substring(1);
        if (
            line2.startsWith("line ") ||
            line2.equals("prologue") ||
            line2.startsWith("local ") ||
            line2.startsWith("parameter")
        ) {
            return false;
        }

        append(line);
        if (line2.equals("end method")) {
            return true;
        }
        if (
            line2.startsWith("annotation ") ||
            line2.equals("sparse-switch") ||
            line2.startsWith("packed-switch ") ||
            line2.startsWith("array-data ")
        ) {
            while(true) {
                line2 = nextAndAppend();
                if (line2.startsWith(".end ")) {
                    break;
                }
            }
        }
        return false;
    }

    private boolean processInstruction(String line) {
        if (mFirstInstruction) {
            mOut.append(".prologue\n");
            mFirstInstruction = false;
        }
        mOut.append(".line ").append(mIt.nextIndex()).append('\n')
            .append(line).append('\n');

        int pos = line.indexOf(' ');
        if (pos == -1) {
            return false;
        }
        Opcode opcode = Opcode.getOpcodeByName(line.substring(0, pos));
        if (! opcode.setsRegister()) {
            return false;
        }

        int pos2 = line.indexOf(',', pos);
        String register = pos2 == -1 ? line.substring(pos + 1) :
            line.substring(pos + 1, pos2);

        mOut.append(".local ").append(register).append(", ").append(register)
            .append(':').append(getRegisterTypeForOpcode(opcode)).append('\n');

        return false;
    }

    private String getRegisterTypeForOpcode(Opcode opcode) {
        switch (opcode.value) {
            case (byte)0x0d: // ?
            case (byte)0x1a:
            case (byte)0x1b:
            case (byte)0x1c: // ?
            case (byte)0x22:
            case (byte)0x23: // ?
            case (byte)0xf4: // ?
                return "Ljava/lang/Object;";
            case (byte)0x1f: // ?
            case (byte)0x20: // ?
                return "Z";
            case (byte)0x21: // ?
                return "I";
        }

        String name = opcode.name;
        int pos = name.lastIndexOf('-');
        if (pos != -1) {
            int pos2 = name.indexOf('/');
            String type = pos2 == -1 ? name.substring(pos + 1) :
                name.substring(pos + 1, pos2);

            if (type.equals("object")) {
                return "Ljava/lang/Object;";
            }
            if (type.equals("int")) {
                return "I";
            }
            if (type.equals("boolean")) {
                return "Z";
            }
            if (type.equals("float")) {
                return "F";
            }
            if (type.equals("double")) {
                return "D";
            }
            if (type.equals("long")) {
                return "J";
            }
            if (type.equals("byte")) {
                return "B";
            }
            if (type.equals("char")) {
                return "C";
            }
            if (type.equals("short")) {
                return "S";
            }
            if (type.equals("long")) {
                return "J";
            }
        }

        return "I";
    }

    private String next() {
        return mIt.next().trim();
    }

    private String nextAndAppend() {
        String line = next();
        append(line);
        return line;
    }

    private void append(String append) {
        mOut.append(append).append('\n');
    }

    private final ListIterator<String> mIt;
    private final StringBuilder mOut;

    private boolean mFirstInstruction = true;
}
