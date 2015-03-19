/*
 * [The "BSD licence"]
 * Copyright (c) 2010 Ben Gruver
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
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

tree grammar smaliTreeWalker;

options {
  tokenVocab=smaliParser;
  ASTLabelType=CommonTree;
}

@header {
package org.jf.smali;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.antlr.runtime.BitSet;
import org.antlr.runtime.*;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.TreeNodeStream;
import org.antlr.runtime.tree.TreeParser;
import org.antlr.runtime.tree.TreeRuleReturnScope;
import org.jf.dexlib2.*;
import org.jf.dexlib2.builder.Label;
import org.jf.dexlib2.builder.MethodImplementationBuilder;
import org.jf.dexlib2.builder.SwitchLabelElement;
import org.jf.dexlib2.builder.instruction.*;
import org.jf.dexlib2.iface.Annotation;
import org.jf.dexlib2.iface.AnnotationElement;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.reference.FieldReference;
import org.jf.dexlib2.iface.reference.MethodReference;
import org.jf.dexlib2.iface.value.EncodedValue;
import org.jf.dexlib2.immutable.ImmutableAnnotation;
import org.jf.dexlib2.immutable.ImmutableAnnotationElement;
import org.jf.dexlib2.immutable.reference.ImmutableFieldReference;
import org.jf.dexlib2.immutable.reference.ImmutableMethodReference;
import org.jf.dexlib2.immutable.reference.ImmutableReference;
import org.jf.dexlib2.immutable.reference.ImmutableTypeReference;
import org.jf.dexlib2.immutable.value.*;
import org.jf.dexlib2.util.MethodUtil;
import org.jf.dexlib2.writer.InstructionFactory;
import org.jf.dexlib2.writer.builder.*;
import org.jf.util.LinearSearch;

import java.util.*;
}

@members {
  public String classType;
  private boolean verboseErrors = false;
  private int apiLevel = 15;
  private Opcodes opcodes = new Opcodes(apiLevel, false);
  private DexBuilder dexBuilder;

  public void setDexBuilder(DexBuilder dexBuilder) {
      this.dexBuilder = dexBuilder;
  }

  public void setApiLevel(int apiLevel, boolean experimental) {
      this.opcodes = new Opcodes(apiLevel, experimental);
      this.apiLevel = apiLevel;
  }

  public void setVerboseErrors(boolean verboseErrors) {
    this.verboseErrors = verboseErrors;
  }

  private byte parseRegister_nibble(String register)
      throws SemanticException {
    int totalMethodRegisters = method_stack.peek().totalMethodRegisters;
    int methodParameterRegisters = method_stack.peek().methodParameterRegisters;

    //register should be in the format "v12"
    int val = Byte.parseByte(register.substring(1));
    if (register.charAt(0) == 'p') {
      val = totalMethodRegisters - methodParameterRegisters + val;
    }
    if (val >= 2<<4) {
      throw new SemanticException(input, "The maximum allowed register in this context is list of registers is v15");
    }
    //the parser wouldn't have accepted a negative register, i.e. v-1, so we don't have to check for val<0;
    return (byte)val;
  }

  //return a short, because java's byte is signed
  private short parseRegister_byte(String register)
      throws SemanticException {
    int totalMethodRegisters = method_stack.peek().totalMethodRegisters;
    int methodParameterRegisters = method_stack.peek().methodParameterRegisters;
    //register should be in the format "v123"
    int val = Short.parseShort(register.substring(1));
    if (register.charAt(0) == 'p') {
      val = totalMethodRegisters - methodParameterRegisters + val;
    }
    if (val >= 2<<8) {
      throw new SemanticException(input, "The maximum allowed register in this context is v255");
    }
    return (short)val;
  }

  //return an int because java's short is signed
  private int parseRegister_short(String register)
      throws SemanticException {
    int totalMethodRegisters = method_stack.peek().totalMethodRegisters;
    int methodParameterRegisters = method_stack.peek().methodParameterRegisters;
    //register should be in the format "v12345"
    int val = Integer.parseInt(register.substring(1));
    if (register.charAt(0) == 'p') {
      val = totalMethodRegisters - methodParameterRegisters + val;
    }
    if (val >= 2<<16) {
      throw new SemanticException(input, "The maximum allowed register in this context is v65535");
    }
    //the parser wouldn't accept a negative register, i.e. v-1, so we don't have to check for val<0;
    return val;
  }

  public String getErrorMessage(RecognitionException e, String[] tokenNames) {
    if ( e instanceof SemanticException ) {
      return e.getMessage();
    } else {
      return super.getErrorMessage(e, tokenNames);
    }
  }

  public String getErrorHeader(RecognitionException e) {
    return getSourceName()+"["+ e.line+","+e.charPositionInLine+"]";
  }
}

smali_file returns[ClassDef classDef]
  : ^(I_CLASS_DEF header methods fields annotations)
  {
    $classDef = dexBuilder.internClassDef($header.classType, $header.accessFlags, $header.superType,
            $header.implementsList, $header.sourceSpec, $annotations.annotations, $fields.fields, $methods.methods);
  };
  catch [Exception ex] {
    if (verboseErrors) {
      ex.printStackTrace(System.err);
    }
    reportError(new SemanticException(input, ex));
  }


header returns[String classType, int accessFlags, String superType, List<String> implementsList, String sourceSpec]
: class_spec super_spec? implements_list source_spec
  {
    classType = $class_spec.type;
    $classType = classType;
    $accessFlags = $class_spec.accessFlags;
    $superType = $super_spec.type;
    $implementsList = $implements_list.implementsList;
    $sourceSpec = $source_spec.source;
  };


class_spec returns[String type, int accessFlags]
  : CLASS_DESCRIPTOR access_list
  {
    $type = $CLASS_DESCRIPTOR.text;
    $accessFlags = $access_list.value;
  };

super_spec returns[String type]
  : ^(I_SUPER CLASS_DESCRIPTOR)
  {
    $type = $CLASS_DESCRIPTOR.text;
  };


implements_spec returns[String type]
  : ^(I_IMPLEMENTS CLASS_DESCRIPTOR)
  {
    $type = $CLASS_DESCRIPTOR.text;
  };

implements_list returns[List<String> implementsList]
@init { List<String> typeList; }
  : {typeList = Lists.newArrayList();}
    (implements_spec {typeList.add($implements_spec.type);} )*
  {
    if (typeList.size() > 0) {
      $implementsList = typeList;
    } else {
      $implementsList = null;
    }
  };

source_spec returns[String source]
  : {$source = null;}
    ^(I_SOURCE string_literal {$source = $string_literal.value;})
  | /*epsilon*/;

access_list returns [int value]
  @init
  {
    $value = 0;
  }
  : ^(I_ACCESS_LIST
      (
        ACCESS_SPEC
        {
          $value |= AccessFlags.getAccessFlag($ACCESS_SPEC.getText()).getValue();
        }
      )*);


fields returns[List<BuilderField> fields]
  @init {$fields = Lists.newArrayList();}
  : ^(I_FIELDS
      (field
      {
        $fields.add($field.field);
      })*);

methods returns[List<BuilderMethod> methods]
  @init {$methods = Lists.newArrayList();}
  : ^(I_METHODS
      (method
      {
        $methods.add($method.ret);
      })*);

field returns [BuilderField field]
  :^(I_FIELD SIMPLE_NAME access_list ^(I_FIELD_TYPE nonvoid_type_descriptor) field_initial_value annotations?)
  {
    int accessFlags = $access_list.value;


    if (!AccessFlags.STATIC.isSet(accessFlags) && $field_initial_value.encodedValue != null) {
        throw new SemanticException(input, "Initial field values can only be specified for static fields.");
    }

    $field = dexBuilder.internField(classType, $SIMPLE_NAME.text, $nonvoid_type_descriptor.type, $access_list.value,
            $field_initial_value.encodedValue, $annotations.annotations);
  };


field_initial_value returns[EncodedValue encodedValue]
  : ^(I_FIELD_INITIAL_VALUE literal) {$encodedValue = $literal.encodedValue;}
  | /*epsilon*/;

literal returns[EncodedValue encodedValue]
  : integer_literal { $encodedValue = new ImmutableIntEncodedValue($integer_literal.value); }
  | long_literal { $encodedValue = new ImmutableLongEncodedValue($long_literal.value); }
  | short_literal { $encodedValue = new ImmutableShortEncodedValue($short_literal.value); }
  | byte_literal { $encodedValue = new ImmutableByteEncodedValue($byte_literal.value); }
  | float_literal { $encodedValue = new ImmutableFloatEncodedValue($float_literal.value); }
  | double_literal { $encodedValue = new ImmutableDoubleEncodedValue($double_literal.value); }
  | char_literal { $encodedValue = new ImmutableCharEncodedValue($char_literal.value); }
  | string_literal { $encodedValue = new ImmutableStringEncodedValue($string_literal.value); }
  | bool_literal { $encodedValue = ImmutableBooleanEncodedValue.forBoolean($bool_literal.value); }
  | NULL_LITERAL { $encodedValue = ImmutableNullEncodedValue.INSTANCE; }
  | type_descriptor { $encodedValue = new ImmutableTypeEncodedValue($type_descriptor.type); }
  | array_literal { $encodedValue = new ImmutableArrayEncodedValue($array_literal.elements); }
  | subannotation { $encodedValue = new ImmutableAnnotationEncodedValue($subannotation.annotationType, $subannotation.elements); }
  | field_literal { $encodedValue = new ImmutableFieldEncodedValue($field_literal.value); }
  | method_literal { $encodedValue = new ImmutableMethodEncodedValue($method_literal.value); }
  | enum_literal { $encodedValue = new ImmutableEnumEncodedValue($enum_literal.value); };

//everything but string
fixed_64bit_literal_number returns[Number value]
  : integer_literal { $value = $integer_literal.value; }
  | long_literal { $value = $long_literal.value; }
  | short_literal { $value = $short_literal.value; }
  | byte_literal { $value = $byte_literal.value; }
  | float_literal { $value = Float.floatToRawIntBits($float_literal.value); }
  | double_literal { $value = Double.doubleToRawLongBits($double_literal.value); }
  | char_literal { $value = (int)$char_literal.value; }
  | bool_literal { $value = $bool_literal.value?1:0; };

fixed_64bit_literal returns[long value]
  : integer_literal { $value = $integer_literal.value; }
  | long_literal { $value = $long_literal.value; }
  | short_literal { $value = $short_literal.value; }
  | byte_literal { $value = $byte_literal.value; }
  | float_literal { $value = Float.floatToRawIntBits($float_literal.value); }
  | double_literal { $value = Double.doubleToRawLongBits($double_literal.value); }
  | char_literal { $value = $char_literal.value; }
  | bool_literal { $value = $bool_literal.value?1:0; };

//everything but string and double
//long is allowed, but it must fit into an int
fixed_32bit_literal returns[int value]
  : integer_literal { $value = $integer_literal.value; }
  | long_literal { LiteralTools.checkInt($long_literal.value); $value = (int)$long_literal.value; }
  | short_literal { $value = $short_literal.value; }
  | byte_literal { $value = $byte_literal.value; }
  | float_literal { $value = Float.floatToRawIntBits($float_literal.value); }
  | char_literal { $value = $char_literal.value; }
  | bool_literal { $value = $bool_literal.value?1:0; };

array_elements returns[List<Number> elements]
  : {$elements = Lists.newArrayList();}
    ^(I_ARRAY_ELEMENTS
      (fixed_64bit_literal_number
      {
        $elements.add($fixed_64bit_literal_number.value);
      })*);

packed_switch_elements returns[List<Label> elements]
  @init {$elements = Lists.newArrayList();}
  :
    ^(I_PACKED_SWITCH_ELEMENTS
      (label_ref { $elements.add($label_ref.label); })*
    );

sparse_switch_elements returns[List<SwitchLabelElement> elements]
  @init {$elements = Lists.newArrayList();}
  :
    ^(I_SPARSE_SWITCH_ELEMENTS
       (fixed_32bit_literal label_ref
       {
         $elements.add(new SwitchLabelElement($fixed_32bit_literal.value, $label_ref.label));
       })*
    );

method returns[BuilderMethod ret]
  scope
  {
    boolean isStatic;
    int totalMethodRegisters;
    int methodParameterRegisters;
    MethodImplementationBuilder methodBuilder;
  }
  @init
  {
    $method::totalMethodRegisters = 0;
    $method::methodParameterRegisters = 0;
    int accessFlags = 0;
    $method::isStatic = false;
  }
  :
    ^(I_METHOD
      method_name_and_prototype
      access_list
      {
        accessFlags = $access_list.value;
        $method::isStatic = AccessFlags.STATIC.isSet(accessFlags);
        $method::methodParameterRegisters =
                MethodUtil.getParameterRegisterCount($method_name_and_prototype.parameters, $method::isStatic);
      }
      (
        (registers_directive
        {
          if ($registers_directive.isLocalsDirective) {
            $method::totalMethodRegisters = $registers_directive.registers + $method::methodParameterRegisters;
          } else {
            $method::totalMethodRegisters = $registers_directive.registers;
          }

          $method::methodBuilder = new MethodImplementationBuilder($method::totalMethodRegisters);

        })
        |
        /* epsilon */
        {
          $method::methodBuilder = new MethodImplementationBuilder(0);
        }
      )
      ordered_method_items
      catches
      parameters[$method_name_and_prototype.parameters]
      annotations
    )
  {
    MethodImplementation methodImplementation = null;
    List<BuilderTryBlock> tryBlocks = $catches.tryBlocks;

    boolean isAbstract = false;
    boolean isNative = false;

    if ((accessFlags & AccessFlags.ABSTRACT.getValue()) != 0) {
      isAbstract = true;
    } else if ((accessFlags & AccessFlags.NATIVE.getValue()) != 0) {
      isNative = true;
    }

    methodImplementation = $method::methodBuilder.getMethodImplementation();

    if (Iterables.isEmpty(methodImplementation.getInstructions())) {
      if (!isAbstract && !isNative) {
        throw new SemanticException(input, $I_METHOD, "A non-abstract/non-native method must have at least 1 instruction");
      }

      String methodType;
      if (isAbstract) {
        methodType = "an abstract";
      } else {
        methodType = "a native";
      }

      if ($registers_directive.start != null) {
        if ($registers_directive.isLocalsDirective) {
          throw new SemanticException(input, $registers_directive.start, "A .locals directive is not valid in \%s method", methodType);
        } else {
          throw new SemanticException(input, $registers_directive.start, "A .registers directive is not valid in \%s method", methodType);
        }
      }

      if (methodImplementation.getTryBlocks().size() > 0) {
        throw new SemanticException(input, $I_METHOD, "try/catch blocks cannot be present in \%s method", methodType);
      }

      if (!Iterables.isEmpty(methodImplementation.getDebugItems())) {
        throw new SemanticException(input, $I_METHOD, "debug directives cannot be present in \%s method", methodType);
      }

      methodImplementation = null;
    } else {
      if (isAbstract) {
        throw new SemanticException(input, $I_METHOD, "An abstract method cannot have any instructions");
      }
      if (isNative) {
        throw new SemanticException(input, $I_METHOD, "A native method cannot have any instructions");
      }

      if ($registers_directive.start == null) {
        throw new SemanticException(input, $I_METHOD, "A .registers or .locals directive must be present for a non-abstract/non-final method");
      }

      if ($method::totalMethodRegisters < $method::methodParameterRegisters) {
        throw new SemanticException(input, $registers_directive.start, "This method requires at least " +
                Integer.toString($method::methodParameterRegisters) +
                " registers, for the method parameters");
      }
    }

    $ret = dexBuilder.internMethod(
            classType,
            $method_name_and_prototype.name,
            $method_name_and_prototype.parameters,
            $method_name_and_prototype.returnType,
            accessFlags,
            $annotations.annotations,
            methodImplementation);
  };

method_prototype returns[List<String> parameters, String returnType]
  : ^(I_METHOD_PROTOTYPE ^(I_METHOD_RETURN_TYPE type_descriptor) method_type_list)
  {
    $returnType = $type_descriptor.type;
    $parameters = $method_type_list.types;
  };

method_name_and_prototype returns[String name, List<SmaliMethodParameter> parameters, String returnType]
  : SIMPLE_NAME method_prototype
  {
    $name = $SIMPLE_NAME.text;
    $parameters = Lists.newArrayList();

    int paramRegister = 0;
    for (String type: $method_prototype.parameters) {
        $parameters.add(new SmaliMethodParameter(paramRegister++, type));
        char c = type.charAt(0);
        if (c == 'D' || c == 'J') {
            paramRegister++;
        }
    }
    $returnType = $method_prototype.returnType;
  };

method_type_list returns[List<String> types]
  @init
  {
    $types = Lists.newArrayList();
  }
  : (
      nonvoid_type_descriptor
      {
        $types.add($nonvoid_type_descriptor.type);
      }
    )*;


method_reference returns[ImmutableMethodReference methodReference]
  : reference_type_descriptor? SIMPLE_NAME method_prototype
  {
    String type;
    if ($reference_type_descriptor.type == null) {
        type = classType;
    } else {
        type = $reference_type_descriptor.type;
    }
    $methodReference = new ImmutableMethodReference(type, $SIMPLE_NAME.text,
             $method_prototype.parameters, $method_prototype.returnType);
  };

field_reference returns[ImmutableFieldReference fieldReference]
  : reference_type_descriptor? SIMPLE_NAME nonvoid_type_descriptor
  {
    String type;
    if ($reference_type_descriptor.type == null) {
        type = classType;
    } else {
        type = $reference_type_descriptor.type;
    }
    $fieldReference = new ImmutableFieldReference(type, $SIMPLE_NAME.text,
            $nonvoid_type_descriptor.type);
  };

registers_directive returns[boolean isLocalsDirective, int registers]
  : {$registers = 0;}
    ^(( I_REGISTERS {$isLocalsDirective = false;}
      | I_LOCALS {$isLocalsDirective = true;}
      )
      short_integral_literal {$registers = $short_integral_literal.value & 0xFFFF;}
     );

label_def
  : ^(I_LABEL SIMPLE_NAME)
  {
    $method::methodBuilder.addLabel($SIMPLE_NAME.text);
  };

catches returns[List<BuilderTryBlock> tryBlocks]
  @init {tryBlocks = Lists.newArrayList();}
  : ^(I_CATCHES catch_directive* catchall_directive*);

catch_directive
  : ^(I_CATCH nonvoid_type_descriptor from=label_ref to=label_ref using=label_ref)
  {
    $method::methodBuilder.addCatch(dexBuilder.internTypeReference($nonvoid_type_descriptor.type),
        $from.label, $to.label, $using.label);
  };

catchall_directive
  : ^(I_CATCHALL from=label_ref to=label_ref using=label_ref)
  {
    $method::methodBuilder.addCatch($from.label, $to.label, $using.label);
  };

parameters[List<SmaliMethodParameter> parameters]
  : ^(I_PARAMETERS (parameter[parameters])*);

parameter[List<SmaliMethodParameter> parameters]
  : ^(I_PARAMETER REGISTER string_literal? annotations)
    {
        final int registerNumber = parseRegister_short($REGISTER.text);
        int totalMethodRegisters = $method::totalMethodRegisters;
        int methodParameterRegisters = $method::methodParameterRegisters;

        if (registerNumber >= totalMethodRegisters) {
            throw new SemanticException(input, $I_PARAMETER, "Register \%s is larger than the maximum register v\%d " +
                    "for this method", $REGISTER.text, totalMethodRegisters-1);
        }
        final int indexGuess = registerNumber - (totalMethodRegisters - methodParameterRegisters) - ($method::isStatic?0:1);

        if (indexGuess < 0) {
            throw new SemanticException(input, $I_PARAMETER, "Register \%s is not a parameter register.",
                    $REGISTER.text);
        }

        int parameterIndex = LinearSearch.linearSearch(parameters, SmaliMethodParameter.COMPARATOR,
            new WithRegister() { public int getRegister() { return indexGuess; } },
                indexGuess);

        if (parameterIndex < 0) {
            throw new SemanticException(input, $I_PARAMETER, "Register \%s is the second half of a wide parameter.",
                                $REGISTER.text);
        }

        SmaliMethodParameter methodParameter = parameters.get(parameterIndex);
        methodParameter.name = $string_literal.value;
        if ($annotations.annotations != null && $annotations.annotations.size() > 0) {
            methodParameter.annotations = $annotations.annotations;
        }
    };

debug_directive
  : line
  | local
  | end_local
  | restart_local
  | prologue
  | epilogue
  | source;

line
  : ^(I_LINE integral_literal)
    {
        $method::methodBuilder.addLineNumber($integral_literal.value);
    };

local
  : ^(I_LOCAL REGISTER ((NULL_LITERAL | name=string_literal) nonvoid_type_descriptor? signature=string_literal?)?)
    {
      int registerNumber = parseRegister_short($REGISTER.text);
      $method::methodBuilder.addStartLocal(registerNumber,
              dexBuilder.internNullableStringReference($name.value),
              dexBuilder.internNullableTypeReference($nonvoid_type_descriptor.type),
              dexBuilder.internNullableStringReference($signature.value));
    };

end_local
  : ^(I_END_LOCAL REGISTER)
    {
      int registerNumber = parseRegister_short($REGISTER.text);
      $method::methodBuilder.addEndLocal(registerNumber);
    };

restart_local
  : ^(I_RESTART_LOCAL REGISTER)
    {
      int registerNumber = parseRegister_short($REGISTER.text);
      $method::methodBuilder.addRestartLocal(registerNumber);
    };

prologue
  : I_PROLOGUE
    {
      $method::methodBuilder.addPrologue();
    };

epilogue
  : I_EPILOGUE
    {
      $method::methodBuilder.addEpilogue();
    };

source
  : ^(I_SOURCE string_literal?)
    {
      $method::methodBuilder.addSetSourceFile(dexBuilder.internNullableStringReference($string_literal.value));
    };

ordered_method_items
  : ^(I_ORDERED_METHOD_ITEMS (label_def | instruction | debug_directive)*);

label_ref returns[Label label]
  : SIMPLE_NAME { $label = $method::methodBuilder.getLabel($SIMPLE_NAME.text); };

register_list returns[byte[\] registers, byte registerCount]
  @init
  {
    $registers = new byte[5];
    $registerCount = 0;
  }
  : ^(I_REGISTER_LIST
      (REGISTER
      {
        if ($registerCount == 5) {
          throw new SemanticException(input, $I_REGISTER_LIST, "A list of registers can only have a maximum of 5 " +
                  "registers. Use the <op>/range alternate opcode instead.");
        }
        $registers[$registerCount++] = parseRegister_nibble($REGISTER.text);
      })*);

register_list4 returns[byte[\] registers, byte registerCount]
  @init
  {
    $registers = new byte[4];
    $registerCount = 0;
  }
  : ^(I_REGISTER_LIST
      (REGISTER
      {
        if ($registerCount == 4) {
          throw new SemanticException(input, $I_REGISTER_LIST, "A list4 of registers can only have a maximum of 4 " +
                  "registers. Use the <op>/range alternate opcode instead.");
        }
        $registers[$registerCount++] = parseRegister_nibble($REGISTER.text);
      })*);

register_range returns[int startRegister, int endRegister]
  : ^(I_REGISTER_RANGE (startReg=REGISTER endReg=REGISTER?)?)
    {
        if ($startReg == null) {
            $startRegister = 0;
            $endRegister = -1;
        } else {
                $startRegister  = parseRegister_short($startReg.text);
                if ($endReg == null) {
                    $endRegister = $startRegister;
                } else {
                    $endRegister = parseRegister_short($endReg.text);
                }

                int registerCount = $endRegister-$startRegister+1;
                if (registerCount < 1) {
                    throw new SemanticException(input, $I_REGISTER_RANGE, "A register range must have the lower register listed first");
                }
            }
    };

verification_error_reference returns[ImmutableReference reference]
  : CLASS_DESCRIPTOR
  {
    $reference = new ImmutableTypeReference($CLASS_DESCRIPTOR.text);
  }
  | field_reference
  {
    $reference = $field_reference.fieldReference;
  }
  | method_reference
  {
    $reference = $method_reference.methodReference;
  };

verification_error_type returns[int verificationError]
  : VERIFICATION_ERROR_TYPE
  {
    $verificationError = VerificationError.getVerificationError($VERIFICATION_ERROR_TYPE.text);
  };

instruction
  : insn_format10t
  | insn_format10x
  | insn_format11n
  | insn_format11x
  | insn_format12x
  | insn_format20bc
  | insn_format20t
  | insn_format21c_field
  | insn_format21c_string
  | insn_format21c_type
  | insn_format21c_lambda
  | insn_format21c_method
  | insn_format21ih
  | insn_format21lh
  | insn_format21s
  | insn_format21t
  | insn_format22b
  | insn_format22c_field
  | insn_format22c_type
  | insn_format22c_string
  | insn_format22s
  | insn_format22t
  | insn_format22x
  | insn_format23x
  | insn_format25x
  | insn_format30t
  | insn_format31c
  | insn_format31i
  | insn_format31t
  | insn_format32x
  | insn_format35c_method
  | insn_format35c_type
  | insn_format3rc_method
  | insn_format3rc_type
  | insn_format51l_type
  | insn_array_data_directive
  | insn_packed_switch_directive
  | insn_sparse_switch_directive;
  catch [Exception ex] {
    reportError(new SemanticException(input, $start, ex.getMessage()));
    recover(input, null);
  }

insn_format10t
  : //e.g. goto endloop:
    ^(I_STATEMENT_FORMAT10t INSTRUCTION_FORMAT10t label_ref)
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT10t.text);
      $method::methodBuilder.addInstruction(new BuilderInstruction10t(opcode, $label_ref.label));
    };

insn_format10x
  : //e.g. return
    ^(I_STATEMENT_FORMAT10x INSTRUCTION_FORMAT10x)
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT10x.text);
      $method::methodBuilder.addInstruction(new BuilderInstruction10x(opcode));
    };

insn_format11n
  : //e.g. const/4 v0, 5
    ^(I_STATEMENT_FORMAT11n INSTRUCTION_FORMAT11n REGISTER short_integral_literal)
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT11n.text);
      byte regA = parseRegister_nibble($REGISTER.text);

      short litB = $short_integral_literal.value;
      LiteralTools.checkNibble(litB);

      $method::methodBuilder.addInstruction(new BuilderInstruction11n(opcode, regA, litB));
    };

insn_format11x
  : //e.g. move-result-object v1
    ^(I_STATEMENT_FORMAT11x INSTRUCTION_FORMAT11x REGISTER)
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT11x.text);
      short regA = parseRegister_byte($REGISTER.text);

      $method::methodBuilder.addInstruction(new BuilderInstruction11x(opcode, regA));
    };

insn_format12x
  : //e.g. move v1 v2
    ^(I_STATEMENT_FORMAT12x INSTRUCTION_FORMAT12x registerA=REGISTER registerB=REGISTER)
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT12x.text);
      byte regA = parseRegister_nibble($registerA.text);
      byte regB = parseRegister_nibble($registerB.text);

      $method::methodBuilder.addInstruction(new BuilderInstruction12x(opcode, regA, regB));
    };

insn_format20bc
  : //e.g. throw-verification-error generic-error, Lsome/class;
    ^(I_STATEMENT_FORMAT20bc INSTRUCTION_FORMAT20bc verification_error_type verification_error_reference)
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT20bc.text);

      int verificationError = $verification_error_type.verificationError;
      ImmutableReference referencedItem = $verification_error_reference.reference;

      $method::methodBuilder.addInstruction(new BuilderInstruction20bc(opcode, verificationError,
              dexBuilder.internReference(referencedItem)));
    };

insn_format20t
  : //e.g. goto/16 endloop:
    ^(I_STATEMENT_FORMAT20t INSTRUCTION_FORMAT20t label_ref)
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT20t.text);
      $method::methodBuilder.addInstruction(new BuilderInstruction20t(opcode, $label_ref.label));
    };

insn_format21c_field
  : //e.g. sget_object v0, java/lang/System/out LJava/io/PrintStream;
    ^(I_STATEMENT_FORMAT21c_FIELD inst=(INSTRUCTION_FORMAT21c_FIELD | INSTRUCTION_FORMAT21c_FIELD_ODEX) REGISTER field_reference)
    {
      Opcode opcode = opcodes.getOpcodeByName($inst.text);
      short regA = parseRegister_byte($REGISTER.text);

      ImmutableFieldReference fieldReference = $field_reference.fieldReference;

      $method::methodBuilder.addInstruction(new BuilderInstruction21c(opcode, regA,
              dexBuilder.internFieldReference(fieldReference)));
    };

insn_format21c_string
  : //e.g. const-string v1, "Hello World!"
    ^(I_STATEMENT_FORMAT21c_STRING INSTRUCTION_FORMAT21c_STRING REGISTER string_literal)
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT21c_STRING.text);
      short regA = parseRegister_byte($REGISTER.text);

      $method::methodBuilder.addInstruction(new BuilderInstruction21c(opcode, regA,
              dexBuilder.internStringReference($string_literal.value)));
    };

insn_format21c_type
  : //e.g. const-class v2, org/jf/HelloWorld2/HelloWorld2
    ^(I_STATEMENT_FORMAT21c_TYPE INSTRUCTION_FORMAT21c_TYPE REGISTER nonvoid_type_descriptor)
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT21c_TYPE.text);
      short regA = parseRegister_byte($REGISTER.text);

      $method::methodBuilder.addInstruction(new BuilderInstruction21c(opcode, regA,
              dexBuilder.internTypeReference($nonvoid_type_descriptor.type)));
    };

insn_format21c_lambda
  : //e.g. capture-variable v1, "foobar"
    ^(I_STATEMENT_FORMAT21c_LAMBDA INSTRUCTION_FORMAT21c_LAMBDA REGISTER string_literal)
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT21c_LAMBDA.text);
      short regA = parseRegister_byte($REGISTER.text);

      $method::methodBuilder.addInstruction(new BuilderInstruction21c(opcode, regA,
              dexBuilder.internStringReference($string_literal.value)));
    };

insn_format21c_method
  : //e.g. create-lambda v1, java/io/PrintStream/print(Ljava/lang/Stream;)V
    ^(I_STATEMENT_FORMAT21c_METHOD INSTRUCTION_FORMAT21c_METHOD REGISTER method_reference)
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT21c_METHOD.text);
      short regA = parseRegister_byte($REGISTER.text);

      ImmutableMethodReference methodReference = $method_reference.methodReference;

      $method::methodBuilder.addInstruction(new BuilderInstruction21c(opcode, regA,
              dexBuilder.internMethodReference(methodReference)));
    };

insn_format21ih
  : //e.g. const/high16 v1, 1234
    ^(I_STATEMENT_FORMAT21ih INSTRUCTION_FORMAT21ih REGISTER fixed_32bit_literal)
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT21ih.text);
      short regA = parseRegister_byte($REGISTER.text);

      int litB = $fixed_32bit_literal.value;

      $method::methodBuilder.addInstruction(new BuilderInstruction21ih(opcode, regA, litB));
    };

insn_format21lh
  : //e.g. const-wide/high16 v1, 1234
    ^(I_STATEMENT_FORMAT21lh INSTRUCTION_FORMAT21lh REGISTER fixed_64bit_literal)
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT21lh.text);
      short regA = parseRegister_byte($REGISTER.text);

      long litB = $fixed_64bit_literal.value;

      $method::methodBuilder.addInstruction(new BuilderInstruction21lh(opcode, regA, litB));
    };

insn_format21s
  : //e.g. const/16 v1, 1234
    ^(I_STATEMENT_FORMAT21s INSTRUCTION_FORMAT21s REGISTER short_integral_literal)
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT21s.text);
      short regA = parseRegister_byte($REGISTER.text);

      short litB = $short_integral_literal.value;

      $method::methodBuilder.addInstruction(new BuilderInstruction21s(opcode, regA, litB));
    };

insn_format21t
  : //e.g. if-eqz v0, endloop:
    ^(I_STATEMENT_FORMAT21t INSTRUCTION_FORMAT21t REGISTER label_ref)
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT21t.text);
      short regA = parseRegister_byte($REGISTER.text);

      $method::methodBuilder.addInstruction(new BuilderInstruction21t(opcode, regA, $label_ref.label));
    };

insn_format22b
  : //e.g. add-int v0, v1, 123
    ^(I_STATEMENT_FORMAT22b INSTRUCTION_FORMAT22b registerA=REGISTER registerB=REGISTER short_integral_literal)
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT22b.text);
      short regA = parseRegister_byte($registerA.text);
      short regB = parseRegister_byte($registerB.text);

      short litC = $short_integral_literal.value;
      LiteralTools.checkByte(litC);

      $method::methodBuilder.addInstruction(new BuilderInstruction22b(opcode, regA, regB, litC));
    };

insn_format22c_field
  : //e.g. iput-object v1, v0, org/jf/HelloWorld2/HelloWorld2.helloWorld Ljava/lang/String;
    ^(I_STATEMENT_FORMAT22c_FIELD inst=(INSTRUCTION_FORMAT22c_FIELD | INSTRUCTION_FORMAT22c_FIELD_ODEX) registerA=REGISTER registerB=REGISTER field_reference)
    {
      Opcode opcode = opcodes.getOpcodeByName($inst.text);
      byte regA = parseRegister_nibble($registerA.text);
      byte regB = parseRegister_nibble($registerB.text);

      ImmutableFieldReference fieldReference = $field_reference.fieldReference;

      $method::methodBuilder.addInstruction(new BuilderInstruction22c(opcode, regA, regB,
              dexBuilder.internFieldReference(fieldReference)));
    };

insn_format22c_type
  : //e.g. instance-of v0, v1, Ljava/lang/String;
    ^(I_STATEMENT_FORMAT22c_TYPE INSTRUCTION_FORMAT22c_TYPE registerA=REGISTER registerB=REGISTER nonvoid_type_descriptor)
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT22c_TYPE.text);
      byte regA = parseRegister_nibble($registerA.text);
      byte regB = parseRegister_nibble($registerB.text);

      $method::methodBuilder.addInstruction(new BuilderInstruction22c(opcode, regA, regB,
              dexBuilder.internTypeReference($nonvoid_type_descriptor.type)));
    };

insn_format22c_string
  : //e.g. liberate-variable v0, v1, "baz"
    ^(I_STATEMENT_FORMAT22c_STRING INSTRUCTION_FORMAT22c_STRING registerA=REGISTER registerB=REGISTER string_literal)
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT22c_STRING.text);
      byte regA = parseRegister_nibble($registerA.text);
      byte regB = parseRegister_nibble($registerB.text);

      $method::methodBuilder.addInstruction(new BuilderInstruction22c(opcode, regA, regB,
              dexBuilder.internStringReference($string_literal.value)));
    };

insn_format22s
  : //e.g. add-int/lit16 v0, v1, 12345
    ^(I_STATEMENT_FORMAT22s INSTRUCTION_FORMAT22s registerA=REGISTER registerB=REGISTER short_integral_literal)
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT22s.text);
      byte regA = parseRegister_nibble($registerA.text);
      byte regB = parseRegister_nibble($registerB.text);

      short litC = $short_integral_literal.value;

      $method::methodBuilder.addInstruction(new BuilderInstruction22s(opcode, regA, regB, litC));
    };

insn_format22t
  : //e.g. if-eq v0, v1, endloop:
    ^(I_STATEMENT_FORMAT22t INSTRUCTION_FORMAT22t registerA=REGISTER registerB=REGISTER label_ref)
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT22t.text);
      byte regA = parseRegister_nibble($registerA.text);
      byte regB = parseRegister_nibble($registerB.text);

      $method::methodBuilder.addInstruction(new BuilderInstruction22t(opcode, regA, regB, $label_ref.label));
    };

insn_format22x
  : //e.g. move/from16 v1, v1234
    ^(I_STATEMENT_FORMAT22x INSTRUCTION_FORMAT22x registerA=REGISTER registerB=REGISTER)
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT22x.text);
      short regA = parseRegister_byte($registerA.text);
      int regB = parseRegister_short($registerB.text);

      $method::methodBuilder.addInstruction(new BuilderInstruction22x(opcode, regA, regB));
    };

insn_format23x
  : //e.g. add-int v1, v2, v3
    ^(I_STATEMENT_FORMAT23x INSTRUCTION_FORMAT23x registerA=REGISTER registerB=REGISTER registerC=REGISTER)
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT23x.text);
      short regA = parseRegister_byte($registerA.text);
      short regB = parseRegister_byte($registerB.text);
      short regC = parseRegister_byte($registerC.text);

      $method::methodBuilder.addInstruction(new BuilderInstruction23x(opcode, regA, regB, regC));
    };

insn_format25x
  : //e.g. invoke-lambda vClosure, {vD, vE, vF, vG} -- up to 4 parameters + the closure.
    ^(I_STATEMENT_FORMAT25x INSTRUCTION_FORMAT25x REGISTER register_list4)
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT25x.text);

      byte closureRegister = parseRegister_nibble($REGISTER.text);

      //this depends on the fact that register_list4 returns a byte[4]
      byte[] registers = $register_list4.registers;
      int parameterRegisterCount = $register_list4.registerCount;  // don't count closure register

      $method::methodBuilder.addInstruction(new BuilderInstruction25x(opcode,
            parameterRegisterCount, closureRegister, registers[0], registers[1],
            registers[2], registers[3]));
    };

insn_format30t
  : //e.g. goto/32 endloop:
    ^(I_STATEMENT_FORMAT30t INSTRUCTION_FORMAT30t label_ref)
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT30t.text);

      $method::methodBuilder.addInstruction(new BuilderInstruction30t(opcode, $label_ref.label));
    };

insn_format31c
  : //e.g. const-string/jumbo v1 "Hello World!"
    ^(I_STATEMENT_FORMAT31c INSTRUCTION_FORMAT31c REGISTER string_literal)
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT31c.text);
      short regA = parseRegister_byte($REGISTER.text);

      $method::methodBuilder.addInstruction(new BuilderInstruction31c(opcode, regA,
              dexBuilder.internStringReference($string_literal.value)));
    };

insn_format31i
  : //e.g. const v0, 123456
    ^(I_STATEMENT_FORMAT31i INSTRUCTION_FORMAT31i REGISTER fixed_32bit_literal)
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT31i.text);
      short regA = parseRegister_byte($REGISTER.text);

      int litB = $fixed_32bit_literal.value;

      $method::methodBuilder.addInstruction(new BuilderInstruction31i(opcode, regA, litB));
    };

insn_format31t
  : //e.g. fill-array-data v0, ArrayData:
    ^(I_STATEMENT_FORMAT31t INSTRUCTION_FORMAT31t REGISTER label_ref)
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT31t.text);

      short regA = parseRegister_byte($REGISTER.text);

      $method::methodBuilder.addInstruction(new BuilderInstruction31t(opcode, regA, $label_ref.label));
    };

insn_format32x
  : //e.g. move/16 v5678, v1234
    ^(I_STATEMENT_FORMAT32x INSTRUCTION_FORMAT32x registerA=REGISTER registerB=REGISTER)
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT32x.text);
      int regA = parseRegister_short($registerA.text);
      int regB = parseRegister_short($registerB.text);

      $method::methodBuilder.addInstruction(new BuilderInstruction32x(opcode, regA, regB));
    };

insn_format35c_method
  : //e.g. invoke-virtual {v0,v1} java/io/PrintStream/print(Ljava/lang/Stream;)V
    ^(I_STATEMENT_FORMAT35c_METHOD INSTRUCTION_FORMAT35c_METHOD register_list method_reference)
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT35c_METHOD.text);

      //this depends on the fact that register_list returns a byte[5]
      byte[] registers = $register_list.registers;
      byte registerCount = $register_list.registerCount;

      ImmutableMethodReference methodReference = $method_reference.methodReference;

      $method::methodBuilder.addInstruction(new BuilderInstruction35c(opcode, registerCount, registers[0], registers[1],
              registers[2], registers[3], registers[4], dexBuilder.internMethodReference(methodReference)));
    };

insn_format35c_type
  : //e.g. filled-new-array {v0,v1}, I
    ^(I_STATEMENT_FORMAT35c_TYPE INSTRUCTION_FORMAT35c_TYPE register_list nonvoid_type_descriptor)
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT35c_TYPE.text);

      //this depends on the fact that register_list returns a byte[5]
      byte[] registers = $register_list.registers;
      byte registerCount = $register_list.registerCount;

      $method::methodBuilder.addInstruction(new BuilderInstruction35c(opcode, registerCount, registers[0], registers[1],
              registers[2], registers[3], registers[4], dexBuilder.internTypeReference($nonvoid_type_descriptor.type)));
    };

insn_format3rc_method
  : //e.g. invoke-virtual/range {v25..v26} java/lang/StringBuilder/append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    ^(I_STATEMENT_FORMAT3rc_METHOD INSTRUCTION_FORMAT3rc_METHOD register_range method_reference)
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT3rc_METHOD.text);
      int startRegister = $register_range.startRegister;
      int endRegister = $register_range.endRegister;

      int registerCount = endRegister-startRegister+1;

      ImmutableMethodReference methodReference = $method_reference.methodReference;

      $method::methodBuilder.addInstruction(new BuilderInstruction3rc(opcode, startRegister, registerCount,
              dexBuilder.internMethodReference(methodReference)));
    };

insn_format3rc_type
  : //e.g. filled-new-array/range {v0..v6} I
    ^(I_STATEMENT_FORMAT3rc_TYPE INSTRUCTION_FORMAT3rc_TYPE register_range nonvoid_type_descriptor)
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT3rc_TYPE.text);
      int startRegister = $register_range.startRegister;
      int endRegister = $register_range.endRegister;

      int registerCount = endRegister-startRegister+1;

      $method::methodBuilder.addInstruction(new BuilderInstruction3rc(opcode, startRegister, registerCount,
              dexBuilder.internTypeReference($nonvoid_type_descriptor.type)));
    };

insn_format51l_type
  : //e.g. const-wide v0, 5000000000L
    ^(I_STATEMENT_FORMAT51l INSTRUCTION_FORMAT51l REGISTER fixed_64bit_literal)
    {
      Opcode opcode = opcodes.getOpcodeByName($INSTRUCTION_FORMAT51l.text);
      short regA = parseRegister_byte($REGISTER.text);

      long litB = $fixed_64bit_literal.value;

      $method::methodBuilder.addInstruction(new BuilderInstruction51l(opcode, regA, litB));
    };

insn_array_data_directive
  : //e.g. .array-data 4 1000000 .end array-data
    ^(I_STATEMENT_ARRAY_DATA ^(I_ARRAY_ELEMENT_SIZE short_integral_literal) array_elements)
    {
      int elementWidth = $short_integral_literal.value;
      List<Number> elements = $array_elements.elements;

      $method::methodBuilder.addInstruction(new BuilderArrayPayload(elementWidth, $array_elements.elements));
    };

insn_packed_switch_directive
  :
    ^(I_STATEMENT_PACKED_SWITCH ^(I_PACKED_SWITCH_START_KEY fixed_32bit_literal) packed_switch_elements)
      {
        int startKey = $fixed_32bit_literal.value;
        $method::methodBuilder.addInstruction(new BuilderPackedSwitchPayload(startKey,
            $packed_switch_elements.elements));
      };

insn_sparse_switch_directive
  :
    ^(I_STATEMENT_SPARSE_SWITCH sparse_switch_elements)
    {
      $method::methodBuilder.addInstruction(new BuilderSparseSwitchPayload($sparse_switch_elements.elements));
    };

nonvoid_type_descriptor returns [String type]
  : (PRIMITIVE_TYPE
  | CLASS_DESCRIPTOR
  | ARRAY_DESCRIPTOR)
  {
    $type = $start.getText();
  };

reference_type_descriptor returns [String type]
  : (CLASS_DESCRIPTOR
  | ARRAY_DESCRIPTOR)
  {
    $type = $start.getText();
  };

type_descriptor returns [String type]
  : VOID_TYPE {$type = "V";}
  | nonvoid_type_descriptor {$type = $nonvoid_type_descriptor.type;}
  ;

short_integral_literal returns[short value]
  : long_literal
    {
      LiteralTools.checkShort($long_literal.value);
      $value = (short)$long_literal.value;
    }
  | integer_literal
    {
      LiteralTools.checkShort($integer_literal.value);
      $value = (short)$integer_literal.value;
    }
  | short_literal {$value = $short_literal.value;}
  | char_literal {$value = (short)$char_literal.value;}
  | byte_literal {$value = $byte_literal.value;};

integral_literal returns[int value]
  : long_literal
    {
      LiteralTools.checkInt($long_literal.value);
      $value = (int)$long_literal.value;
    }
  | integer_literal {$value = $integer_literal.value;}
  | short_literal {$value = $short_literal.value;}
  | byte_literal {$value = $byte_literal.value;};


integer_literal returns[int value]
  : INTEGER_LITERAL { $value = LiteralTools.parseInt($INTEGER_LITERAL.text); };

long_literal returns[long value]
  : LONG_LITERAL { $value = LiteralTools.parseLong($LONG_LITERAL.text); };

short_literal returns[short value]
  : SHORT_LITERAL { $value = LiteralTools.parseShort($SHORT_LITERAL.text); };

byte_literal returns[byte value]
  : BYTE_LITERAL { $value = LiteralTools.parseByte($BYTE_LITERAL.text); };

float_literal returns[float value]
  : FLOAT_LITERAL { $value = LiteralTools.parseFloat($FLOAT_LITERAL.text); };

double_literal returns[double value]
  : DOUBLE_LITERAL { $value = LiteralTools.parseDouble($DOUBLE_LITERAL.text); };

char_literal returns[char value]
  : CHAR_LITERAL { $value = $CHAR_LITERAL.text.charAt(1); };

string_literal returns[String value]
  : STRING_LITERAL
    {
      $value = $STRING_LITERAL.text;
      $value = $value.substring(1,$value.length()-1);
    };

bool_literal returns[boolean value]
  : BOOL_LITERAL { $value = Boolean.parseBoolean($BOOL_LITERAL.text); };

array_literal returns[List<EncodedValue> elements]
  : {$elements = Lists.newArrayList();}
    ^(I_ENCODED_ARRAY (literal {$elements.add($literal.encodedValue);})*);

annotations returns[Set<Annotation> annotations]
  : {HashMap<String, Annotation> annotationMap = Maps.newHashMap();}
    ^(I_ANNOTATIONS (annotation
    {
        Annotation anno = $annotation.annotation;
        Annotation old = annotationMap.put(anno.getType(), anno);
        if (old != null) {
            throw new SemanticException(input, "Multiple annotations of type \%s", anno.getType());
        }
    })*)
    {
      if (annotationMap.size() > 0) {
        $annotations = ImmutableSet.copyOf(annotationMap.values());
      }
    };

annotation returns[Annotation annotation]
  : ^(I_ANNOTATION ANNOTATION_VISIBILITY subannotation)
    {
      int visibility = AnnotationVisibility.getVisibility($ANNOTATION_VISIBILITY.text);
      $annotation = new ImmutableAnnotation(visibility, $subannotation.annotationType, $subannotation.elements);
    };

annotation_element returns[AnnotationElement element]
  : ^(I_ANNOTATION_ELEMENT SIMPLE_NAME literal)
    {
      $element = new ImmutableAnnotationElement($SIMPLE_NAME.text, $literal.encodedValue);
    };

subannotation returns[String annotationType, List<AnnotationElement> elements]
  : {ArrayList<AnnotationElement> elements = Lists.newArrayList();}
    ^(I_SUBANNOTATION
        CLASS_DESCRIPTOR
        (annotation_element
        {
           elements.add($annotation_element.element);
        })*
     )
    {
      $annotationType = $CLASS_DESCRIPTOR.text;
      $elements = elements;
    };

field_literal returns[FieldReference value]
  : ^(I_ENCODED_FIELD field_reference)
    {
      $value = $field_reference.fieldReference;
    };

method_literal returns[MethodReference value]
  : ^(I_ENCODED_METHOD method_reference)
    {
      $value = $method_reference.methodReference;
    };

enum_literal returns[FieldReference value]
  : ^(I_ENCODED_ENUM field_reference)
    {
      $value = $field_reference.fieldReference;
    };
