/*
 * Copyright 2012, Google Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 *     * Neither the name of Google Inc. nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.jf.dexlib2;

import org.stringtemplate.v4.*;

import java.io.*;
import java.net.URL;

public class AccessorTestGenerator {
    private static class UnaryOperation {
        public final String name;
        public UnaryOperation(String name) {
            this.name = name;
        }
    }

    private static class BinaryOperation {
        public final String name;
        public final String[] inputTypes;
        public BinaryOperation(String name, String[] inputTypes) {
            this.name = name;
            this.inputTypes = inputTypes;
        }
    }

    private static class TypeDef {
        public final String name;
        public final UnaryOperation[] unaryOperations;
        public final BinaryOperation[] binaryOperations;
        public TypeDef(String name, UnaryOperation[] unaryOperations, BinaryOperation[] binaryOperations) {
            this.name = name;
            this.unaryOperations = unaryOperations;
            this.binaryOperations = binaryOperations;
        }
    }

    private static final UnaryOperation[] unaryOperations = new UnaryOperation[] {
            new UnaryOperation("preinc"),
            new UnaryOperation("postinc"),
            new UnaryOperation("predec"),
            new UnaryOperation("postdec")
    };

    private static final String[] booleanInputs = new String[] {"boolean"};
    private static final String[] integralInputs = new String[] {"int", "long"};
    private static final String[] allInputs = new String[] {"int", "float", "long", "double"};

    private static final BinaryOperation[] booleanOperations = new BinaryOperation[] {
            new BinaryOperation("and", booleanInputs),
            new BinaryOperation("or", booleanInputs),
            new BinaryOperation("xor", booleanInputs),
    };

    private static final BinaryOperation[] floatOperations = new BinaryOperation[] {
            new BinaryOperation("add", allInputs),
            new BinaryOperation("sub", allInputs),
            new BinaryOperation("mul", allInputs),
            new BinaryOperation("div", allInputs),
            new BinaryOperation("rem", allInputs),
    };

    private static final BinaryOperation[] integralOperations = new BinaryOperation[] {
            new BinaryOperation("add", allInputs),
            new BinaryOperation("sub", allInputs),
            new BinaryOperation("mul", allInputs),
            new BinaryOperation("div", allInputs),
            new BinaryOperation("rem", allInputs),
            new BinaryOperation("and", integralInputs),
            new BinaryOperation("or", integralInputs),
            new BinaryOperation("xor", integralInputs),
            new BinaryOperation("shl", integralInputs),
            new BinaryOperation("shr", integralInputs),
            new BinaryOperation("ushr", integralInputs),
    };

    private static final TypeDef[] types = new TypeDef[] {
            new TypeDef("boolean", new UnaryOperation[0], booleanOperations),
            new TypeDef("byte", unaryOperations, integralOperations),
            new TypeDef("char", unaryOperations, integralOperations),
            new TypeDef("short", unaryOperations, integralOperations),
            new TypeDef("int", unaryOperations, integralOperations),
            new TypeDef("long", unaryOperations, integralOperations),
            new TypeDef("float", unaryOperations, floatOperations),
            new TypeDef("double", unaryOperations, floatOperations),
    };


    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: java org.jf.dexlib2.AccessorTestGenerator <output_file>");
        }

        URL stgUrl = AccessorTestGenerator.class.getClassLoader().getResource("AccessorTest.stg");
        STGroupFile stg = new STGroupFile(stgUrl, "utf-8", '<', '>');
        ST fileSt = stg.getInstanceOf("file");
        fileSt.add("types", types);

        PrintWriter w = null;
        try {
            w = new PrintWriter(new BufferedWriter(new FileWriter(args[0])));
            w.print(fileSt.render());
        } finally {
            if (w != null) {
                w.close();
            }
        }
    }
}



