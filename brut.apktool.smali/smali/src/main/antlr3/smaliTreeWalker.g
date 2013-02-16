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

import com.google.common.collect.ImmutableSortedMap;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.regex.*;
import java.lang.Float;
import java.lang.Double;

import org.jf.dexlib.*;
import org.jf.dexlib.EncodedValue.*;
import org.jf.dexlib.Util.*;
import org.jf.dexlib.Code.*;
import org.jf.dexlib.Code.Format.*;
}

@members {
  public DexFile dexFile;
  public TypeIdItem classType;
  private boolean verboseErrors = false;

  public void setVerboseErrors(boolean verboseErrors) {
    this.verboseErrors = verboseErrors;
  }

  private byte parseRegister_nibble(String register, int totalMethodRegisters, int methodParameterRegisters)
    throws SemanticException {
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
  private short parseRegister_byte(String register, int totalMethodRegisters, int methodParameterRegisters)
    throws SemanticException {
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
  private int parseRegister_short(String register, int totalMethodRegisters, int methodParameterRegisters)
    throws SemanticException {
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



smali_file
  : ^(I_CLASS_DEF header methods fields annotations)
  {
    AnnotationDirectoryItem annotationDirectoryItem = null;
    ClassDefItem classDefItem = null;
    ClassDataItem classDataItem = null;

    if ($methods.methodAnnotations != null ||
        $methods.parameterAnnotations != null ||
        $fields.fieldAnnotations != null ||
        $annotations.annotationSetItem != null) {
        annotationDirectoryItem = AnnotationDirectoryItem.internAnnotationDirectoryItem(
          dexFile,
          $annotations.annotationSetItem,
          $fields.fieldAnnotations,
          $methods.methodAnnotations,
          $methods.parameterAnnotations);
    }

    if ($fields.staticFields.size() != 0 || $fields.instanceFields.size() != 0 ||
        $methods.directMethods.size() != 0 || $methods.virtualMethods.size()!= 0) {
      classDataItem = ClassDataItem.internClassDataItem(dexFile, $fields.staticFields, $fields.instanceFields,
                  $methods.directMethods, $methods.virtualMethods);
    }

    classDefItem = ClassDefItem.internClassDefItem(dexFile, $header.classType, $header.accessFlags,
        $header.superType, $header.implementsList, $header.sourceSpec, annotationDirectoryItem,
        classDataItem, $fields.staticFieldInitialValues);
  };
  catch [Exception ex] {
    if (verboseErrors) {
      ex.printStackTrace(System.err);
    }
    reportError(new SemanticException(input, ex));
  }


header returns[TypeIdItem classType, int accessFlags, TypeIdItem superType, TypeListItem implementsList, StringIdItem sourceSpec]
: class_spec super_spec? implements_list source_spec
  {
    classType = $class_spec.type;
    $classType = classType;
    $accessFlags = $class_spec.accessFlags;
    $superType = $super_spec.type;
    $implementsList = $implements_list.implementsList;
    $sourceSpec = $source_spec.source;
  };


class_spec returns[TypeIdItem type, int accessFlags]
  : class_type_descriptor access_list
  {
    $type = $class_type_descriptor.type;
    $accessFlags = $access_list.value;
  };

super_spec returns[TypeIdItem type]
  : ^(I_SUPER class_type_descriptor)
  {
    $type = $class_type_descriptor.type;
  };


implements_spec returns[TypeIdItem type]
  : ^(I_IMPLEMENTS class_type_descriptor)
  {
    $type = $class_type_descriptor.type;
  };

implements_list returns[TypeListItem implementsList]
@init { List<TypeIdItem> typeList; }
  : {typeList = new LinkedList<TypeIdItem>();}
    (implements_spec {typeList.add($implements_spec.type);} )*
  {
    if (typeList.size() > 0) {
      $implementsList = TypeListItem.internTypeListItem(dexFile, typeList);
    } else {
      $implementsList = null;
    }
  };

source_spec returns[StringIdItem source]
  : {$source = null;}
    ^(I_SOURCE string_literal {$source = StringIdItem.internStringIdItem(dexFile, $string_literal.value);})
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


fields returns[List<ClassDataItem.EncodedField> staticFields, List<ClassDataItem.EncodedField> instanceFields,
         List<ClassDefItem.StaticFieldInitializer> staticFieldInitialValues, List<AnnotationDirectoryItem.FieldAnnotation> fieldAnnotations]
  @init
  {
    $staticFields = new LinkedList<ClassDataItem.EncodedField>();
    $instanceFields = new LinkedList<ClassDataItem.EncodedField>();
    $staticFieldInitialValues = new ArrayList<ClassDefItem.StaticFieldInitializer>();
  }
  : ^(I_FIELDS
      (field
      {
        if ($field.encodedField.isStatic()) {
          $staticFields.add($field.encodedField);
          $staticFieldInitialValues.add(new ClassDefItem.StaticFieldInitializer(
            $field.encodedValue, $field.encodedField));
        } else {
          $instanceFields.add($field.encodedField);
        }
        if ($field.fieldAnnotationSet != null) {
          if ($fieldAnnotations == null) {
            $fieldAnnotations = new LinkedList<AnnotationDirectoryItem.FieldAnnotation>();
          }
          AnnotationDirectoryItem.FieldAnnotation fieldAnnotation = new AnnotationDirectoryItem.FieldAnnotation(
            $field.encodedField.field, $field.fieldAnnotationSet);
          $fieldAnnotations.add(fieldAnnotation);
        }
      })*);

methods returns[List<ClassDataItem.EncodedMethod> directMethods,
    List<ClassDataItem.EncodedMethod> virtualMethods,
    List<AnnotationDirectoryItem.MethodAnnotation> methodAnnotations,
    List<AnnotationDirectoryItem.ParameterAnnotation> parameterAnnotations]
  @init
  {
    $directMethods = new LinkedList<ClassDataItem.EncodedMethod>();
    $virtualMethods = new LinkedList<ClassDataItem.EncodedMethod>();
  }
  : ^(I_METHODS
      (method
      {
        if ($method.encodedMethod.isDirect()) {
          $directMethods.add($method.encodedMethod);
        } else {
          $virtualMethods.add($method.encodedMethod);
        }
        if ($method.methodAnnotationSet != null) {
          if ($methodAnnotations == null) {
            $methodAnnotations = new LinkedList<AnnotationDirectoryItem.MethodAnnotation>();
          }
          AnnotationDirectoryItem.MethodAnnotation methodAnnotation =
            new AnnotationDirectoryItem.MethodAnnotation($method.encodedMethod.method, $method.methodAnnotationSet);
          $methodAnnotations.add(methodAnnotation);
        }
        if ($method.parameterAnnotationSets != null) {
          if ($parameterAnnotations == null) {
            $parameterAnnotations = new LinkedList<AnnotationDirectoryItem.ParameterAnnotation>();
          }
          AnnotationDirectoryItem.ParameterAnnotation parameterAnnotation =
            new AnnotationDirectoryItem.ParameterAnnotation($method.encodedMethod.method,
              $method.parameterAnnotationSets);
          $parameterAnnotations.add(parameterAnnotation);
        }
      })*);

field returns [ClassDataItem.EncodedField encodedField, EncodedValue encodedValue, AnnotationSetItem fieldAnnotationSet]
  :^(I_FIELD SIMPLE_NAME access_list ^(I_FIELD_TYPE nonvoid_type_descriptor) field_initial_value annotations?)
  {
    StringIdItem memberName = StringIdItem.internStringIdItem(dexFile, $SIMPLE_NAME.text);
    TypeIdItem fieldType = $nonvoid_type_descriptor.type;

    FieldIdItem fieldIdItem = FieldIdItem.internFieldIdItem(dexFile, classType, fieldType, memberName);
    $encodedField = new ClassDataItem.EncodedField(fieldIdItem, $access_list.value);

    if ($field_initial_value.encodedValue != null) {
      if (!$encodedField.isStatic()) {
        throw new SemanticException(input, "Initial field values can only be specified for static fields.");
      }

      $encodedValue = $field_initial_value.encodedValue;
    } else {
      $encodedValue = null;
    }

    if ($annotations.annotationSetItem != null) {
      $fieldAnnotationSet = $annotations.annotationSetItem;
    }
  };


field_initial_value returns[EncodedValue encodedValue]
  : ^(I_FIELD_INITIAL_VALUE literal) {$encodedValue = $literal.encodedValue;}
  | /*epsilon*/;

literal returns[EncodedValue encodedValue]
  : integer_literal { $encodedValue = new IntEncodedValue($integer_literal.value); }
  | long_literal { $encodedValue = new LongEncodedValue($long_literal.value); }
  | short_literal { $encodedValue = new ShortEncodedValue($short_literal.value); }
  | byte_literal { $encodedValue = new ByteEncodedValue($byte_literal.value); }
  | float_literal { $encodedValue = new FloatEncodedValue($float_literal.value); }
  | double_literal { $encodedValue = new DoubleEncodedValue($double_literal.value); }
  | char_literal { $encodedValue = new CharEncodedValue($char_literal.value); }
  | string_literal { $encodedValue = new StringEncodedValue(StringIdItem.internStringIdItem(dexFile, $string_literal.value)); }
  | bool_literal { $encodedValue = $bool_literal.value?BooleanEncodedValue.TrueValue:BooleanEncodedValue.FalseValue; }
  | NULL_LITERAL { $encodedValue = NullEncodedValue.NullValue; }
  | type_descriptor { $encodedValue = new TypeEncodedValue($type_descriptor.type); }
  | array_literal { $encodedValue = new ArrayEncodedValue($array_literal.values); }
  | subannotation { $encodedValue = new AnnotationEncodedValue($subannotation.annotationType, $subannotation.elementNames, $subannotation.elementValues); }
  | field_literal { $encodedValue = new FieldEncodedValue($field_literal.value); }
  | method_literal { $encodedValue = new MethodEncodedValue($method_literal.value); }
  | enum_literal { $encodedValue = new EnumEncodedValue($enum_literal.value); };


//everything but string
fixed_size_literal returns[byte[\] value]
  : integer_literal { $value = LiteralTools.intToBytes($integer_literal.value); }
  | long_literal { $value = LiteralTools.longToBytes($long_literal.value); }
  | short_literal { $value = LiteralTools.shortToBytes($short_literal.value); }
  | byte_literal { $value = new byte[] { $byte_literal.value }; }
  | float_literal { $value = LiteralTools.floatToBytes($float_literal.value); }
  | double_literal { $value = LiteralTools.doubleToBytes($double_literal.value); }
  | char_literal { $value = LiteralTools.charToBytes($char_literal.value); }
  | bool_literal { $value = LiteralTools.boolToBytes($bool_literal.value); };

//everything but string
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

array_elements returns[List<byte[\]> values]
  : {$values = new ArrayList<byte[]>();}
    ^(I_ARRAY_ELEMENTS
      (fixed_size_literal
      {
        $values.add($fixed_size_literal.value);
      })*);

packed_switch_target_count returns[int targetCount]
  : I_PACKED_SWITCH_TARGET_COUNT {$targetCount = Integer.parseInt($I_PACKED_SWITCH_TARGET_COUNT.text);};

packed_switch_targets[int baseAddress] returns[int[\] targets]
  :
    ^(I_PACKED_SWITCH_TARGETS
      packed_switch_target_count
      {
        int targetCount = $packed_switch_target_count.targetCount;
        $targets = new int[targetCount];
        int targetsPosition = 0;
      }

      (offset_or_label
      {
        $targets[targetsPosition++] = ($method::currentAddress + $offset_or_label.offsetValue) - $baseAddress;
      })*
    );

sparse_switch_target_count returns[int targetCount]
  : I_SPARSE_SWITCH_TARGET_COUNT {$targetCount = Integer.parseInt($I_SPARSE_SWITCH_TARGET_COUNT.text);};

sparse_switch_keys[int targetCount] returns[int[\] keys]
  : {
      $keys = new int[$targetCount];
      int keysPosition = 0;
    }
    ^(I_SPARSE_SWITCH_KEYS
      (fixed_32bit_literal
      {
        $keys[keysPosition++] = $fixed_32bit_literal.value;
      })*
    );


sparse_switch_targets[int baseAddress, int targetCount] returns[int[\] targets]
  : {
      $targets = new int[$targetCount];
      int targetsPosition = 0;
    }
    ^(I_SPARSE_SWITCH_TARGETS
      (offset_or_label
      {
        $targets[targetsPosition++] = ($method::currentAddress + $offset_or_label.offsetValue) - $baseAddress;
      })*
    );

method returns[ClassDataItem.EncodedMethod encodedMethod,
    AnnotationSetItem methodAnnotationSet,
    AnnotationSetRefList parameterAnnotationSets]
  scope
  {
    HashMap<String, Integer> labels;
    TryListBuilder tryList;
    int currentAddress;
    DebugInfoBuilder debugInfo;
    HashMap<Integer, Integer> packedSwitchDeclarations;
    HashMap<Integer, Integer> sparseSwitchDeclarations;
  }
  @init
  {
    MethodIdItem methodIdItem = null;
    int totalMethodRegisters = 0;
    int methodParameterRegisters = 0;
    int accessFlags = 0;
    boolean isStatic = false;
  }
  : {
      $method::labels = new HashMap<String, Integer>();
      $method::tryList = new TryListBuilder();
      $method::currentAddress = 0;
      $method::debugInfo = new DebugInfoBuilder();
      $method::packedSwitchDeclarations = new HashMap<Integer, Integer>();
      $method::sparseSwitchDeclarations = new HashMap<Integer, Integer>();
    }
    ^(I_METHOD
      method_name_and_prototype
      access_list
      {
        methodIdItem = $method_name_and_prototype.methodIdItem;
        accessFlags = $access_list.value;
        isStatic = (accessFlags & AccessFlags.STATIC.getValue()) != 0;
        methodParameterRegisters = methodIdItem.getPrototype().getParameterRegisterCount();
        if (!isStatic) {
          methodParameterRegisters++;
        }
      }
      (registers_directive
       {
         if ($registers_directive.isLocalsDirective) {
           totalMethodRegisters = $registers_directive.registers + methodParameterRegisters;
         } else {
           totalMethodRegisters = $registers_directive.registers;
         }
       }
      )?
      labels
      packed_switch_declarations
      sparse_switch_declarations
      statements[totalMethodRegisters, methodParameterRegisters]
      catches
      parameters
      ordered_debug_directives[totalMethodRegisters, methodParameterRegisters]
      annotations
    )
  {
    Pair<List<CodeItem.TryItem>, List<CodeItem.EncodedCatchHandler>> temp = $method::tryList.encodeTries();
    List<CodeItem.TryItem> tries = temp.first;
    List<CodeItem.EncodedCatchHandler> handlers = temp.second;

    DebugInfoItem debugInfoItem = $method::debugInfo.encodeDebugInfo(dexFile);

    CodeItem codeItem;

    boolean isAbstract = false;
    boolean isNative = false;

    if ((accessFlags & AccessFlags.ABSTRACT.getValue()) != 0) {
      isAbstract = true;
    } else if ((accessFlags & AccessFlags.NATIVE.getValue()) != 0) {
      isNative = true;
    }

    if ($statements.instructions.size() == 0) {
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
              throw new SemanticException(input, $registers_directive.start, "A  .registers directive is not valid in \%s method", methodType);
            }
          }

          if ($method::labels.size() > 0) {
            throw new SemanticException(input, $I_METHOD, "Labels cannot be present in \%s method", methodType);
          }

          if ((tries != null && tries.size() > 0) || (handlers != null && handlers.size() > 0)) {
            throw new SemanticException(input, $I_METHOD, "try/catch blocks cannot be present in \%s method", methodType);
          }

          if (debugInfoItem != null) {
            throw new SemanticException(input, $I_METHOD, "debug directives cannot be present in \%s method", methodType);
          }

          codeItem = null;
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

      if (totalMethodRegisters < methodParameterRegisters) {
        throw new SemanticException(input, $registers_directive.start, "This method requires at least " +
                Integer.toString(methodParameterRegisters) +
                " registers, for the method parameters");
      }

      int methodParameterCount = methodIdItem.getPrototype().getParameterRegisterCount();
      if ($method::debugInfo.getParameterNameCount() > methodParameterCount) {
        throw new SemanticException(input, $I_METHOD, "Too many parameter names specified. This method only has " +
                Integer.toString(methodParameterCount) +
                " parameters.");
      }

      codeItem = CodeItem.internCodeItem(dexFile,
            totalMethodRegisters,
            methodParameterRegisters,
            $statements.maxOutRegisters,
            debugInfoItem,
            $statements.instructions,
            tries,
            handlers);
    }

    $encodedMethod = new ClassDataItem.EncodedMethod(methodIdItem, accessFlags, codeItem);

    if ($annotations.annotationSetItem != null) {
      $methodAnnotationSet = $annotations.annotationSetItem;
    }

    if ($parameters.parameterAnnotations != null) {
      $parameterAnnotationSets = $parameters.parameterAnnotations;
    }
  };

method_prototype returns[ProtoIdItem protoIdItem]
  : ^(I_METHOD_PROTOTYPE ^(I_METHOD_RETURN_TYPE type_descriptor) field_type_list)
  {
    TypeIdItem returnType = $type_descriptor.type;
    List<TypeIdItem> parameterTypes = $field_type_list.types;
    TypeListItem parameterTypeListItem = null;
    if (parameterTypes != null && parameterTypes.size() > 0) {
      parameterTypeListItem = TypeListItem.internTypeListItem(dexFile, parameterTypes);
    }

    $protoIdItem = ProtoIdItem.internProtoIdItem(dexFile, returnType, parameterTypeListItem);
  };

method_name_and_prototype returns[MethodIdItem methodIdItem]
  : SIMPLE_NAME method_prototype
  {
    String methodNameString = $SIMPLE_NAME.text;
    StringIdItem methodName = StringIdItem.internStringIdItem(dexFile, methodNameString);
    ProtoIdItem protoIdItem = $method_prototype.protoIdItem;

    $methodIdItem = MethodIdItem.internMethodIdItem(dexFile, classType, protoIdItem, methodName);
  };

field_type_list returns[List<TypeIdItem> types]
  @init
  {
    $types = new LinkedList<TypeIdItem>();
  }
  : (
      nonvoid_type_descriptor
      {
        $types.add($nonvoid_type_descriptor.type);
      }
    )*;


fully_qualified_method returns[MethodIdItem methodIdItem]
  : reference_type_descriptor SIMPLE_NAME method_prototype
  {
    TypeIdItem classType = $reference_type_descriptor.type;
    StringIdItem methodName = StringIdItem.internStringIdItem(dexFile, $SIMPLE_NAME.text);
    ProtoIdItem prototype = $method_prototype.protoIdItem;
    $methodIdItem = MethodIdItem.internMethodIdItem(dexFile, classType, prototype, methodName);
  };

fully_qualified_field returns[FieldIdItem fieldIdItem]
  : reference_type_descriptor SIMPLE_NAME nonvoid_type_descriptor
  {
    TypeIdItem classType = $reference_type_descriptor.type;
    StringIdItem fieldName = StringIdItem.internStringIdItem(dexFile, $SIMPLE_NAME.text);
    TypeIdItem fieldType = $nonvoid_type_descriptor.type;
    $fieldIdItem = FieldIdItem.internFieldIdItem(dexFile, classType, fieldType, fieldName);
  };

registers_directive returns[boolean isLocalsDirective, int registers]
  : {$registers = 0;}
    ^(( I_REGISTERS {$isLocalsDirective = false;}
      | I_LOCALS {$isLocalsDirective = true;}
      )
      short_integral_literal {$registers = $short_integral_literal.value;}
     );

labels
  : ^(I_LABELS label_def*);

label_def
  : ^(I_LABEL SIMPLE_NAME address)
    {
      if ($method::labels.containsKey($SIMPLE_NAME.text)) {
        throw new SemanticException(input, $I_LABEL, "Label " + $SIMPLE_NAME.text + " has multiple defintions.");
      }

      $method::labels.put($SIMPLE_NAME.text, $address.address);
    };

packed_switch_declarations
  : ^(I_PACKED_SWITCH_DECLARATIONS packed_switch_declaration*);
packed_switch_declaration
  : ^(I_PACKED_SWITCH_DECLARATION address offset_or_label_absolute[$address.address])
    {
      int switchDataAddress = $offset_or_label_absolute.address;
      if ((switchDataAddress \% 2) != 0) {
        switchDataAddress++;
      }
      if (!$method::packedSwitchDeclarations.containsKey(switchDataAddress)) {
        $method::packedSwitchDeclarations.put(switchDataAddress, $address.address);
      }
    };

sparse_switch_declarations
  : ^(I_SPARSE_SWITCH_DECLARATIONS sparse_switch_declaration*);
sparse_switch_declaration
  : ^(I_SPARSE_SWITCH_DECLARATION address offset_or_label_absolute[$address.address])
    {
      int switchDataAddress = $offset_or_label_absolute.address;
      if ((switchDataAddress \% 2) != 0) {
        switchDataAddress++;
      }
      if (!$method::sparseSwitchDeclarations.containsKey(switchDataAddress)) {
        $method::sparseSwitchDeclarations.put(switchDataAddress, $address.address);
      }

    };

catches : ^(I_CATCHES catch_directive* catchall_directive*);

catch_directive
  : ^(I_CATCH address nonvoid_type_descriptor from=offset_or_label_absolute[$address.address] to=offset_or_label_absolute[$address.address]
        using=offset_or_label_absolute[$address.address])
    {
      TypeIdItem type = $nonvoid_type_descriptor.type;
      int startAddress = $from.address;
      int endAddress = $to.address;
      int handlerAddress = $using.address;

      $method::tryList.addHandler(type, startAddress, endAddress, handlerAddress);
    };

catchall_directive
  : ^(I_CATCHALL address from=offset_or_label_absolute[$address.address] to=offset_or_label_absolute[$address.address]
        using=offset_or_label_absolute[$address.address])
    {
      int startAddress = $from.address;
      int endAddress = $to.address;
      int handlerAddress = $using.address;

      $method::tryList.addCatchAllHandler(startAddress, endAddress, handlerAddress);
    };

address returns[int address]
  : I_ADDRESS
    {
      $address = Integer.parseInt($I_ADDRESS.text);
    };

parameters returns[AnnotationSetRefList parameterAnnotations]
  @init
  {
    int parameterCount = 0;
    List<AnnotationSetItem> annotationSetItems = new ArrayList<AnnotationSetItem>();
  }
  : ^(I_PARAMETERS (parameter
        {
          if ($parameter.parameterAnnotationSet != null) {
            while (annotationSetItems.size() < parameterCount) {
              annotationSetItems.add(AnnotationSetItem.internAnnotationSetItem(dexFile, null));
            }
            annotationSetItems.add($parameter.parameterAnnotationSet);
          }

          parameterCount++;
        })*
    )
    {
      if (annotationSetItems.size() > 0) {
        while (annotationSetItems.size() < parameterCount) {
          annotationSetItems.add(AnnotationSetItem.internAnnotationSetItem(dexFile, null));
        }
        $parameterAnnotations = AnnotationSetRefList.internAnnotationSetRefList(dexFile, annotationSetItems);
      }
    };

parameter returns[AnnotationSetItem parameterAnnotationSet]
  : ^(I_PARAMETER (string_literal {$method::debugInfo.addParameterName($string_literal.value);}
                  | {$method::debugInfo.addParameterName(null);}
                  )
        annotations {$parameterAnnotationSet = $annotations.annotationSetItem;}
    );

ordered_debug_directives[int totalMethodRegisters, int methodParameterRegisters]
  : ^(I_ORDERED_DEBUG_DIRECTIVES
       ( line
       | local[$totalMethodRegisters, $methodParameterRegisters]
       | end_local[$totalMethodRegisters, $methodParameterRegisters]
       | restart_local[$totalMethodRegisters, $methodParameterRegisters]
       | prologue
       | epilogue
       | source
       )*
     );

line
  : ^(I_LINE integral_literal address)
    {
      $method::debugInfo.addLine($address.address, $integral_literal.value);
    };

local[int totalMethodRegisters, int methodParameterRegisters]
  : ^(I_LOCAL REGISTER SIMPLE_NAME nonvoid_type_descriptor string_literal? address)
    {
      int registerNumber = parseRegister_short($REGISTER.text, $totalMethodRegisters, $methodParameterRegisters);

      if ($string_literal.value != null) {
        $method::debugInfo.addLocalExtended($address.address, registerNumber, $SIMPLE_NAME.text, $nonvoid_type_descriptor.type.getTypeDescriptor(), $string_literal.value);
      } else {
        $method::debugInfo.addLocal($address.address, registerNumber, $SIMPLE_NAME.text, $nonvoid_type_descriptor.type.getTypeDescriptor());
      }
    };

end_local[int totalMethodRegisters, int methodParameterRegisters]
  : ^(I_END_LOCAL REGISTER address)
    {
      int registerNumber = parseRegister_short($REGISTER.text, $totalMethodRegisters, $methodParameterRegisters);

      $method::debugInfo.addEndLocal($address.address, registerNumber);
    };

restart_local[int totalMethodRegisters, int methodParameterRegisters]
  : ^(I_RESTART_LOCAL REGISTER address)
    {
      int registerNumber = parseRegister_short($REGISTER.text, $totalMethodRegisters, $methodParameterRegisters);

      $method::debugInfo.addRestartLocal($address.address, registerNumber);
    };

prologue
  : ^(I_PROLOGUE address)
    {
      $method::debugInfo.addPrologue($address.address);
    };

epilogue
  : ^(I_EPILOGUE address)
    {
      $method::debugInfo.addEpilogue($address.address);
    };

source
  : ^(I_SOURCE string_literal address)
    {
      $method::debugInfo.addSetFile($address.address, $string_literal.value);
    };

statements[int totalMethodRegisters, int methodParameterRegisters] returns[List<Instruction> instructions, int maxOutRegisters]
  @init
  {
    $instructions = new LinkedList<Instruction>();
    $maxOutRegisters = 0;
  }
  : ^(I_STATEMENTS (instruction[$totalMethodRegisters, $methodParameterRegisters, $instructions]
        {
          $method::currentAddress += $instructions.get($instructions.size() - 1).getSize($method::currentAddress);
          if ($maxOutRegisters < $instruction.outRegisters) {
            $maxOutRegisters = $instruction.outRegisters;
          }
        })*);

label_ref returns[int labelAddress]
  : SIMPLE_NAME
    {
      Integer labelAdd = $method::labels.get($SIMPLE_NAME.text);

      if (labelAdd == null) {
        throw new SemanticException(input, $SIMPLE_NAME, "Label \"" + $SIMPLE_NAME.text + "\" is not defined.");
      }

      $labelAddress = labelAdd;
    };

offset returns[int offsetValue]
  : OFFSET
    {
      String offsetText = $OFFSET.text;
      if (offsetText.charAt(0) == '+') {
        offsetText = offsetText.substring(1);
      }
      $offsetValue = LiteralTools.parseInt(offsetText);
    };

offset_or_label_absolute[int baseAddress] returns[int address]
  : offset {$address = $offset.offsetValue + $baseAddress;}
  | label_ref {$address = $label_ref.labelAddress;};

offset_or_label returns[int offsetValue]
  : offset {$offsetValue = $offset.offsetValue;}
  | label_ref {$offsetValue = $label_ref.labelAddress-$method::currentAddress;};


register_list[int totalMethodRegisters, int methodParameterRegisters] returns[byte[\] registers, byte registerCount]
  @init
  {
    $registers = new byte[5];
    $registerCount = 0;
  }
  : ^(I_REGISTER_LIST
      (REGISTER
      {
        if ($registerCount == 5) {
          throw new SemanticException(input, $I_REGISTER_LIST, "A list of registers can only have a maximum of 5 registers. Use the <op>/range alternate opcode instead.");
        }
        $registers[$registerCount++] = parseRegister_nibble($REGISTER.text, $totalMethodRegisters, $methodParameterRegisters);
      })*);

register_range[int totalMethodRegisters, int methodParameterRegisters] returns[int startRegister, int endRegister]
  : ^(I_REGISTER_RANGE (startReg=REGISTER endReg=REGISTER?)?)
    {
        if ($startReg == null) {
            $startRegister = 0;
            $endRegister = -1;
        } else {
                $startRegister  = parseRegister_short($startReg.text, $totalMethodRegisters, $methodParameterRegisters);
                if ($endReg == null) {
                    $endRegister = $startRegister;
                } else {
                    $endRegister = parseRegister_short($endReg.text, $totalMethodRegisters, $methodParameterRegisters);
                }

                int registerCount = $endRegister-$startRegister+1;
                if (registerCount < 1) {
                    throw new SemanticException(input, $I_REGISTER_RANGE, "A register range must have the lower register listed first");
                }
            }
    }
  ;

verification_error_reference returns[Item item]
  : CLASS_DESCRIPTOR
  {
    $item = TypeIdItem.internTypeIdItem(dexFile, $start.getText());
  }
  | fully_qualified_field
  {
    $item = $fully_qualified_field.fieldIdItem;
  }
  | fully_qualified_method
  {
    $item = $fully_qualified_method.methodIdItem;
  };

verification_error_type returns[VerificationErrorType verificationErrorType]
  : VERIFICATION_ERROR_TYPE
  {
    $verificationErrorType = VerificationErrorType.fromString($VERIFICATION_ERROR_TYPE.text);
  };

instruction[int totalMethodRegisters, int methodParameterRegisters, List<Instruction> instructions] returns[int outRegisters]
  : insn_format10t[$totalMethodRegisters, $methodParameterRegisters, $instructions] { $outRegisters = $insn_format10t.outRegisters; }
  | insn_format10x[$totalMethodRegisters, $methodParameterRegisters, $instructions] { $outRegisters = $insn_format10x.outRegisters; }
  | insn_format11n[$totalMethodRegisters, $methodParameterRegisters, $instructions] { $outRegisters = $insn_format11n.outRegisters; }
  | insn_format11x[$totalMethodRegisters, $methodParameterRegisters, $instructions] { $outRegisters = $insn_format11x.outRegisters; }
  | insn_format12x[$totalMethodRegisters, $methodParameterRegisters, $instructions] { $outRegisters = $insn_format12x.outRegisters; }
  | insn_format20bc[$totalMethodRegisters, $methodParameterRegisters, $instructions] { $outRegisters = $insn_format20bc.outRegisters; }
  | insn_format20t[$totalMethodRegisters, $methodParameterRegisters, $instructions] { $outRegisters = $insn_format20t.outRegisters; }
  | insn_format21c_field[$totalMethodRegisters, $methodParameterRegisters, $instructions] { $outRegisters = $insn_format21c_field.outRegisters; }
  | insn_format21c_string[$totalMethodRegisters, $methodParameterRegisters, $instructions] { $outRegisters = $insn_format21c_string.outRegisters; }
  | insn_format21c_type[$totalMethodRegisters, $methodParameterRegisters, $instructions] { $outRegisters = $insn_format21c_type.outRegisters; }
  | insn_format21h[$totalMethodRegisters, $methodParameterRegisters, $instructions] { $outRegisters = $insn_format21h.outRegisters; }
  | insn_format21s[$totalMethodRegisters, $methodParameterRegisters, $instructions] { $outRegisters = $insn_format21s.outRegisters; }
  | insn_format21t[$totalMethodRegisters, $methodParameterRegisters, $instructions] { $outRegisters = $insn_format21t.outRegisters; }
  | insn_format22b[$totalMethodRegisters, $methodParameterRegisters, $instructions] { $outRegisters = $insn_format22b.outRegisters; }
  | insn_format22c_field[$totalMethodRegisters, $methodParameterRegisters, $instructions] { $outRegisters = $insn_format22c_field.outRegisters; }
  | insn_format22c_type[$totalMethodRegisters, $methodParameterRegisters, $instructions] { $outRegisters = $insn_format22c_type.outRegisters; }
  | insn_format22s[$totalMethodRegisters, $methodParameterRegisters, $instructions] { $outRegisters = $insn_format22s.outRegisters; }
  | insn_format22t[$totalMethodRegisters, $methodParameterRegisters, $instructions] { $outRegisters = $insn_format22t.outRegisters; }
  | insn_format22x[$totalMethodRegisters, $methodParameterRegisters, $instructions] { $outRegisters = $insn_format22x.outRegisters; }
  | insn_format23x[$totalMethodRegisters, $methodParameterRegisters, $instructions] { $outRegisters = $insn_format23x.outRegisters; }
  | insn_format30t[$totalMethodRegisters, $methodParameterRegisters, $instructions] { $outRegisters = $insn_format30t.outRegisters; }
  | insn_format31c[$totalMethodRegisters, $methodParameterRegisters, $instructions] { $outRegisters = $insn_format31c.outRegisters; }
  | insn_format31i[$totalMethodRegisters, $methodParameterRegisters, $instructions] { $outRegisters = $insn_format31i.outRegisters; }
  | insn_format31t[$totalMethodRegisters, $methodParameterRegisters, $instructions] { $outRegisters = $insn_format31t.outRegisters; }
  | insn_format32x[$totalMethodRegisters, $methodParameterRegisters, $instructions] { $outRegisters = $insn_format32x.outRegisters; }
  | insn_format35c_method[$totalMethodRegisters, $methodParameterRegisters, $instructions] { $outRegisters = $insn_format35c_method.outRegisters; }
  | insn_format35c_type[$totalMethodRegisters, $methodParameterRegisters, $instructions] { $outRegisters = $insn_format35c_type.outRegisters; }
  | insn_format3rc_method[$totalMethodRegisters, $methodParameterRegisters, $instructions] { $outRegisters = $insn_format3rc_method.outRegisters; }
  | insn_format3rc_type[$totalMethodRegisters, $methodParameterRegisters, $instructions] { $outRegisters = $insn_format3rc_type.outRegisters; }
  | insn_format41c_type[$totalMethodRegisters, $methodParameterRegisters, $instructions] { $outRegisters = $insn_format41c_type.outRegisters; }
  | insn_format41c_field[$totalMethodRegisters, $methodParameterRegisters, $instructions] { $outRegisters = $insn_format41c_field.outRegisters; }
  | insn_format51l_type[$totalMethodRegisters, $methodParameterRegisters, $instructions] { $outRegisters = $insn_format51l_type.outRegisters; }
  | insn_format52c_type[$totalMethodRegisters, $methodParameterRegisters, $instructions] { $outRegisters = $insn_format52c_type.outRegisters; }
  | insn_format52c_field[$totalMethodRegisters, $methodParameterRegisters, $instructions] { $outRegisters = $insn_format52c_field.outRegisters; }
  | insn_format5rc_method[$totalMethodRegisters, $methodParameterRegisters, $instructions] { $outRegisters = $insn_format5rc_method.outRegisters; }
  | insn_format5rc_type[$totalMethodRegisters, $methodParameterRegisters, $instructions] { $outRegisters = $insn_format5rc_type.outRegisters; }
  | insn_array_data_directive[$totalMethodRegisters, $methodParameterRegisters, $instructions] { $outRegisters = $insn_array_data_directive.outRegisters; }
  | insn_packed_switch_directive[$totalMethodRegisters, $methodParameterRegisters, $instructions] { $outRegisters = $insn_packed_switch_directive.outRegisters; }
  | insn_sparse_switch_directive[$totalMethodRegisters, $methodParameterRegisters, $instructions] { $outRegisters = $insn_sparse_switch_directive.outRegisters; };
    catch [Exception ex] {
      reportError(new SemanticException(input, ex));
      recover(input, null);
    }


insn_format10t[int totalMethodRegisters, int methodParameterRegisters, List<Instruction> instructions] returns[int outRegisters]
  : //e.g. goto endloop:
    {$outRegisters = 0;}
    ^(I_STATEMENT_FORMAT10t INSTRUCTION_FORMAT10t offset_or_label)
    {
      Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT10t.text);

      int addressOffset = $offset_or_label.offsetValue;

      $instructions.add(new Instruction10t(opcode, addressOffset));
    };

insn_format10x[int totalMethodRegisters, int methodParameterRegisters, List<Instruction> instructions] returns[int outRegisters]
  : //e.g. return
    ^(I_STATEMENT_FORMAT10x INSTRUCTION_FORMAT10x)
    {
      Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT10x.text);
      $instructions.add(new Instruction10x(opcode));
    };

insn_format11n[int totalMethodRegisters, int methodParameterRegisters, List<Instruction> instructions] returns[int outRegisters]
  : //e.g. const/4 v0, 5
    ^(I_STATEMENT_FORMAT11n INSTRUCTION_FORMAT11n REGISTER short_integral_literal)
    {
      Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT11n.text);
      byte regA = parseRegister_nibble($REGISTER.text, $totalMethodRegisters, $methodParameterRegisters);

      short litB = $short_integral_literal.value;
      LiteralTools.checkNibble(litB);

      $instructions.add(new Instruction11n(opcode, regA, (byte)litB));
    };

insn_format11x[int totalMethodRegisters, int methodParameterRegisters, List<Instruction> instructions] returns[int outRegisters]
  : //e.g. move-result-object v1
    ^(I_STATEMENT_FORMAT11x INSTRUCTION_FORMAT11x REGISTER)
    {
      Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT11x.text);
      short regA = parseRegister_byte($REGISTER.text, $totalMethodRegisters, $methodParameterRegisters);

      $instructions.add(new Instruction11x(opcode, regA));
    };

insn_format12x[int totalMethodRegisters, int methodParameterRegisters, List<Instruction> instructions] returns[int outRegisters]
  : //e.g. move v1 v2
    ^(I_STATEMENT_FORMAT12x INSTRUCTION_FORMAT12x registerA=REGISTER registerB=REGISTER)
    {
      Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT12x.text);
      byte regA = parseRegister_nibble($registerA.text, $totalMethodRegisters, $methodParameterRegisters);
      byte regB = parseRegister_nibble($registerB.text, $totalMethodRegisters, $methodParameterRegisters);

      $instructions.add(new Instruction12x(opcode, regA, regB));
    };

insn_format20bc[int totalMethodRegisters, int methodParameterRegisters, List<Instruction> instructions] returns[int outRegisters]
  : //e.g. throw-verification-error generic-error, Lsome/class;
    ^(I_STATEMENT_FORMAT20bc INSTRUCTION_FORMAT20bc verification_error_type verification_error_reference)
    {
      Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT20bc.text);

      VerificationErrorType verificationErrorType = $verification_error_type.verificationErrorType;
      Item referencedItem = $verification_error_reference.item;

      $instructions.add(new Instruction20bc(opcode, verificationErrorType, referencedItem));
    };

insn_format20t[int totalMethodRegisters, int methodParameterRegisters, List<Instruction> instructions] returns[int outRegisters]
  : //e.g. goto/16 endloop:
    ^(I_STATEMENT_FORMAT20t INSTRUCTION_FORMAT20t offset_or_label)
    {
      Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT20t.text);

      int addressOffset = $offset_or_label.offsetValue;

      $instructions.add(new Instruction20t(opcode, addressOffset));
    };

insn_format21c_field[int totalMethodRegisters, int methodParameterRegisters, List<Instruction> instructions] returns[int outRegisters]
  : //e.g. sget_object v0, java/lang/System/out LJava/io/PrintStream;
    ^(I_STATEMENT_FORMAT21c_FIELD inst=(INSTRUCTION_FORMAT21c_FIELD | INSTRUCTION_FORMAT21c_FIELD_ODEX) REGISTER fully_qualified_field)
    {
      Opcode opcode = Opcode.getOpcodeByName($inst.text);
      short regA = parseRegister_byte($REGISTER.text, $totalMethodRegisters, $methodParameterRegisters);

      FieldIdItem fieldIdItem = $fully_qualified_field.fieldIdItem;

      $instructions.add(new Instruction21c(opcode, regA, fieldIdItem));
    };

insn_format21c_string[int totalMethodRegisters, int methodParameterRegisters, List<Instruction> instructions] returns[int outRegisters]
  : //e.g. const-string v1, "Hello World!"
    ^(I_STATEMENT_FORMAT21c_STRING INSTRUCTION_FORMAT21c_STRING REGISTER string_literal)
    {
      Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT21c_STRING.text);
      short regA = parseRegister_byte($REGISTER.text, $totalMethodRegisters, $methodParameterRegisters);

      StringIdItem stringIdItem = StringIdItem.internStringIdItem(dexFile, $string_literal.value);

      instructions.add(new Instruction21c(opcode, regA, stringIdItem));
    };

insn_format21c_type[int totalMethodRegisters, int methodParameterRegisters, List<Instruction> instructions] returns[int outRegisters]
  : //e.g. const-class v2, org/jf/HelloWorld2/HelloWorld2
    ^(I_STATEMENT_FORMAT21c_TYPE INSTRUCTION_FORMAT21c_TYPE REGISTER reference_type_descriptor)
    {
      Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT21c_TYPE.text);
      short regA = parseRegister_byte($REGISTER.text, $totalMethodRegisters, $methodParameterRegisters);

      TypeIdItem typeIdItem = $reference_type_descriptor.type;

      $instructions.add(new Instruction21c(opcode, regA, typeIdItem));
    };

insn_format21h[int totalMethodRegisters, int methodParameterRegisters, List<Instruction> instructions] returns[int outRegisters]
  : //e.g. const/high16 v1, 1234
    ^(I_STATEMENT_FORMAT21h INSTRUCTION_FORMAT21h REGISTER short_integral_literal)
    {
      Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT21h.text);
      short regA = parseRegister_byte($REGISTER.text, $totalMethodRegisters, $methodParameterRegisters);

      short litB = $short_integral_literal.value;

      instructions.add(new Instruction21h(opcode, regA, litB));
    };

insn_format21s[int totalMethodRegisters, int methodParameterRegisters, List<Instruction> instructions] returns[int outRegisters]
  : //e.g. const/16 v1, 1234
    ^(I_STATEMENT_FORMAT21s INSTRUCTION_FORMAT21s REGISTER short_integral_literal)
    {
      Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT21s.text);
      short regA = parseRegister_byte($REGISTER.text, $totalMethodRegisters, $methodParameterRegisters);

      short litB = $short_integral_literal.value;

      $instructions.add(new Instruction21s(opcode, regA, litB));
    };

insn_format21t[int totalMethodRegisters, int methodParameterRegisters, List<Instruction> instructions] returns[int outRegisters]
  : //e.g. if-eqz v0, endloop:
    ^(I_STATEMENT_FORMAT21t INSTRUCTION_FORMAT21t REGISTER offset_or_label)
    {
      Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT21t.text);
      short regA = parseRegister_byte($REGISTER.text, $totalMethodRegisters, $methodParameterRegisters);

      int addressOffset = $offset_or_label.offsetValue;

      if (addressOffset < Short.MIN_VALUE || addressOffset > Short.MAX_VALUE) {
        throw new SemanticException(input, $offset_or_label.start, "The offset/label is out of range. The offset is " + Integer.toString(addressOffset) + " and the range for this opcode is [-32768, 32767].");
      }

      $instructions.add(new Instruction21t(opcode, regA, (short)addressOffset));
    };

insn_format22b[int totalMethodRegisters, int methodParameterRegisters, List<Instruction> instructions] returns[int outRegisters]
  : //e.g. add-int v0, v1, 123
    ^(I_STATEMENT_FORMAT22b INSTRUCTION_FORMAT22b registerA=REGISTER registerB=REGISTER short_integral_literal)
    {
      Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT22b.text);
      short regA = parseRegister_byte($registerA.text, $totalMethodRegisters, $methodParameterRegisters);
      short regB = parseRegister_byte($registerB.text, $totalMethodRegisters, $methodParameterRegisters);

      short litC = $short_integral_literal.value;
      LiteralTools.checkByte(litC);

      $instructions.add(new Instruction22b(opcode, regA, regB, (byte)litC));
    };

insn_format22c_field[int totalMethodRegisters, int methodParameterRegisters, List<Instruction> instructions] returns[int outRegisters]
  : //e.g. iput-object v1, v0, org/jf/HelloWorld2/HelloWorld2.helloWorld Ljava/lang/String;
    ^(I_STATEMENT_FORMAT22c_FIELD inst=(INSTRUCTION_FORMAT22c_FIELD | INSTRUCTION_FORMAT22c_FIELD_ODEX) registerA=REGISTER registerB=REGISTER fully_qualified_field)
    {
      Opcode opcode = Opcode.getOpcodeByName($inst.text);
      byte regA = parseRegister_nibble($registerA.text, $totalMethodRegisters, $methodParameterRegisters);
      byte regB = parseRegister_nibble($registerB.text, $totalMethodRegisters, $methodParameterRegisters);

      FieldIdItem fieldIdItem = $fully_qualified_field.fieldIdItem;

      $instructions.add(new Instruction22c(opcode, regA, regB, fieldIdItem));
    };

insn_format22c_type[int totalMethodRegisters, int methodParameterRegisters, List<Instruction> instructions] returns[int outRegisters]
  : //e.g. instance-of v0, v1, Ljava/lang/String;
    ^(I_STATEMENT_FORMAT22c_TYPE INSTRUCTION_FORMAT22c_TYPE registerA=REGISTER registerB=REGISTER nonvoid_type_descriptor)
    {
      Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT22c_TYPE.text);
      byte regA = parseRegister_nibble($registerA.text, $totalMethodRegisters, $methodParameterRegisters);
      byte regB = parseRegister_nibble($registerB.text, $totalMethodRegisters, $methodParameterRegisters);

      TypeIdItem typeIdItem = $nonvoid_type_descriptor.type;

      $instructions.add(new Instruction22c(opcode, regA, regB, typeIdItem));
    };

insn_format22s[int totalMethodRegisters, int methodParameterRegisters, List<Instruction> instructions] returns[int outRegisters]
  : //e.g. add-int/lit16 v0, v1, 12345
    ^(I_STATEMENT_FORMAT22s INSTRUCTION_FORMAT22s registerA=REGISTER registerB=REGISTER short_integral_literal)
    {
      Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT22s.text);
      byte regA = parseRegister_nibble($registerA.text, $totalMethodRegisters, $methodParameterRegisters);
      byte regB = parseRegister_nibble($registerB.text, $totalMethodRegisters, $methodParameterRegisters);

      short litC = $short_integral_literal.value;

      $instructions.add(new Instruction22s(opcode, regA, regB, litC));
    };

insn_format22t[int totalMethodRegisters, int methodParameterRegisters, List<Instruction> instructions] returns[int outRegisters]
  : //e.g. if-eq v0, v1, endloop:
    ^(I_STATEMENT_FORMAT22t INSTRUCTION_FORMAT22t registerA=REGISTER registerB=REGISTER offset_or_label)
    {
      Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT22t.text);
      byte regA = parseRegister_nibble($registerA.text, $totalMethodRegisters, $methodParameterRegisters);
      byte regB = parseRegister_nibble($registerB.text, $totalMethodRegisters, $methodParameterRegisters);

      int addressOffset = $offset_or_label.offsetValue;

      if (addressOffset < Short.MIN_VALUE || addressOffset > Short.MAX_VALUE) {
        throw new SemanticException(input, $offset_or_label.start, "The offset/label is out of range. The offset is " + Integer.toString(addressOffset) + " and the range for this opcode is [-32768, 32767].");
      }

      $instructions.add(new Instruction22t(opcode, regA, regB, (short)addressOffset));
    };

insn_format22x[int totalMethodRegisters, int methodParameterRegisters, List<Instruction> instructions] returns[int outRegisters]
  : //e.g. move/from16 v1, v1234
    ^(I_STATEMENT_FORMAT22x INSTRUCTION_FORMAT22x registerA=REGISTER registerB=REGISTER)
    {
      Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT22x.text);
      short regA = parseRegister_byte($registerA.text, $totalMethodRegisters, $methodParameterRegisters);
      int regB = parseRegister_short($registerB.text, $totalMethodRegisters, $methodParameterRegisters);

      $instructions.add(new Instruction22x(opcode, regA, regB));
    };

insn_format23x[int totalMethodRegisters, int methodParameterRegisters, List<Instruction> instructions] returns[int outRegisters]
  : //e.g. add-int v1, v2, v3
    ^(I_STATEMENT_FORMAT23x INSTRUCTION_FORMAT23x registerA=REGISTER registerB=REGISTER registerC=REGISTER)
    {
      Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT23x.text);
      short regA = parseRegister_byte($registerA.text, $totalMethodRegisters, $methodParameterRegisters);
      short regB = parseRegister_byte($registerB.text, $totalMethodRegisters, $methodParameterRegisters);
      short regC = parseRegister_byte($registerC.text, $totalMethodRegisters, $methodParameterRegisters);

      $instructions.add(new Instruction23x(opcode, regA, regB, regC));
    };

insn_format30t[int totalMethodRegisters, int methodParameterRegisters, List<Instruction> instructions] returns[int outRegisters]
  : //e.g. goto/32 endloop:
    ^(I_STATEMENT_FORMAT30t INSTRUCTION_FORMAT30t offset_or_label)
    {
      Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT30t.text);

      int addressOffset = $offset_or_label.offsetValue;

      $instructions.add(new Instruction30t(opcode, addressOffset));
    };

insn_format31c[int totalMethodRegisters, int methodParameterRegisters, List<Instruction> instructions] returns[int outRegisters]
  : //e.g. const-string/jumbo v1 "Hello World!"
    ^(I_STATEMENT_FORMAT31c INSTRUCTION_FORMAT31c REGISTER string_literal)
    {
      Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT31c.text);
      short regA = parseRegister_byte($REGISTER.text, $totalMethodRegisters, $methodParameterRegisters);

      StringIdItem stringIdItem = StringIdItem.internStringIdItem(dexFile, $string_literal.value);

      $instructions.add(new Instruction31c(opcode, regA, stringIdItem));
    };

insn_format31i[int totalMethodRegisters, int methodParameterRegisters, List<Instruction> instructions] returns[int outRegisters]
  : //e.g. const v0, 123456
    ^(I_STATEMENT_FORMAT31i INSTRUCTION_FORMAT31i REGISTER fixed_32bit_literal)
    {
      Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT31i.text);
      short regA = parseRegister_byte($REGISTER.text, $totalMethodRegisters, $methodParameterRegisters);

      int litB = $fixed_32bit_literal.value;

      $instructions.add(new Instruction31i(opcode, regA, litB));
    };

insn_format31t[int totalMethodRegisters, int methodParameterRegisters, List<Instruction> instructions] returns[int outRegisters]
  : //e.g. fill-array-data v0, ArrayData:
    ^(I_STATEMENT_FORMAT31t INSTRUCTION_FORMAT31t REGISTER offset_or_label)
    {
      Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT31t.text);

      short regA = parseRegister_byte($REGISTER.text, $totalMethodRegisters, $methodParameterRegisters);

      int addressOffset = $offset_or_label.offsetValue;
      if (($method::currentAddress + addressOffset) \% 2 != 0) {
        addressOffset++;
      }

      $instructions.add(new Instruction31t(opcode, regA, addressOffset));
    };

insn_format32x[int totalMethodRegisters, int methodParameterRegisters, List<Instruction> instructions] returns[int outRegisters]
  : //e.g. move/16 v5678, v1234
    ^(I_STATEMENT_FORMAT32x INSTRUCTION_FORMAT32x registerA=REGISTER registerB=REGISTER)
    {
      Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT32x.text);
      int regA = parseRegister_short($registerA.text, $totalMethodRegisters, $methodParameterRegisters);
      int regB = parseRegister_short($registerB.text, $totalMethodRegisters, $methodParameterRegisters);

      $instructions.add(new Instruction32x(opcode, regA, regB));
    };

insn_format35c_method[int totalMethodRegisters, int methodParameterRegisters, List<Instruction> instructions] returns[int outRegisters]
  : //e.g. invoke-virtual {v0,v1} java/io/PrintStream/print(Ljava/lang/Stream;)V
    ^(I_STATEMENT_FORMAT35c_METHOD INSTRUCTION_FORMAT35c_METHOD register_list[$totalMethodRegisters, $methodParameterRegisters] fully_qualified_method)
    {
      Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT35c_METHOD.text);

      //this depends on the fact that register_list returns a byte[5]
      byte[] registers = $register_list.registers;
      byte registerCount = $register_list.registerCount;
      $outRegisters = registerCount;

      MethodIdItem methodIdItem = $fully_qualified_method.methodIdItem;

      $instructions.add(new Instruction35c(opcode, registerCount, registers[0], registers[1], registers[2], registers[3], registers[4], methodIdItem));
    };

insn_format35c_type[int totalMethodRegisters, int methodParameterRegisters, List<Instruction> instructions] returns[int outRegisters]
  : //e.g. filled-new-array {v0,v1}, I
    ^(I_STATEMENT_FORMAT35c_TYPE INSTRUCTION_FORMAT35c_TYPE register_list[$totalMethodRegisters, $methodParameterRegisters] nonvoid_type_descriptor)
    {
      Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT35c_TYPE.text);

      //this depends on the fact that register_list returns a byte[5]
      byte[] registers = $register_list.registers;
      byte registerCount = $register_list.registerCount;
      $outRegisters = registerCount;

      TypeIdItem typeIdItem = $nonvoid_type_descriptor.type;

      $instructions.add(new Instruction35c(opcode, registerCount, registers[0], registers[1], registers[2], registers[3], registers[4], typeIdItem));
    };

insn_format3rc_method[int totalMethodRegisters, int methodParameterRegisters, List<Instruction> instructions] returns[int outRegisters]
  : //e.g. invoke-virtual/range {v25..v26} java/lang/StringBuilder/append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    ^(I_STATEMENT_FORMAT3rc_METHOD INSTRUCTION_FORMAT3rc_METHOD register_range[$totalMethodRegisters, $methodParameterRegisters] fully_qualified_method)
    {
      Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT3rc_METHOD.text);
      int startRegister = $register_range.startRegister;
      int endRegister = $register_range.endRegister;

      int registerCount = endRegister-startRegister+1;
      $outRegisters = registerCount;

      MethodIdItem methodIdItem = $fully_qualified_method.methodIdItem;

      $instructions.add(new Instruction3rc(opcode, (short)registerCount, startRegister, methodIdItem));
    };

insn_format3rc_type[int totalMethodRegisters, int methodParameterRegisters, List<Instruction> instructions] returns[int outRegisters]
  : //e.g. filled-new-array/range {v0..v6} I
    ^(I_STATEMENT_FORMAT3rc_TYPE INSTRUCTION_FORMAT3rc_TYPE register_range[$totalMethodRegisters, $methodParameterRegisters] nonvoid_type_descriptor)
    {
      Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT3rc_TYPE.text);
      int startRegister = $register_range.startRegister;
      int endRegister = $register_range.endRegister;

      int registerCount = endRegister-startRegister+1;
      $outRegisters = registerCount;

      TypeIdItem typeIdItem = $nonvoid_type_descriptor.type;

      $instructions.add(new Instruction3rc(opcode, (short)registerCount, startRegister, typeIdItem));
    };

insn_format41c_type[int totalMethodRegisters, int methodParameterRegisters, List<Instruction> instructions] returns[int outRegisters]
  : //e.g. const-class/jumbo v2, org/jf/HelloWorld2/HelloWorld2
    ^(I_STATEMENT_FORMAT41c_TYPE INSTRUCTION_FORMAT41c_TYPE REGISTER reference_type_descriptor)
    {
      Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT41c_TYPE.text);
      int regA = parseRegister_short($REGISTER.text, $totalMethodRegisters, $methodParameterRegisters);

      TypeIdItem typeIdItem = $reference_type_descriptor.type;

      $instructions.add(new Instruction41c(opcode, regA, typeIdItem));
    };

insn_format41c_field[int totalMethodRegisters, int methodParameterRegisters, List<Instruction> instructions] returns[int outRegisters]
  : //e.g. sget-object/jumbo v0, Ljava/lang/System;->out:LJava/io/PrintStream;
    ^(I_STATEMENT_FORMAT41c_FIELD INSTRUCTION_FORMAT41c_FIELD REGISTER fully_qualified_field)
    {
      Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT41c_FIELD.text);
      int regA = parseRegister_short($REGISTER.text, $totalMethodRegisters, $methodParameterRegisters);

      FieldIdItem fieldIdItem = $fully_qualified_field.fieldIdItem;

      $instructions.add(new Instruction41c(opcode, regA, fieldIdItem));
    };

insn_format51l_type[int totalMethodRegisters, int methodParameterRegisters, List<Instruction> instructions] returns[int outRegisters]
  : //e.g. const-wide v0, 5000000000L
    ^(I_STATEMENT_FORMAT51l INSTRUCTION_FORMAT51l REGISTER fixed_64bit_literal)
    {
      Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT51l.text);
      short regA = parseRegister_byte($REGISTER.text, $totalMethodRegisters, $methodParameterRegisters);

      long litB = $fixed_64bit_literal.value;

      $instructions.add(new Instruction51l(opcode, regA, litB));
    };

insn_format52c_type[int totalMethodRegisters, int methodParameterRegisters, List<Instruction> instructions] returns[int outRegisters]
  : //e.g. instance-of/jumbo v0, v1, Ljava/lang/String;
    ^(I_STATEMENT_FORMAT52c_TYPE INSTRUCTION_FORMAT52c_TYPE registerA=REGISTER registerB=REGISTER nonvoid_type_descriptor)
    {
      Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT52c_TYPE.text);
      int regA = parseRegister_short($registerA.text, $totalMethodRegisters, $methodParameterRegisters);
      int regB = parseRegister_short($registerB.text, $totalMethodRegisters, $methodParameterRegisters);

      TypeIdItem typeIdItem = $nonvoid_type_descriptor.type;

      $instructions.add(new Instruction52c(opcode, regA, regB, typeIdItem));
    };

insn_format52c_field[int totalMethodRegisters, int methodParameterRegisters, List<Instruction> instructions] returns[int outRegisters]
  : //e.g. iput-object/jumbo v1, v0, Lorg/jf/HelloWorld2/HelloWorld2;->helloWorld:Ljava/lang/String;
    ^(I_STATEMENT_FORMAT52c_FIELD INSTRUCTION_FORMAT52c_FIELD registerA=REGISTER registerB=REGISTER fully_qualified_field)
    {
      Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT52c_FIELD.text);
      int regA = parseRegister_short($registerA.text, $totalMethodRegisters, $methodParameterRegisters);
      int regB = parseRegister_short($registerB.text, $totalMethodRegisters, $methodParameterRegisters);

      FieldIdItem fieldIdItem = $fully_qualified_field.fieldIdItem;

      $instructions.add(new Instruction52c(opcode, regA, regB, fieldIdItem));
    };

insn_format5rc_method[int totalMethodRegisters, int methodParameterRegisters, List<Instruction> instructions] returns[int outRegisters]
  : //e.g. invoke-virtual/jumbo {v25..v26} java/lang/StringBuilder/append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    ^(I_STATEMENT_FORMAT5rc_METHOD INSTRUCTION_FORMAT5rc_METHOD register_range[$totalMethodRegisters, $methodParameterRegisters] fully_qualified_method)
    {
      Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT5rc_METHOD.text);
      int startRegister = $register_range.startRegister;
      int endRegister = $register_range.endRegister;

      int registerCount = endRegister-startRegister+1;
      $outRegisters = registerCount;

      MethodIdItem methodIdItem = $fully_qualified_method.methodIdItem;

      $instructions.add(new Instruction5rc(opcode, registerCount, startRegister, methodIdItem));
    };

insn_format5rc_type[int totalMethodRegisters, int methodParameterRegisters, List<Instruction> instructions] returns[int outRegisters]
  : //e.g. filled-new-array/jumbo {v0..v6} I
    ^(I_STATEMENT_FORMAT5rc_TYPE INSTRUCTION_FORMAT5rc_TYPE register_range[$totalMethodRegisters, $methodParameterRegisters] nonvoid_type_descriptor)
    {
      Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT5rc_TYPE.text);
      int startRegister = $register_range.startRegister;
      int endRegister = $register_range.endRegister;

      int registerCount = endRegister-startRegister+1;
      $outRegisters = registerCount;

      TypeIdItem typeIdItem = $nonvoid_type_descriptor.type;

      $instructions.add(new Instruction5rc(opcode, registerCount, startRegister, typeIdItem));
    };

insn_array_data_directive[int totalMethodRegisters, int methodParameterRegisters, List<Instruction> instructions] returns[int outRegisters]
  : //e.g. .array-data 4 1000000 .end array-data
    ^(I_STATEMENT_ARRAY_DATA ^(I_ARRAY_ELEMENT_SIZE short_integral_literal) array_elements)
    {
      if (($method::currentAddress \% 2) != 0) {
        $instructions.add(new Instruction10x(Opcode.NOP));
        $method::currentAddress++;
      }

      int elementWidth = $short_integral_literal.value;
      List<byte[]> byteValues = $array_elements.values;

      int length = 0;
      for (byte[] byteValue: byteValues) {
        length+=byteValue.length;
      }

      byte[] encodedValues = new byte[length];
      int index = 0;
      for (byte[] byteValue: byteValues) {
        System.arraycopy(byteValue, 0, encodedValues, index, byteValue.length);
        index+=byteValue.length;
      }

      $instructions.add(new ArrayDataPseudoInstruction(elementWidth, encodedValues));
    };

insn_packed_switch_directive[int totalMethodRegisters, int methodParameterRegisters, List<Instruction> instructions] returns[int outRegisters]
  :
    ^(I_STATEMENT_PACKED_SWITCH ^(I_PACKED_SWITCH_START_KEY fixed_32bit_literal)
      {
        if (($method::currentAddress \% 2) != 0) {
          $instructions.add(new Instruction10x(Opcode.NOP));
          $method::currentAddress++;
        }
        Integer baseAddress = $method::packedSwitchDeclarations.get($method::currentAddress);
        if (baseAddress == null) {
          baseAddress = 0;
        }
      }
      packed_switch_targets[baseAddress])
    {

      int startKey = $fixed_32bit_literal.value;
      int[] targets = $packed_switch_targets.targets;

      $instructions.add(new PackedSwitchDataPseudoInstruction(startKey, targets));
    };

insn_sparse_switch_directive[int totalMethodRegisters, int methodParameterRegisters, List<Instruction> instructions] returns[int outRegisters]
  :
    ^(I_STATEMENT_SPARSE_SWITCH sparse_switch_target_count sparse_switch_keys[$sparse_switch_target_count.targetCount]
      {
        if (($method::currentAddress \% 2) != 0) {
          $instructions.add(new Instruction10x(Opcode.NOP));
          $method::currentAddress++;
        }
        Integer baseAddress = $method::sparseSwitchDeclarations.get($method::currentAddress);
        if (baseAddress == null) {
          baseAddress = 0;
        }
      }

      sparse_switch_targets[baseAddress, $sparse_switch_target_count.targetCount])
    {
      int[] keys = $sparse_switch_keys.keys;
      int[] targets = $sparse_switch_targets.targets;

      $instructions.add(new SparseSwitchDataPseudoInstruction(keys, targets));
    };

nonvoid_type_descriptor returns [TypeIdItem type]
  : (PRIMITIVE_TYPE
  | CLASS_DESCRIPTOR
  | ARRAY_DESCRIPTOR)
  {
    $type = TypeIdItem.internTypeIdItem(dexFile, $start.getText());
  };


reference_type_descriptor returns [TypeIdItem type]
  : (CLASS_DESCRIPTOR
  | ARRAY_DESCRIPTOR)
  {
    $type = TypeIdItem.internTypeIdItem(dexFile, $start.getText());
  };






class_type_descriptor returns [TypeIdItem type]
  : CLASS_DESCRIPTOR
  {
    $type = TypeIdItem.internTypeIdItem(dexFile, $CLASS_DESCRIPTOR.text);
  };

type_descriptor returns [TypeIdItem type]
  : VOID_TYPE {$type = TypeIdItem.internTypeIdItem(dexFile, "V");}
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

array_literal returns[EncodedValue[\] values]
  : {ArrayList<EncodedValue> valuesList = new ArrayList<EncodedValue>();}
    ^(I_ENCODED_ARRAY (literal {valuesList.add($literal.encodedValue);})*)
    {
      $values = new EncodedValue[valuesList.size()];
      valuesList.toArray($values);
    };


annotations returns[AnnotationSetItem annotationSetItem]
  : {ArrayList<AnnotationItem> annotationList = new ArrayList<AnnotationItem>();}
    ^(I_ANNOTATIONS (annotation {annotationList.add($annotation.annotationItem);} )*)
    {
      if (annotationList.size() > 0) {
        $annotationSetItem = AnnotationSetItem.internAnnotationSetItem(dexFile, annotationList);
      }
    };


annotation returns[AnnotationItem annotationItem]
  : ^(I_ANNOTATION ANNOTATION_VISIBILITY subannotation)
    {
      AnnotationVisibility visibility = AnnotationVisibility.valueOf($ANNOTATION_VISIBILITY.text.toUpperCase());
      AnnotationEncodedSubValue encodedAnnotation = new AnnotationEncodedSubValue($subannotation.annotationType,
          $subannotation.elementNames, $subannotation.elementValues);
      $annotationItem = AnnotationItem.internAnnotationItem(dexFile, visibility, encodedAnnotation);
    };

annotation_element returns[StringIdItem elementName, EncodedValue elementValue]
  : ^(I_ANNOTATION_ELEMENT SIMPLE_NAME literal)
    {
      $elementName = StringIdItem.internStringIdItem(dexFile, $SIMPLE_NAME.text);
      $elementValue = $literal.encodedValue;
    };

subannotation returns[TypeIdItem annotationType, StringIdItem[\] elementNames, EncodedValue[\] elementValues]
  : {ImmutableSortedMap.Builder<StringIdItem, EncodedValue> elementBuilder =
        ImmutableSortedMap.<StringIdItem, EncodedValue>naturalOrder();}
    ^(I_SUBANNOTATION
        class_type_descriptor
        (annotation_element
        {
          elementBuilder.put($annotation_element.elementName, $annotation_element.elementValue);
        }
        )*
     )
    {
      ImmutableSortedMap<StringIdItem, EncodedValue> elementMap = elementBuilder.build();

      $annotationType = $class_type_descriptor.type;

      $elementNames = new StringIdItem[elementMap.size()];
      $elementValues = new EncodedValue[elementMap.size()];

      elementMap.keySet().toArray($elementNames);
      elementMap.values().toArray($elementValues);
    };

field_literal returns[FieldIdItem value]
  : ^(I_ENCODED_FIELD fully_qualified_field)
    {
      $value = $fully_qualified_field.fieldIdItem;
    };

method_literal returns[MethodIdItem value]
  : ^(I_ENCODED_METHOD fully_qualified_method)
    {
      $value = $fully_qualified_method.methodIdItem;
    };

enum_literal returns[FieldIdItem value]
  : ^(I_ENCODED_ENUM fully_qualified_field)
    {
      $value = $fully_qualified_field.fieldIdItem;
    };
