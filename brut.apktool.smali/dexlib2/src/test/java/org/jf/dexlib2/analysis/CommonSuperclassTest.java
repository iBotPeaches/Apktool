/*
 * Copyright 2013, Google Inc.
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

package org.jf.dexlib2.analysis;

import com.google.common.collect.ImmutableSet;
import junit.framework.Assert;
import org.jf.dexlib2.immutable.ImmutableDexFile;
import org.junit.Test;

import java.io.IOException;

public class CommonSuperclassTest {
    // object tree:
    // object
    //   one
    //     onetwo
    //       onetwothree
    //     onethree
    // five (undefined class)
    //   fivetwo
    //     fivetwothree
    //   fivethree

    private final ClassPath classPath;

    public CommonSuperclassTest() throws IOException {
        classPath = new ClassPath(new ImmutableDexFile(ImmutableSet.of(
                TestUtils.makeClassDef("Ljava/lang/Object;", null),
                TestUtils.makeClassDef("Ltest/one;", "Ljava/lang/Object;"),
                TestUtils.makeClassDef("Ltest/two;", "Ljava/lang/Object;"),
                TestUtils.makeClassDef("Ltest/onetwo;", "Ltest/one;"),
                TestUtils.makeClassDef("Ltest/onetwothree;", "Ltest/onetwo;"),
                TestUtils.makeClassDef("Ltest/onethree;", "Ltest/one;"),
                TestUtils.makeClassDef("Ltest/fivetwo;", "Ltest/five;"),
                TestUtils.makeClassDef("Ltest/fivetwothree;", "Ltest/fivetwo;"),
                TestUtils.makeClassDef("Ltest/fivethree;", "Ltest/five;"),
                TestUtils.makeInterfaceDef("Ljava/lang/Cloneable;"),
                TestUtils.makeInterfaceDef("Ljava/io/Serializable;"),

                // basic class and interface
                TestUtils.makeClassDef("Liface/classiface1;", "Ljava/lang/Object;", "Liface/iface1;"),
                TestUtils.makeInterfaceDef("Liface/iface1;"),

                // a more complex interface tree
                TestUtils.makeInterfaceDef("Liface/base1;"),
                // implements undefined interface
                TestUtils.makeInterfaceDef("Liface/sub1;", "Liface/base1;", "Liface/base2;"),
                // this implements sub1, so that its interfaces can't be fully resolved either
                TestUtils.makeInterfaceDef("Liface/sub2;", "Liface/base1;", "Liface/sub1;"),
                TestUtils.makeInterfaceDef("Liface/sub3;", "Liface/base1;"),
                TestUtils.makeInterfaceDef("Liface/sub4;", "Liface/base1;", "Liface/sub3;"),
                TestUtils.makeClassDef("Liface/classsub1;", "Ljava/lang/Object;", "Liface/sub1;"),
                TestUtils.makeClassDef("Liface/classsub2;", "Ljava/lang/Object;", "Liface/sub2;"),
                TestUtils.makeClassDef("Liface/classsub3;", "Ljava/lang/Object;", "Liface/sub3;", "Liface/base;"),
                TestUtils.makeClassDef("Liface/classsub4;", "Ljava/lang/Object;", "Liface/sub3;", "Liface/sub4;"),
                TestUtils.makeClassDef("Liface/classsubsub4;", "Liface/classsub4;"),
                TestUtils.makeClassDef("Liface/classsub1234;", "Ljava/lang/Object;", "Liface/sub1;", "Liface/sub2;",
                        "Liface/sub3;", "Liface/sub4;")
        )));
    }

    public void superclassTest(String commonSuperclass,
                                      String type1, String type2) {
        TypeProto commonSuperclassProto = classPath.getClass(commonSuperclass);
        TypeProto type1Proto = classPath.getClass(type1);
        TypeProto type2Proto = classPath.getClass(type2);

        Assert.assertSame(commonSuperclassProto, type1Proto.getCommonSuperclass(type2Proto));
        Assert.assertSame(commonSuperclassProto, type2Proto.getCommonSuperclass(type1Proto));
    }

    @Test
    public void testGetCommonSuperclass() throws IOException {
        String object = "Ljava/lang/Object;";
        String unknown = "Ujava/lang/Object;";
        String one = "Ltest/one;";
        String two = "Ltest/two;";
        String onetwo = "Ltest/onetwo;";
        String onetwothree = "Ltest/onetwothree;";
        String onethree = "Ltest/onethree;";
        String five = "Ltest/five;";
        String fivetwo = "Ltest/fivetwo;";
        String fivetwothree = "Ltest/fivetwothree;";
        String fivethree = "Ltest/fivethree;";

        // same object        
        superclassTest(object, object, object);
        superclassTest(unknown, unknown, unknown);
        superclassTest(one, one, one);
        superclassTest(onetwo, onetwo, onetwo);
        superclassTest(onetwothree, onetwothree, onetwothree);
        superclassTest(onethree, onethree, onethree);
        superclassTest(five, five, five);
        superclassTest(fivetwo, fivetwo, fivetwo);
        superclassTest(fivetwothree, fivetwothree, fivetwothree);
        superclassTest(fivethree, fivethree, fivethree);
        
        // same value, but different object
        Assert.assertEquals(
                onetwo,
                classPath.getClass(onetwo).getCommonSuperclass(new ClassProto(classPath, onetwo)).getType());

        // other object is superclass
        superclassTest(object, object, one);

        // other object is superclass two levels up
        superclassTest(object, object, onetwo);

        // unknown and non-object class
        superclassTest(unknown, one, unknown);

        // unknown and object class
        superclassTest(object, object, unknown);

        // siblings
        superclassTest(one, onetwo, onethree);

        // nephew
        superclassTest(one, onethree, onetwothree);

        // unrelated
        superclassTest(object, one, two);

        // undefined superclass and object
        superclassTest(object, fivetwo, object);

        // undefined class and unrelated type
        superclassTest(unknown, one, five);

        // undefined superclass and unrelated type
        superclassTest(unknown, one, fivetwo);

        // undefined ancestor and unrelated type
        superclassTest(unknown, one, fivetwothree);

        // undefined class and direct subclass
        superclassTest(five, five, fivetwo);

        // undefined class and descendent
        superclassTest(five, five, fivetwothree);

        // undefined superclass and direct subclass
        superclassTest(fivetwo, fivetwo, fivetwothree);

        // siblings with undefined superclass
        superclassTest(five, fivetwo, fivethree);

        // undefined superclass and nephew
        superclassTest(five, fivethree, fivetwothree);
    }

    @Test
    public void testGetCommonSuperclass_interfaces() {
        String classiface1 = "Liface/classiface1;";
        String iface1 = "Liface/iface1;";
        String base1 = "Liface/base1;";
        String base2 = "Liface/base2;";
        String sub1 = "Liface/sub1;";
        String sub2 = "Liface/sub2;";
        String sub3 = "Liface/sub3;";
        String sub4 = "Liface/sub4;";
        String classsub1 = "Liface/classsub1;";
        String classsub2 = "Liface/classsub2;";
        String classsub3 = "Liface/classsub3;";
        String classsub4 = "Liface/classsub4;";
        String classsubsub4 = "Liface/classsubsub4;";
        String classsub1234 = "Liface/classsub1234;";
        String object = "Ljava/lang/Object;";
        String unknown = "Ujava/lang/Object;";

        superclassTest(iface1, classiface1, iface1);

        superclassTest(base1, base1, base1);
        superclassTest(base1, base1, sub1);
        superclassTest(base1, base1, classsub1);
        superclassTest(base1, base1, sub2);
        superclassTest(base1, base1, classsub2);
        superclassTest(base1, base1, sub3);
        superclassTest(base1, base1, classsub3);
        superclassTest(base1, base1, sub4);
        superclassTest(base1, base1, classsub4);
        superclassTest(base1, base1, classsubsub4);
        superclassTest(base1, base1, classsub1234);

        superclassTest(object, sub3, iface1);
        superclassTest(unknown, sub2, iface1);
        superclassTest(unknown, sub1, iface1);

        superclassTest(base2, base2, sub1);
        superclassTest(base2, base2, classsub1);
        superclassTest(base2, base2, sub2);
        superclassTest(base2, base2, classsub2);
        superclassTest(base2, base2, classsub1234);

        superclassTest(unknown, iface1, classsub1234);

        superclassTest(sub1, sub1, classsub1);

        superclassTest(sub2, sub2, classsub2);
        superclassTest(sub1, sub1, classsub2);

        superclassTest(sub3, sub3, classsub3);

        superclassTest(sub4, sub4, classsub4);
        superclassTest(sub3, sub3, classsub4);
        superclassTest(object, sub2, classsub4);
        superclassTest(object, sub1, classsub4);

        superclassTest(sub1, sub2, sub1);

        superclassTest(sub1, sub1, classsub1234);
        superclassTest(sub2, sub2, classsub1234);
        superclassTest(sub3, sub3, classsub1234);
        superclassTest(sub4, sub4, classsub1234);

        superclassTest(unknown, sub3, classsub1);
        superclassTest(unknown, sub4, classsub1);
        superclassTest(unknown, sub3, classsub2);
        superclassTest(unknown, sub4, classsub2);

        superclassTest(unknown, sub4, base2);
        superclassTest(unknown, classsub4, base2);
    }

    @Test
    public void testGetCommonSuperclass_arrays() throws IOException {
        String object = "Ljava/lang/Object;";
        String one = "Ltest/one;";
        String unknown = "Ujava/lang/Object;";

        String cloneable = "Ljava/lang/Cloneable;";
        String serializable = "Ljava/io/Serializable;";

        String object1 = "[Ljava/lang/Object;";
        String one1 = "[Ltest/one;";
        String one2 = "[[Ltest/one;";
        String two1 = "[Ltest/two;";
        String onetwo1 = "[Ltest/onetwo;";
        String onetwo2 = "[[Ltest/onetwo;";
        String onethree1 = "[Ltest/onethree;";
        String onethree2 = "[[Ltest/onethree;";
        String five = "Ltest/five;";
        String five1 = "[Ltest/five;";
        String unknown1 = "[Ujava/lang/Object;";

        String int1 = "[I";
        String int2 = "[[I";
        String float1 = "[F";

        superclassTest(one1, one1, one1);
        superclassTest(object1, object1, one1);
        superclassTest(one1, onetwo1, onethree1);
        superclassTest(one1, one1, onethree1);
        superclassTest(object1, one1, two1);

        superclassTest(one2, one2, one2);
        superclassTest(one2, one2, onetwo2);
        superclassTest(one2, onetwo2, onethree2);
        superclassTest(object1, one1, one2);
        superclassTest(object1, two1, one2);

        superclassTest(unknown1, five1, one1);
        superclassTest(object1, five1, one2);

        superclassTest(unknown1, one1, unknown1);

        superclassTest(object, one1, one);
        superclassTest(object, object1, one);
        superclassTest(object, onetwo1, one);
        superclassTest(object, five1, one);
        superclassTest(object, one2, one);

        superclassTest(object, one1, unknown);
        superclassTest(object, unknown1, unknown);

        superclassTest(cloneable, one1, cloneable);
        superclassTest(serializable, one1, serializable);

        superclassTest(object, one1, five);

        superclassTest(int1, int1, int1);
        superclassTest(object, int1, float1);
        superclassTest(object, int1, int2);
    }
}
