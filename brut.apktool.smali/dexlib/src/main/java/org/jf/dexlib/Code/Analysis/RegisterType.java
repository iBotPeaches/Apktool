/*
 * [The "BSD licence"]
 * Copyright (c) 2010 Ben Gruver (JesusFreke)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.jf.dexlib.Code.Analysis;

import org.jf.dexlib.TypeIdItem;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;

import static org.jf.dexlib.Code.Analysis.ClassPath.ClassDef;

public class RegisterType {
    private final static HashMap<RegisterType, RegisterType> internedRegisterTypes =
            new HashMap<RegisterType, RegisterType>();

    public final Category category;
    public final ClassDef type;

    private RegisterType(Category category, ClassDef type) {
        assert ((category == Category.Reference || category == Category.UninitRef || category == Category.UninitThis) &&
                    type != null) ||
               ((category != Category.Reference && category != Category.UninitRef && category != Category.UninitThis) &&
                    type == null);

        this.category = category;
        this.type = type;
    }

    @Override
    public String toString() {
        return "(" + category.name() + (type==null?"":("," + type.getClassType())) + ")";
    }

    public void writeTo(Writer writer) throws IOException {
        writer.write('(');
        writer.write(category.name());
        if (type != null) {
            writer.write(',');
            writer.write(type.getClassType());
        }
        writer.write(')');
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RegisterType that = (RegisterType) o;

        if (category != that.category) return false;
        if (type != null ? !type.equals(that.type) : that.type != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = category.hashCode();
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }

    public static enum Category {
        //the Unknown category denotes a register type that hasn't been determined yet
        Unknown,
        Uninit,
        Null,
        One,
        Boolean,
        Byte,
        PosByte,
        Short,
        PosShort,
        Char,
        Integer,
        Float,
        LongLo,
        LongHi,
        DoubleLo,
        DoubleHi,
        //the UninitRef category is used after a new-instance operation, and before the corresponding <init> is called
        UninitRef,
        //the UninitThis category is used the "this" register inside an <init> method, before the superclass' <init>
        //method is called
        UninitThis,
        Reference,
        //This is used when there are multiple incoming execution paths that have incompatible register types. For
        //example if the register's type is an Integer on one incomming code path, but is a Reference type on another
        //incomming code path. There is no register type that can hold either an Integer or a Reference.
        Conflicted;

        //this table is used when merging register types. For example, if a particular register can be either a Byte
        //or a Char, then the "merged" type of that register would be Integer, because it is the "smallest" type can
        //could hold either type of value.
        protected static Category[][] mergeTable  =
        {
                /*             Unknown      Uninit      Null        One,        Boolean     Byte        PosByte     Short       PosShort    Char        Integer,    Float,      LongLo      LongHi      DoubleLo    DoubleHi    UninitRef   UninitThis  Reference   Conflicted*/
                /*Unknown*/    {Unknown,    Uninit,     Null,       One,        Boolean,    Byte,       PosByte,    Short,      PosShort,   Char,       Integer,    Float,      LongLo,     LongHi,     DoubleLo,   DoubleHi,   UninitRef,  UninitThis, Reference,  Conflicted},
                /*Uninit*/     {Uninit,     Uninit,     Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted},
                /*Null*/       {Null,       Conflicted, Null,       Boolean,    Boolean,    Byte,       PosByte,    Short,      PosShort,   Char,       Integer,    Float,      Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Reference,  Conflicted},
                /*One*/        {One,        Conflicted, Boolean,    One,        Boolean,    Byte,       PosByte,    Short,      PosShort,   Char,       Integer,    Float,      Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted},
                /*Boolean*/    {Boolean,    Conflicted, Boolean,    Boolean,    Boolean,    Byte,       PosByte,    Short,      PosShort,   Char,       Integer,    Float,      Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted},
                /*Byte*/       {Byte,       Conflicted, Byte,       Byte,       Byte,       Byte,       Byte,       Short,      Short,      Integer,    Integer,    Float,      Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted},
                /*PosByte*/    {PosByte,    Conflicted, PosByte,    PosByte,    PosByte,    Byte,       PosByte,    Short,      PosShort,   Char,       Integer,    Float,      Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted},
                /*Short*/      {Short,      Conflicted, Short,      Short,      Short,      Short,      Short,      Short,      Short,      Integer,    Integer,    Float,      Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted},
                /*PosShort*/   {PosShort,   Conflicted, PosShort,   PosShort,   PosShort,   Short,      PosShort,   Short,      PosShort,   Char,       Integer,    Float,      Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted},
                /*Char*/       {Char,       Conflicted, Char,       Char,       Char,       Integer,    Char,       Integer,    Char,       Char,       Integer,    Float,      Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted},
                /*Integer*/    {Integer,    Conflicted, Integer,    Integer,    Integer,    Integer,    Integer,    Integer,    Integer,    Integer,    Integer,    Integer,    Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted},
                /*Float*/      {Float,      Conflicted, Float,      Float,      Float,      Float,      Float,      Float,      Float,      Float,      Integer,    Float,      Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted},
                /*LongLo*/     {LongLo,     Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, LongLo,     Conflicted, LongLo,     Conflicted, Conflicted, Conflicted, Conflicted, Conflicted},
                /*LongHi*/     {LongHi,     Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, LongHi,     Conflicted, LongHi,     Conflicted, Conflicted, Conflicted, Conflicted},
                /*DoubleLo*/   {DoubleLo,   Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, LongLo,     Conflicted, DoubleLo,   Conflicted, Conflicted, Conflicted, Conflicted, Conflicted},
                /*DoubleHi*/   {DoubleHi,   Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, LongHi,     Conflicted, DoubleHi,   Conflicted, Conflicted, Conflicted, Conflicted},
                /*UninitRef*/  {UninitRef,  Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted},
                /*UninitThis*/ {UninitThis, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, UninitThis, Conflicted, Conflicted},
                /*Reference*/  {Reference,  Conflicted, Reference,  Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Reference,  Conflicted},
                /*Conflicted*/ {Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted}
        };

        //this table is used to denote whether a given value type can be assigned to a "slot" of a certain type. For
        //example, to determine if you can assign a Boolean value to a particular array "slot", where the array is an
        //array of Integers, you would look up assignmentTable[Boolean.ordinal()][Integer.ordinal()]
        //Note that not all slot types in the table are expected to be used. For example, it doesn't make sense to
        //check if a value can be assigned to an uninitialized reference slot - because there is no such thing.
        protected static boolean[][] assigmentTable =
        {
                /*             Unknown      Uninit      Null        One,        Boolean     Byte        PosByte     Short       PosShort    Char        Integer,    Float,      LongLo      LongHi      DoubleLo    DoubleHi    UninitRef   UninitThis  Reference   Conflicted  |slot type*/
                /*Unknown*/    {false,      false,      false,      false,      false,      false,      false,      false,      false,      false,      false,      false,      false,      false,      false,      false,      false,      false,      false,      false},
                /*Uninit*/     {false,      false,      false,      false,      false,      false,      false,      false,      false,      false,      false,      false,      false,      false,      false,      false,      false,      false,      false,      false},
                /*Null*/       {false,      false,      true,       false,      true,       true,       true,       true,       true,       true,       true,       true,       false,      false,      false,      false,      false,      false,      true,       false},
                /*One*/        {false,      false,      false,      true,       true,       true,       true,       true,       true,       true,       true,       true,       false,      false,      false,      false,      false,      false,      false,      false},
                /*Boolean*/    {false,      false,      false,      false,      true,       true,       true,       true,       true,       true,       true,       true,       false,      false,      false,      false,      false,      false,      false,      false},
                /*Byte*/       {false,      false,      false,      false,      false,      true,       false,      true,       true,       false,      true,       true,       false,      false,      false,      false,      false,      false,      false,      false},
                /*PosByte*/    {false,      false,      false,      false,      false,      true,       true,       true,       true,       true,       true,       true,       false,      false,      false,      false,      false,      false,      false,      false},
                /*Short*/      {false,      false,      false,      false,      false,      false,      false,      true,       false,      false,      true,       true,       false,      false,      false,      false,      false,      false,      false,      false},
                /*PosShort*/   {false,      false,      false,      false,      false,      false,      false,      true,       true,       true,       true,       true,       false,      false,      false,      false,      false,      false,      false,      false},
                /*Char*/       {false,      false,      false,      false,      false,      false,      false,      false,      false,      true,       true,       true,       false,      false,      false,      false,      false,      false,      false,      false},
                /*Integer*/    {false,      false,      false,      false,      false,      false,      false,      false,      false,      false,      true,       true,       false,      false,      false,      false,      false,      false,      false,      false},
                /*Float*/      {false,      false,      false,      false,      false,      false,      false,      false,      false,      false,      true,       true,       false,      false,      false,      false,      false,      false,      false,      false},
                /*LongLo*/     {false,      false,      false,      false,      false,      false,      false,      false,      false,      false,      false,      false,      true,       false,      true,       false,      false,      false,      false,      false},
                /*LongHi*/     {false,      false,      false,      false,      false,      false,      false,      false,      false,      false,      false,      false,      false,      true,       false,      true,       false,      false,      false,      false},
                /*DoubleLo*/   {false,      false,      false,      false,      false,      false,      false,      false,      false,      false,      false,      false,      true,       false,      true,       false,      false,      false,      false,      false},
                /*DoubleHi*/   {false,      false,      false,      false,      false,      false,      false,      false,      false,      false,      false,      false,      false,      true,       false,      true,       false,      false,      false,      false},
                /*UninitRef*/  {false,      false,      false,      false,      false,      false,      false,      false,      false,      false,      false,      false,      false,      false,      false,      false,      false,      false,      false,      false},
                /*UninitThis*/ {false,      false,      false,      false,      false,      false,      false,      false,      false,      false,      false,      false,      false,      false,      false,      false,      false,      false,      false,      false},
                /*Reference*/  {false,      false,      false,      false,      false,      false,      false,      false,      false,      false,      false,      false,      false,      false,      false,      false,      false,      false,      true,       false},
                /*Conflicted*/ {false,      false,      false,      false,      false,      false,      false,      false,      false,      false,      false,      false,      false,      false,      false,      false,      false,      false,      false,      false}
                /*----------*/
                /*value type*/
        };

    }

    public static RegisterType getRegisterTypeForType(String type) {
        switch (type.charAt(0)) {
            case 'V':
                throw new ValidationException("The V type can only be used as a method return type");
            case 'Z':
                return getRegisterType(Category.Boolean, null);
            case 'B':
                return getRegisterType(Category.Byte, null);
            case 'S':
                return getRegisterType(Category.Short, null);
            case 'C':
                return getRegisterType(Category.Char, null);
            case 'I':
                return getRegisterType(Category.Integer, null);
            case 'F':
                return getRegisterType(Category.Float, null);
            case 'J':
                return getRegisterType(Category.LongLo, null);
            case 'D':
                return getRegisterType(Category.DoubleLo, null);
            case 'L':
            case '[':
                return getRegisterType(Category.Reference, ClassPath.getClassDef(type));
            default:
                throw new RuntimeException("Invalid type: " + type);
        }
    }

    public static RegisterType getRegisterTypeForTypeIdItem(TypeIdItem typeIdItem) {
        return getRegisterTypeForType(typeIdItem.getTypeDescriptor());
    }

    public static RegisterType getWideRegisterTypeForTypeIdItem(TypeIdItem typeIdItem, boolean firstRegister) {
        if (typeIdItem.getRegisterCount() == 1) {
            throw new RuntimeException("Cannot use this method for non-wide register type: " +
                    typeIdItem.getTypeDescriptor());
        }

        switch (typeIdItem.getTypeDescriptor().charAt(0)) {
            case 'J':
                if (firstRegister) {
                    return getRegisterType(Category.LongLo, null);
                } else {
                    return getRegisterType(Category.LongHi, null);
                }
            case 'D':
                if (firstRegister) {
                    return getRegisterType(Category.DoubleLo, null);
                } else {
                    return getRegisterType(Category.DoubleHi, null);
                }
            default:
                throw new RuntimeException("Invalid type: " + typeIdItem.getTypeDescriptor());
        }
    }

    public static RegisterType getRegisterTypeForLiteral(long literalValue) {
        if (literalValue < -32768) {
            return getRegisterType(Category.Integer, null);
        }
        if (literalValue < -128) {
            return getRegisterType(Category.Short, null);
        }
        if (literalValue < 0) {
            return getRegisterType(Category.Byte, null);
        }
        if (literalValue == 0) {
            return getRegisterType(Category.Null, null);
        }
        if (literalValue == 1) {
            return getRegisterType(Category.One, null);
        }
        if (literalValue < 128) {
            return getRegisterType(Category.PosByte, null);
        }
        if (literalValue < 32768) {
            return getRegisterType(Category.PosShort, null);
        }
        if (literalValue < 65536) {
            return getRegisterType(Category.Char, null);
        }
        return getRegisterType(Category.Integer, null);
    }

    public RegisterType merge(RegisterType type) {
        if (type == null || type == this) {
            return this;
        }

        Category mergedCategory = Category.mergeTable[this.category.ordinal()][type.category.ordinal()];

        ClassDef mergedType = null;
        if (mergedCategory == Category.Reference) {
            if (this.type instanceof ClassPath.UnresolvedClassDef ||
                type.type instanceof ClassPath.UnresolvedClassDef) {
                mergedType = ClassPath.getUnresolvedObjectClassDef();
            } else {
                mergedType = ClassPath.getCommonSuperclass(this.type, type.type);
            }
        } else if (mergedCategory == Category.UninitRef || mergedCategory == Category.UninitThis) {
            if (this.category == Category.Unknown) {
                return type;
            }
            assert type.category == Category.Unknown;
            return this;
        }
        return RegisterType.getRegisterType(mergedCategory, mergedType);
    }

    public boolean canBeAssignedTo(RegisterType slotType) {
        if (Category.assigmentTable[this.category.ordinal()][slotType.category.ordinal()]) {
            if (this.category == Category.Reference && slotType.category == Category.Reference) {
                if (!slotType.type.isInterface()) {
                    return this.type.extendsClass(slotType.type);
                }
                //for verification, we assume all objects implement all interfaces, so we don't verify the type if
                //slotType is an interface
            }
            return true;
        }
        return false;
    }

    public static RegisterType getUnitializedReference(ClassDef classType) {
        //We always create a new RegisterType instance for an uninit ref. Each unique uninit RegisterType instance
        //is used to track a specific uninitialized reference, so that if multiple registers contain the same
        //uninitialized reference, then they can all be upgraded to an initialized reference when the appropriate
        //<init> is invoked
        return new RegisterType(Category.UninitRef, classType);
    }

    public static RegisterType getRegisterType(Category category, ClassDef classType) {
        RegisterType newRegisterType = new RegisterType(category, classType);
        RegisterType internedRegisterType = internedRegisterTypes.get(newRegisterType);
        if (internedRegisterType == null) {
            internedRegisterTypes.put(newRegisterType, newRegisterType);
            return newRegisterType;
        }
        return internedRegisterType;
    }
}
