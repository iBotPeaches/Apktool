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

parser grammar smaliParser;

options {
  output=AST;
  ASTLabelType=CommonTree;
}

tokens {
  //Lexer tokens
  ACCESS_SPEC;
  ANNOTATION_DIRECTIVE;
  ANNOTATION_VISIBILITY;
  ARRAY_DATA_DIRECTIVE;
  ARRAY_DESCRIPTOR;
  ARROW;
  BASE_ARRAY_DESCRIPTOR;
  BASE_CHAR_LITERAL;
  BASE_CLASS_DESCRIPTOR;
  BASE_FLOAT;
  BASE_FLOAT_OR_ID;
  BASE_INTEGER;
  BASE_PRIMITIVE_TYPE;
  BASE_SIMPLE_NAME;
  BASE_STRING_LITERAL;
  BASE_TYPE;
  BINARY_EXPONENT;
  BOOL_LITERAL;
  BYTE_LITERAL;
  CATCH_DIRECTIVE;
  CATCHALL_DIRECTIVE;
  CHAR_LITERAL;
  CLASS_DESCRIPTOR;
  CLASS_DIRECTIVE;
  CLOSE_BRACE;
  CLOSE_PAREN;
  COLON;
  COMMA;
  DECIMAL_EXPONENT;
  DOTDOT;
  DOUBLE_LITERAL;
  DOUBLE_LITERAL_OR_ID;
  END_ANNOTATION_DIRECTIVE;
  END_ARRAY_DATA_DIRECTIVE;
  END_FIELD_DIRECTIVE;
  END_LOCAL_DIRECTIVE;
  END_METHOD_DIRECTIVE;
  END_PACKED_SWITCH_DIRECTIVE;
  END_PARAMETER_DIRECTIVE;
  END_SPARSE_SWITCH_DIRECTIVE;
  END_SUBANNOTATION_DIRECTIVE;
  ENUM_DIRECTIVE;
  EPILOGUE_DIRECTIVE;
  EQUAL;
  ESCAPE_SEQUENCE;
  FIELD_DIRECTIVE;
  FIELD_OFFSET;
  FLOAT_LITERAL;
  FLOAT_LITERAL_OR_ID;
  HEX_DIGIT;
  HEX_DIGITS;
  HEX_PREFIX;
  IMPLEMENTS_DIRECTIVE;
  INLINE_INDEX;
  INSTRUCTION_FORMAT10t;
  INSTRUCTION_FORMAT10x;
  INSTRUCTION_FORMAT10x_ODEX;
  INSTRUCTION_FORMAT11n;
  INSTRUCTION_FORMAT11x;
  INSTRUCTION_FORMAT12x;
  INSTRUCTION_FORMAT12x_OR_ID;
  INSTRUCTION_FORMAT20bc;
  INSTRUCTION_FORMAT20t;
  INSTRUCTION_FORMAT21c_FIELD;
  INSTRUCTION_FORMAT21c_FIELD_ODEX;
  INSTRUCTION_FORMAT21c_STRING;
  INSTRUCTION_FORMAT21c_TYPE;
  INSTRUCTION_FORMAT21h;
  INSTRUCTION_FORMAT21s;
  INSTRUCTION_FORMAT21t;
  INSTRUCTION_FORMAT22b;
  INSTRUCTION_FORMAT22c_FIELD;
  INSTRUCTION_FORMAT22c_FIELD_ODEX;
  INSTRUCTION_FORMAT22c_TYPE;
  INSTRUCTION_FORMAT22cs_FIELD;
  INSTRUCTION_FORMAT22s;
  INSTRUCTION_FORMAT22s_OR_ID;
  INSTRUCTION_FORMAT22t;
  INSTRUCTION_FORMAT22x;
  INSTRUCTION_FORMAT23x;
  INSTRUCTION_FORMAT30t;
  INSTRUCTION_FORMAT31c;
  INSTRUCTION_FORMAT31i;
  INSTRUCTION_FORMAT31i_OR_ID;
  INSTRUCTION_FORMAT31t;
  INSTRUCTION_FORMAT32x;
  INSTRUCTION_FORMAT35c_METHOD;
  INSTRUCTION_FORMAT35c_METHOD_ODEX;
  INSTRUCTION_FORMAT35c_TYPE;
  INSTRUCTION_FORMAT35mi_METHOD;
  INSTRUCTION_FORMAT35ms_METHOD;
  INSTRUCTION_FORMAT3rc_METHOD;
  INSTRUCTION_FORMAT3rc_METHOD_ODEX;
  INSTRUCTION_FORMAT3rc_TYPE;
  INSTRUCTION_FORMAT3rmi_METHOD;
  INSTRUCTION_FORMAT3rms_METHOD;
  INSTRUCTION_FORMAT51l;
  INVALID_TOKEN;
  LINE_COMMENT;
  LINE_DIRECTIVE;
  LOCAL_DIRECTIVE;
  LOCALS_DIRECTIVE;
  LONG_LITERAL;
  METHOD_DIRECTIVE;
  METHOD_NAME;
  NEGATIVE_INTEGER_LITERAL;
  NULL_LITERAL;
  OFFSET;
  OPEN_BRACE;
  OPEN_PAREN;
  PACKED_SWITCH_DIRECTIVE;
  PARAM_LIST;
  PARAM_LIST_OR_ID;
  PARAMETER_DIRECTIVE;
  POSITIVE_INTEGER_LITERAL;
  PRIMITIVE_TYPE;
  PROLOGUE_DIRECTIVE;
  REGISTER;
  REGISTERS_DIRECTIVE;
  RESTART_LOCAL_DIRECTIVE;
  SHORT_LITERAL;
  SIMPLE_NAME;
  SOURCE_DIRECTIVE;
  SPARSE_SWITCH_DIRECTIVE;
  STRING_LITERAL;
  SUBANNOTATION_DIRECTIVE;
  SUPER_DIRECTIVE;
  VERIFICATION_ERROR_TYPE;
  VOID_TYPE;
  VTABLE_INDEX;
  WHITE_SPACE;

  //A couple of generated types that we remap other tokens to, to simplify the generated AST
  LABEL;
  INTEGER_LITERAL;

  //I_* tokens are imaginary tokens used as parent AST nodes
  I_CLASS_DEF;
  I_SUPER;
  I_IMPLEMENTS;
  I_SOURCE;
  I_ACCESS_LIST;
  I_METHODS;
  I_FIELDS;
  I_FIELD;
  I_FIELD_TYPE;
  I_FIELD_INITIAL_VALUE;
  I_METHOD;
  I_METHOD_PROTOTYPE;
  I_METHOD_RETURN_TYPE;
  I_REGISTERS;
  I_LOCALS;
  I_LABELS;
  I_LABEL;
  I_ANNOTATIONS;
  I_ANNOTATION;
  I_ANNOTATION_ELEMENT;
  I_SUBANNOTATION;
  I_ENCODED_FIELD;
  I_ENCODED_METHOD;
  I_ENCODED_ENUM;
  I_ENCODED_ARRAY;
  I_ARRAY_ELEMENT_SIZE;
  I_ARRAY_ELEMENTS;
  I_PACKED_SWITCH_START_KEY;
  I_PACKED_SWITCH_TARGET_COUNT;
  I_PACKED_SWITCH_TARGETS;
  I_PACKED_SWITCH_DECLARATION;
  I_PACKED_SWITCH_DECLARATIONS;
  I_SPARSE_SWITCH_KEYS;
  I_SPARSE_SWITCH_TARGET_COUNT;
  I_SPARSE_SWITCH_TARGETS;
  I_SPARSE_SWITCH_DECLARATION;
  I_SPARSE_SWITCH_DECLARATIONS;
  I_ADDRESS;
  I_CATCH;
  I_CATCHALL;
  I_CATCHES;
  I_PARAMETER;
  I_PARAMETERS;
  I_PARAMETER_NOT_SPECIFIED;
  I_ORDERED_DEBUG_DIRECTIVES;
  I_LINE;
  I_LOCAL;
  I_END_LOCAL;
  I_RESTART_LOCAL;
  I_PROLOGUE;
  I_EPILOGUE;
  I_STATEMENTS;
  I_STATEMENT_FORMAT10t;
  I_STATEMENT_FORMAT10x;
  I_STATEMENT_FORMAT11n;
  I_STATEMENT_FORMAT11x;
  I_STATEMENT_FORMAT12x;
  I_STATEMENT_FORMAT20bc;
  I_STATEMENT_FORMAT20t;
  I_STATEMENT_FORMAT21c_TYPE;
  I_STATEMENT_FORMAT21c_FIELD;
  I_STATEMENT_FORMAT21c_STRING;
  I_STATEMENT_FORMAT21h;
  I_STATEMENT_FORMAT21s;
  I_STATEMENT_FORMAT21t;
  I_STATEMENT_FORMAT22b;
  I_STATEMENT_FORMAT22c_FIELD;
  I_STATEMENT_FORMAT22c_TYPE;
  I_STATEMENT_FORMAT22s;
  I_STATEMENT_FORMAT22t;
  I_STATEMENT_FORMAT22x;
  I_STATEMENT_FORMAT23x;
  I_STATEMENT_FORMAT30t;
  I_STATEMENT_FORMAT31c;
  I_STATEMENT_FORMAT31i;
  I_STATEMENT_FORMAT31t;
  I_STATEMENT_FORMAT32x;
  I_STATEMENT_FORMAT35c_METHOD;
  I_STATEMENT_FORMAT35c_TYPE;
  I_STATEMENT_FORMAT3rc_METHOD;
  I_STATEMENT_FORMAT3rc_TYPE;
  I_STATEMENT_FORMAT41c_TYPE;
  I_STATEMENT_FORMAT41c_FIELD;
  I_STATEMENT_FORMAT51l;
  I_STATEMENT_FORMAT52c_TYPE;
  I_STATEMENT_FORMAT52c_FIELD;
  I_STATEMENT_FORMAT5rc_METHOD;
  I_STATEMENT_FORMAT5rc_TYPE;
  I_STATEMENT_ARRAY_DATA;
  I_STATEMENT_PACKED_SWITCH;
  I_STATEMENT_SPARSE_SWITCH;
  I_REGISTER_RANGE;
  I_REGISTER_LIST;
}

@header {
package org.jf.smali;

import org.jf.dexlib.Code.Format.*;
import org.jf.dexlib.Code.Opcode;
}


@members {
  public static final int ERROR_CHANNEL = 100;

  private boolean verboseErrors = false;
  private boolean allowOdex = false;
  private int apiLevel;

  public void setVerboseErrors(boolean verboseErrors) {
    this.verboseErrors = verboseErrors;
  }

  public void setAllowOdex(boolean allowOdex) {
      this.allowOdex = allowOdex;
  }

  public void setApiLevel(int apiLevel) {
      this.apiLevel = apiLevel;
  }

  public String getErrorMessage(RecognitionException e,
    String[] tokenNames) {

    if (verboseErrors) {
      List stack = getRuleInvocationStack(e, this.getClass().getName());
      String msg = null;

      if (e instanceof NoViableAltException) {
        NoViableAltException nvae = (NoViableAltException)e;
        msg = " no viable alt; token="+getTokenErrorDisplay(e.token)+
        " (decision="+nvae.decisionNumber+
        " state "+nvae.stateNumber+")"+
        " decision=<<"+nvae.grammarDecisionDescription+">>";
      } else {
        msg = super.getErrorMessage(e, tokenNames);
      }

      return stack + " " + msg;
    } else {
      return super.getErrorMessage(e, tokenNames);
    }
  }

  public String getTokenErrorDisplay(Token t) {
    if (!verboseErrors) {
      String s = t.getText();
      if ( s==null ) {
        if ( t.getType()==Token.EOF ) {
          s = "<EOF>";
        }
        else {
          s = "<"+tokenNames[t.getType()]+">";
        }
      }
      s = s.replaceAll("\n","\\\\n");
      s = s.replaceAll("\r","\\\\r");
      s = s.replaceAll("\t","\\\\t");
      return "'"+s+"'";
    }

    CommonToken ct = (CommonToken)t;

    String channelStr = "";
    if (t.getChannel()>0) {
      channelStr=",channel="+t.getChannel();
    }
    String txt = t.getText();
    if ( txt!=null ) {
      txt = txt.replaceAll("\n","\\\\n");
      txt = txt.replaceAll("\r","\\\\r");
      txt = txt.replaceAll("\t","\\\\t");
    }
    else {
      txt = "<no text>";
    }
    return "[@"+t.getTokenIndex()+","+ct.getStartIndex()+":"+ct.getStopIndex()+"='"+txt+"',<"+tokenNames[t.getType()]+">"+channelStr+","+t.getLine()+":"+t.getCharPositionInLine()+"]";
  }

  public String getErrorHeader(RecognitionException e) {
    return getSourceName()+"["+ e.line+","+e.charPositionInLine+"]";
  }

  private CommonTree buildTree(int type, String text, List<CommonTree> children) {
    CommonTree root = new CommonTree(new CommonToken(type, text));
    for (CommonTree child: children) {
      root.addChild(child);
    }
    return root;
  }

  private CommonToken getParamListSubToken(CommonToken baseToken, String str, int typeStartIndex) {
    CommonToken token = new CommonToken(baseToken);
    token.setStartIndex(baseToken.getStartIndex() + typeStartIndex);

    switch (str.charAt(typeStartIndex)) {
      case 'Z':
      case 'B':
      case 'S':
      case 'C':
      case 'I':
      case 'J':
      case 'F':
      case 'D':
      {
        token.setType(PRIMITIVE_TYPE);
        token.setText(str.substring(typeStartIndex, typeStartIndex+1));
        token.setStopIndex(baseToken.getStartIndex() + typeStartIndex);
        break;
      }
      case 'L':
      {
        int i = typeStartIndex;
        while (str.charAt(++i) != ';');

        token.setType(CLASS_DESCRIPTOR);
        token.setText(str.substring(typeStartIndex, i + 1));
        token.setStopIndex(baseToken.getStartIndex() + i);
        break;
      }
      case '[':
      {
        int i = typeStartIndex;
            while (str.charAt(++i) == '[');

            if (str.charAt(i++) == 'L') {
                while (str.charAt(i++) != ';');
        }

            token.setType(ARRAY_DESCRIPTOR);
            token.setText(str.substring(typeStartIndex, i));
            token.setStopIndex(baseToken.getStartIndex() + i - 1);
            break;
      }
      default:
        throw new RuntimeException(String.format("Invalid character '\%c' in param list \"\%s\" at position \%d", str.charAt(typeStartIndex), str, typeStartIndex));
    }

    return token;
  }

  private CommonTree parseParamList(CommonToken paramListToken) {
    String paramList = paramListToken.getText();
    CommonTree root = new CommonTree();

    int startIndex = paramListToken.getStartIndex();

    int i=0;
    while (i<paramList.length()) {
      CommonToken token = getParamListSubToken(paramListToken, paramList, i);
      root.addChild(new CommonTree(token));
      i += token.getText().length();
    }

    if (root.getChildCount() == 0) {
      return null;
    }
    return root;
  }

  private void throwOdexedInstructionException(IntStream input, String odexedInstruction)
      throws OdexedInstructionException {
    /*this has to be done in a separate method, otherwise java will complain about the
    auto-generated code in the rule after the throw not being reachable*/
    throw new OdexedInstructionException(input, odexedInstruction);
  }
}


smali_file
  scope
  {
    boolean hasClassSpec;
    boolean hasSuperSpec;
    boolean hasSourceSpec;
    List<CommonTree> classAnnotations;
  }
  @init
  { $smali_file::hasClassSpec = $smali_file::hasSuperSpec = $smali_file::hasSourceSpec = false;
    $smali_file::classAnnotations = new ArrayList<CommonTree>();
  }
  :
  ( {!$smali_file::hasClassSpec}?=> class_spec {$smali_file::hasClassSpec = true;}
  | {!$smali_file::hasSuperSpec}?=> super_spec {$smali_file::hasSuperSpec = true;}
  | implements_spec
  | {!$smali_file::hasSourceSpec}?=> source_spec {$smali_file::hasSourceSpec = true;}
  | method
  | field
  | annotation {$smali_file::classAnnotations.add($annotation.tree);}
  )+
  EOF
  {
    if (!$smali_file::hasClassSpec) {
      throw new SemanticException(input, "The file must contain a .class directive");
    }

    if (!$smali_file::hasSuperSpec) {
      if (!$class_spec.className.equals("Ljava/lang/Object;")) {
        throw new SemanticException(input, "The file must contain a .super directive");
      }
    }
  }
  -> ^(I_CLASS_DEF
       class_spec
       super_spec?
       implements_spec*
       source_spec?
       ^(I_METHODS method*) ^(I_FIELDS field*) {buildTree(I_ANNOTATIONS, "I_ANNOTATIONS", $smali_file::classAnnotations)});

class_spec returns[String className]
  : CLASS_DIRECTIVE access_list CLASS_DESCRIPTOR {$className = $CLASS_DESCRIPTOR.text;} -> CLASS_DESCRIPTOR access_list;

super_spec
  : SUPER_DIRECTIVE CLASS_DESCRIPTOR -> ^(I_SUPER[$start, "I_SUPER"] CLASS_DESCRIPTOR);

implements_spec
  : IMPLEMENTS_DIRECTIVE CLASS_DESCRIPTOR -> ^(I_IMPLEMENTS[$start, "I_IMPLEMENTS"] CLASS_DESCRIPTOR);

source_spec
  : SOURCE_DIRECTIVE STRING_LITERAL -> ^(I_SOURCE[$start, "I_SOURCE"] STRING_LITERAL);

access_list
  : ACCESS_SPEC* -> ^(I_ACCESS_LIST[$start,"I_ACCESS_LIST"] ACCESS_SPEC*);


/*When there are annotations immediately after a field definition, we don't know whether they are field annotations
or class annotations until we determine if there is an .end field directive. In either case, we still "consume" and parse
the annotations. If it turns out that they are field annotations, we include them in the I_FIELD AST. Otherwise, we
add them to the $smali_file::classAnnotations list*/
field
  @init {List<CommonTree> annotations = new ArrayList<CommonTree>();}
  : FIELD_DIRECTIVE access_list simple_name COLON nonvoid_type_descriptor (EQUAL literal)?
    ( ({input.LA(1) == ANNOTATION_DIRECTIVE}? annotation {annotations.add($annotation.tree);})*
      ( END_FIELD_DIRECTIVE
        -> ^(I_FIELD[$start, "I_FIELD"] simple_name access_list ^(I_FIELD_TYPE nonvoid_type_descriptor) ^(I_FIELD_INITIAL_VALUE literal)? ^(I_ANNOTATIONS annotation*))
      | /*epsilon*/ {$smali_file::classAnnotations.addAll(annotations);}
        -> ^(I_FIELD[$start, "I_FIELD"] simple_name access_list ^(I_FIELD_TYPE nonvoid_type_descriptor) ^(I_FIELD_INITIAL_VALUE literal)? ^(I_ANNOTATIONS))
      )
    );

method
  scope {int currentAddress;}
  : {$method::currentAddress = 0;}
    METHOD_DIRECTIVE access_list method_name method_prototype statements_and_directives
    END_METHOD_DIRECTIVE
    -> ^(I_METHOD[$start, "I_METHOD"] method_name method_prototype access_list statements_and_directives);

statements_and_directives
  scope
  {
    boolean hasRegistersDirective;
    List<CommonTree> packedSwitchDeclarations;
    List<CommonTree> sparseSwitchDeclarations;
    List<CommonTree> methodAnnotations;
  }
  : {
      $method::currentAddress = 0;
      $statements_and_directives::hasRegistersDirective = false;
      $statements_and_directives::packedSwitchDeclarations = new ArrayList<CommonTree>();
      $statements_and_directives::sparseSwitchDeclarations = new ArrayList<CommonTree>();
      $statements_and_directives::methodAnnotations = new ArrayList<CommonTree>();
    }
    ( instruction {$method::currentAddress += $instruction.size/2;}
    | registers_directive
    | label
    | catch_directive
    | catchall_directive
    | parameter_directive
    | ordered_debug_directive
    | annotation  {$statements_and_directives::methodAnnotations.add($annotation.tree);}
    )*
    -> registers_directive?
       ^(I_LABELS label*)
       {buildTree(I_PACKED_SWITCH_DECLARATIONS, "I_PACKED_SWITCH_DECLARATIONS", $statements_and_directives::packedSwitchDeclarations)}
       {buildTree(I_SPARSE_SWITCH_DECLARATIONS, "I_SPARSE_SWITCH_DECLARATIONS", $statements_and_directives::sparseSwitchDeclarations)}
       ^(I_STATEMENTS instruction*)
       ^(I_CATCHES catch_directive* catchall_directive*)
       ^(I_PARAMETERS parameter_directive*)
       ^(I_ORDERED_DEBUG_DIRECTIVES ordered_debug_directive*)
       {buildTree(I_ANNOTATIONS, "I_ANNOTATIONS", $statements_and_directives::methodAnnotations)};

registers_directive
  : (
      directive=REGISTERS_DIRECTIVE regCount=integral_literal -> ^(I_REGISTERS[$REGISTERS_DIRECTIVE, "I_REGISTERS"] $regCount)
    | directive=LOCALS_DIRECTIVE regCount2=integral_literal -> ^(I_LOCALS[$LOCALS_DIRECTIVE, "I_LOCALS"] $regCount2)
    )
    {
      if ($statements_and_directives::hasRegistersDirective) {
        throw new SemanticException(input, $directive, "There can only be a single .registers or .locals directive in a method");
      }
      $statements_and_directives::hasRegistersDirective=true;
    };

/*identifiers are much more general than most languages. Any of the below can either be
the indicated type OR an identifier, depending on the context*/
simple_name
  : SIMPLE_NAME
  | ACCESS_SPEC -> SIMPLE_NAME[$ACCESS_SPEC]
  | VERIFICATION_ERROR_TYPE -> SIMPLE_NAME[$VERIFICATION_ERROR_TYPE]
  | POSITIVE_INTEGER_LITERAL -> SIMPLE_NAME[$POSITIVE_INTEGER_LITERAL]
  | NEGATIVE_INTEGER_LITERAL -> SIMPLE_NAME[$NEGATIVE_INTEGER_LITERAL]
  | FLOAT_LITERAL_OR_ID -> SIMPLE_NAME[$FLOAT_LITERAL_OR_ID]
  | DOUBLE_LITERAL_OR_ID -> SIMPLE_NAME[$DOUBLE_LITERAL_OR_ID]
  | BOOL_LITERAL -> SIMPLE_NAME[$BOOL_LITERAL]
  | NULL_LITERAL -> SIMPLE_NAME[$NULL_LITERAL]
  | REGISTER -> SIMPLE_NAME[$REGISTER]
  | PARAM_LIST_OR_ID -> SIMPLE_NAME[$PARAM_LIST_OR_ID]
  | PRIMITIVE_TYPE -> SIMPLE_NAME[$PRIMITIVE_TYPE]
  | VOID_TYPE -> SIMPLE_NAME[$VOID_TYPE]
  | ANNOTATION_VISIBILITY -> SIMPLE_NAME[$ANNOTATION_VISIBILITY]
  | INSTRUCTION_FORMAT10t -> SIMPLE_NAME[$INSTRUCTION_FORMAT10t]
  | INSTRUCTION_FORMAT10x -> SIMPLE_NAME[$INSTRUCTION_FORMAT10x]
  | INSTRUCTION_FORMAT10x_ODEX -> SIMPLE_NAME[$INSTRUCTION_FORMAT10x_ODEX]
  | INSTRUCTION_FORMAT11x -> SIMPLE_NAME[$INSTRUCTION_FORMAT11x]
  | INSTRUCTION_FORMAT12x_OR_ID -> SIMPLE_NAME[$INSTRUCTION_FORMAT12x_OR_ID]
  | INSTRUCTION_FORMAT21c_FIELD -> SIMPLE_NAME[$INSTRUCTION_FORMAT21c_FIELD]
  | INSTRUCTION_FORMAT21c_FIELD_ODEX -> SIMPLE_NAME[$INSTRUCTION_FORMAT21c_FIELD_ODEX]
  | INSTRUCTION_FORMAT21c_STRING -> SIMPLE_NAME[$INSTRUCTION_FORMAT21c_STRING]
  | INSTRUCTION_FORMAT21c_TYPE -> SIMPLE_NAME[$INSTRUCTION_FORMAT21c_TYPE]
  | INSTRUCTION_FORMAT21t -> SIMPLE_NAME[$INSTRUCTION_FORMAT21t]
  | INSTRUCTION_FORMAT22c_FIELD -> SIMPLE_NAME[$INSTRUCTION_FORMAT22c_FIELD]
  | INSTRUCTION_FORMAT22c_FIELD_ODEX -> SIMPLE_NAME[$INSTRUCTION_FORMAT22c_FIELD_ODEX]
  | INSTRUCTION_FORMAT22c_TYPE -> SIMPLE_NAME[$INSTRUCTION_FORMAT22c_TYPE]
  | INSTRUCTION_FORMAT22cs_FIELD -> SIMPLE_NAME[$INSTRUCTION_FORMAT22cs_FIELD]
  | INSTRUCTION_FORMAT22s_OR_ID -> SIMPLE_NAME[$INSTRUCTION_FORMAT22s_OR_ID]
  | INSTRUCTION_FORMAT22t -> SIMPLE_NAME[$INSTRUCTION_FORMAT22t]
  | INSTRUCTION_FORMAT23x -> SIMPLE_NAME[$INSTRUCTION_FORMAT23x]
  | INSTRUCTION_FORMAT31i_OR_ID -> SIMPLE_NAME[$INSTRUCTION_FORMAT31i_OR_ID]
  | INSTRUCTION_FORMAT31t -> SIMPLE_NAME[$INSTRUCTION_FORMAT31t]
  | INSTRUCTION_FORMAT35c_METHOD -> SIMPLE_NAME[$INSTRUCTION_FORMAT35c_METHOD]
  | INSTRUCTION_FORMAT35c_METHOD_ODEX -> SIMPLE_NAME[$INSTRUCTION_FORMAT35c_METHOD_ODEX]
  | INSTRUCTION_FORMAT35c_TYPE -> SIMPLE_NAME[$INSTRUCTION_FORMAT35c_TYPE]
  | INSTRUCTION_FORMAT35mi_METHOD -> SIMPLE_NAME[$INSTRUCTION_FORMAT35mi_METHOD]
  | INSTRUCTION_FORMAT35ms_METHOD -> SIMPLE_NAME[$INSTRUCTION_FORMAT35ms_METHOD]
  | INSTRUCTION_FORMAT51l -> SIMPLE_NAME[$INSTRUCTION_FORMAT51l];

method_name
  : simple_name
  | METHOD_NAME -> SIMPLE_NAME[$METHOD_NAME];

method_prototype
  : OPEN_PAREN param_list CLOSE_PAREN type_descriptor
    -> ^(I_METHOD_PROTOTYPE[$start, "I_METHOD_PROTOTYPE"] ^(I_METHOD_RETURN_TYPE type_descriptor) param_list?);

param_list
  : PARAM_LIST -> { parseParamList((CommonToken)$PARAM_LIST) }
  | PARAM_LIST_OR_ID -> { parseParamList((CommonToken)$PARAM_LIST_OR_ID) }
  | nonvoid_type_descriptor*;

type_descriptor
  : VOID_TYPE
  | PRIMITIVE_TYPE
  | CLASS_DESCRIPTOR
  | ARRAY_DESCRIPTOR;

nonvoid_type_descriptor
  : PRIMITIVE_TYPE
  | CLASS_DESCRIPTOR
  | ARRAY_DESCRIPTOR;

reference_type_descriptor
  : CLASS_DESCRIPTOR
  | ARRAY_DESCRIPTOR;

integer_literal
  : POSITIVE_INTEGER_LITERAL -> INTEGER_LITERAL[$POSITIVE_INTEGER_LITERAL]
  | NEGATIVE_INTEGER_LITERAL -> INTEGER_LITERAL[$NEGATIVE_INTEGER_LITERAL];

float_literal
  : FLOAT_LITERAL_OR_ID -> FLOAT_LITERAL[$FLOAT_LITERAL_OR_ID]
  | FLOAT_LITERAL;

double_literal
  : DOUBLE_LITERAL_OR_ID -> DOUBLE_LITERAL[$DOUBLE_LITERAL_OR_ID]
  | DOUBLE_LITERAL;

literal
  : LONG_LITERAL
  | integer_literal
  | SHORT_LITERAL
  | BYTE_LITERAL
  | float_literal
  | double_literal
  | CHAR_LITERAL
  | STRING_LITERAL
  | BOOL_LITERAL
  | NULL_LITERAL
  | array_literal
  | subannotation
  | type_field_method_literal
  | enum_literal;

integral_literal
  : LONG_LITERAL
  | integer_literal
  | SHORT_LITERAL
  | CHAR_LITERAL
  | BYTE_LITERAL;

fixed_32bit_literal
  : LONG_LITERAL
  | integer_literal
  | SHORT_LITERAL
  | BYTE_LITERAL
  | float_literal
  | CHAR_LITERAL
  | BOOL_LITERAL;

fixed_literal returns[int size]
  : integer_literal {$size = 4;}
  | LONG_LITERAL {$size = 8;}
  | SHORT_LITERAL {$size = 2;}
  | BYTE_LITERAL {$size = 1;}
  | float_literal {$size = 4;}
  | double_literal {$size = 8;}
  | CHAR_LITERAL {$size = 2;}
  | BOOL_LITERAL {$size = 1;};

array_literal
  : OPEN_BRACE (literal (COMMA literal)* | ) CLOSE_BRACE
    -> ^(I_ENCODED_ARRAY[$start, "I_ENCODED_ARRAY"] literal*);

annotation_element
  : simple_name EQUAL literal
    -> ^(I_ANNOTATION_ELEMENT[$start, "I_ANNOTATION_ELEMENT"] simple_name literal);

annotation
  : ANNOTATION_DIRECTIVE ANNOTATION_VISIBILITY CLASS_DESCRIPTOR
    annotation_element* END_ANNOTATION_DIRECTIVE
    -> ^(I_ANNOTATION[$start, "I_ANNOTATION"] ANNOTATION_VISIBILITY ^(I_SUBANNOTATION[$start, "I_SUBANNOTATION"] CLASS_DESCRIPTOR annotation_element*));

subannotation
  : SUBANNOTATION_DIRECTIVE CLASS_DESCRIPTOR annotation_element* END_SUBANNOTATION_DIRECTIVE
    -> ^(I_SUBANNOTATION[$start, "I_SUBANNOTATION"] CLASS_DESCRIPTOR annotation_element*);

enum_literal
  : ENUM_DIRECTIVE reference_type_descriptor ARROW simple_name COLON reference_type_descriptor
  -> ^(I_ENCODED_ENUM reference_type_descriptor simple_name reference_type_descriptor);

type_field_method_literal
  : reference_type_descriptor
    ( ARROW
      ( simple_name COLON nonvoid_type_descriptor -> ^(I_ENCODED_FIELD reference_type_descriptor simple_name nonvoid_type_descriptor)
      | method_name method_prototype -> ^(I_ENCODED_METHOD reference_type_descriptor method_name method_prototype)
      )
    | -> reference_type_descriptor
    )
  | PRIMITIVE_TYPE
  | VOID_TYPE;

fully_qualified_method
  : reference_type_descriptor ARROW method_name method_prototype
  -> reference_type_descriptor method_name method_prototype;

fully_qualified_field
  : reference_type_descriptor ARROW simple_name COLON nonvoid_type_descriptor
  -> reference_type_descriptor simple_name nonvoid_type_descriptor;

label
  : COLON simple_name -> ^(I_LABEL[$COLON, "I_LABEL"] simple_name I_ADDRESS[$start, Integer.toString($method::currentAddress)]);

label_ref_or_offset
  : COLON simple_name -> simple_name
  | OFFSET
  | NEGATIVE_INTEGER_LITERAL -> OFFSET[$NEGATIVE_INTEGER_LITERAL];

register_list
  : REGISTER (COMMA REGISTER)* -> ^(I_REGISTER_LIST[$start, "I_REGISTER_LIST"] REGISTER*)
  | ->^(I_REGISTER_LIST[$start, "I_REGISTER_LIST"]);

register_range
  : (startreg=REGISTER (DOTDOT endreg=REGISTER)?)? -> ^(I_REGISTER_RANGE[$start, "I_REGISTER_RANGE"] $startreg? $endreg?);

verification_error_reference
  : CLASS_DESCRIPTOR | fully_qualified_field | fully_qualified_method;

catch_directive
  : CATCH_DIRECTIVE nonvoid_type_descriptor OPEN_BRACE from=label_ref_or_offset DOTDOT to=label_ref_or_offset CLOSE_BRACE using=label_ref_or_offset
    -> ^(I_CATCH[$start, "I_CATCH"] I_ADDRESS[$start, Integer.toString($method::currentAddress)] nonvoid_type_descriptor $from $to $using);

catchall_directive
  : CATCHALL_DIRECTIVE OPEN_BRACE from=label_ref_or_offset DOTDOT to=label_ref_or_offset CLOSE_BRACE using=label_ref_or_offset
    -> ^(I_CATCHALL[$start, "I_CATCHALL"] I_ADDRESS[$start, Integer.toString($method::currentAddress)] $from $to $using);

/*When there are annotations immediately after a parameter definition, we don't know whether they are parameter annotations
or method annotations until we determine if there is an .end parameter directive. In either case, we still "consume" and parse
the annotations. If it turns out that they are parameter annotations, we include them in the I_PARAMETER AST. Otherwise, we
add them to the $statements_and_directives::methodAnnotations list*/
parameter_directive
  @init {List<CommonTree> annotations = new ArrayList<CommonTree>();}
  : PARAMETER_DIRECTIVE
    STRING_LITERAL?
    ({input.LA(1) == ANNOTATION_DIRECTIVE}? annotation {annotations.add($annotation.tree);})*

    ( END_PARAMETER_DIRECTIVE
      -> ^(I_PARAMETER[$start, "I_PARAMETER"] STRING_LITERAL? ^(I_ANNOTATIONS annotation*))
    | /*epsilon*/ {$statements_and_directives::methodAnnotations.addAll(annotations);}
      -> ^(I_PARAMETER[$start, "I_PARAMETER"] STRING_LITERAL? ^(I_ANNOTATIONS))
    );

ordered_debug_directive
  : line_directive
  | local_directive
  | end_local_directive
  | restart_local_directive
  | prologue_directive
  | epilogue_directive
  | source_directive;

line_directive
  : LINE_DIRECTIVE integral_literal
    -> ^(I_LINE integral_literal I_ADDRESS[$start, Integer.toString($method::currentAddress)]);

local_directive
  : LOCAL_DIRECTIVE REGISTER COMMA simple_name COLON nonvoid_type_descriptor (COMMA STRING_LITERAL)?
    -> ^(I_LOCAL[$start, "I_LOCAL"] REGISTER simple_name nonvoid_type_descriptor STRING_LITERAL? I_ADDRESS[$start, Integer.toString($method::currentAddress)]);

end_local_directive
  : END_LOCAL_DIRECTIVE REGISTER
    -> ^(I_END_LOCAL[$start, "I_END_LOCAL"] REGISTER I_ADDRESS[$start, Integer.toString($method::currentAddress)]);

restart_local_directive
  : RESTART_LOCAL_DIRECTIVE REGISTER
    -> ^(I_RESTART_LOCAL[$start, "I_RESTART_LOCAL"] REGISTER I_ADDRESS[$start, Integer.toString($method::currentAddress)]);

prologue_directive
  : PROLOGUE_DIRECTIVE
    -> ^(I_PROLOGUE[$start, "I_PROLOGUE"] I_ADDRESS[$start, Integer.toString($method::currentAddress)]);

epilogue_directive
  : EPILOGUE_DIRECTIVE
    -> ^(I_EPILOGUE[$start, "I_EPILOGUE"] I_ADDRESS[$start, Integer.toString($method::currentAddress)]);

source_directive
  : SOURCE_DIRECTIVE STRING_LITERAL
    -> ^(I_SOURCE[$start, "I_SOURCE"] STRING_LITERAL I_ADDRESS[$start, Integer.toString($method::currentAddress)]);

instruction_format12x
  : INSTRUCTION_FORMAT12x
  | INSTRUCTION_FORMAT12x_OR_ID -> INSTRUCTION_FORMAT12x[$INSTRUCTION_FORMAT12x_OR_ID];

instruction_format22s
  : INSTRUCTION_FORMAT22s
  | INSTRUCTION_FORMAT22s_OR_ID -> INSTRUCTION_FORMAT22s[$INSTRUCTION_FORMAT22s_OR_ID];

instruction_format31i
  : INSTRUCTION_FORMAT31i
  | INSTRUCTION_FORMAT31i_OR_ID -> INSTRUCTION_FORMAT31i[$INSTRUCTION_FORMAT31i_OR_ID];



instruction returns [int size]
  : insn_format10t { $size = $insn_format10t.size; }
  | insn_format10x { $size = $insn_format10x.size; }
  | insn_format10x_odex { $size = $insn_format10x_odex.size; }
  | insn_format11n { $size = $insn_format11n.size; }
  | insn_format11x { $size = $insn_format11x.size; }
  | insn_format12x { $size = $insn_format12x.size; }
  | insn_format20bc { $size = $insn_format20bc.size; }
  | insn_format20t { $size = $insn_format20t.size; }
  | insn_format21c_field { $size = $insn_format21c_field.size; }
  | insn_format21c_field_odex { $size = $insn_format21c_field_odex.size; }
  | insn_format21c_string { $size = $insn_format21c_string.size; }
  | insn_format21c_type { $size = $insn_format21c_type.size; }
  | insn_format21h { $size = $insn_format21h.size; }
  | insn_format21s { $size = $insn_format21s.size; }
  | insn_format21t { $size = $insn_format21t.size; }
  | insn_format22b { $size = $insn_format22b.size; }
  | insn_format22c_field { $size = $insn_format22c_field.size; }
  | insn_format22c_field_odex { $size = $insn_format22c_field_odex.size; }
  | insn_format22c_type { $size = $insn_format22c_type.size; }
  | insn_format22cs_field { $size = $insn_format22cs_field.size; }
  | insn_format22s { $size = $insn_format22s.size; }
  | insn_format22t { $size = $insn_format22t.size; }
  | insn_format22x { $size = $insn_format22x.size; }
  | insn_format23x { $size = $insn_format23x.size; }
  | insn_format30t { $size = $insn_format30t.size; }
  | insn_format31c { $size = $insn_format31c.size; }
  | insn_format31i { $size = $insn_format31i.size; }
  | insn_format31t { $size = $insn_format31t.size; }
  | insn_format32x { $size = $insn_format32x.size; }
  | insn_format35c_method { $size = $insn_format35c_method.size; }
  | insn_format35c_type { $size = $insn_format35c_type.size; }
  | insn_format35c_method_odex { $size = $insn_format35c_method_odex.size; }
  | insn_format35mi_method { $size = $insn_format35mi_method.size; }
  | insn_format35ms_method { $size = $insn_format35ms_method.size; }
  | insn_format3rc_method { $size = $insn_format3rc_method.size; }
  | insn_format3rc_method_odex { $size = $insn_format3rc_method_odex.size; }
  | insn_format3rc_type { $size = $insn_format3rc_type.size; }
  | insn_format3rmi_method { $size = $insn_format3rmi_method.size; }
  | insn_format3rms_method { $size = $insn_format3rms_method.size; }
  | insn_format41c_type { $size = $insn_format41c_type.size; }
  | insn_format41c_field { $size = $insn_format41c_field.size; }
  | insn_format41c_field_odex { $size = $insn_format41c_field_odex.size; }
  | insn_format51l { $size = $insn_format51l.size; }
  | insn_format52c_type { $size = $insn_format52c_type.size; }
  |  insn_format52c_field { $size = $insn_format52c_field.size; }
  |  insn_format52c_field_odex { $size = $insn_format52c_field_odex.size; }
  |  insn_format5rc_method { $size = $insn_format5rc_method.size; }
  |  insn_format5rc_method_odex { $size = $insn_format5rc_method_odex.size; }
  |  insn_format5rc_type { $size = $insn_format5rc_type.size; }
  | insn_array_data_directive { $size = $insn_array_data_directive.size; }
  | insn_packed_switch_directive { $size = $insn_packed_switch_directive.size; }
  | insn_sparse_switch_directive { $size = $insn_sparse_switch_directive.size; };

insn_format10t returns [int size]
  : //e.g. goto endloop:
    //e.g. goto +3
    INSTRUCTION_FORMAT10t label_ref_or_offset {$size = Format.Format10t.size;}
    -> ^(I_STATEMENT_FORMAT10t[$start, "I_STATEMENT_FORMAT10t"] INSTRUCTION_FORMAT10t label_ref_or_offset);

insn_format10x returns [int size]
  : //e.g. return-void
    INSTRUCTION_FORMAT10x {$size = Format.Format10x.size;}
    -> ^(I_STATEMENT_FORMAT10x[$start, "I_STATEMENT_FORMAT10x"] INSTRUCTION_FORMAT10x);

insn_format10x_odex returns [int size]
  : //e.g. return-void-barrier
    INSTRUCTION_FORMAT10x_ODEX {$size = Format.Format10x.size;}
    {
      throwOdexedInstructionException(input, $INSTRUCTION_FORMAT10x_ODEX.text);
    };

insn_format11n returns [int size]
  : //e.g. const/4 v0, 5
    INSTRUCTION_FORMAT11n REGISTER COMMA integral_literal {$size = Format.Format11n.size;}
    -> ^(I_STATEMENT_FORMAT11n[$start, "I_STATEMENT_FORMAT11n"] INSTRUCTION_FORMAT11n REGISTER integral_literal);

insn_format11x returns [int size]
  : //e.g. move-result-object v1
    INSTRUCTION_FORMAT11x REGISTER {$size = Format.Format11x.size;}
    -> ^(I_STATEMENT_FORMAT11x[$start, "I_STATEMENT_FORMAT11x"] INSTRUCTION_FORMAT11x REGISTER);

insn_format12x returns [int size]
  : //e.g. move v1 v2
    instruction_format12x REGISTER COMMA REGISTER {$size = Format.Format12x.size;}
    -> ^(I_STATEMENT_FORMAT12x[$start, "I_STATEMENT_FORMAT12x"] instruction_format12x REGISTER REGISTER);

insn_format20bc returns [int size]
  : //e.g. throw-verification-error generic-error, Lsome/class;
    INSTRUCTION_FORMAT20bc VERIFICATION_ERROR_TYPE COMMA verification_error_reference {$size += Format.Format20bc.size;}
    {
      if (!allowOdex || Opcode.getOpcodeByName($INSTRUCTION_FORMAT20bc.text) == null || apiLevel >= 14) {
        throwOdexedInstructionException(input, $INSTRUCTION_FORMAT20bc.text);
      }
    }
    -> ^(I_STATEMENT_FORMAT20bc INSTRUCTION_FORMAT20bc VERIFICATION_ERROR_TYPE verification_error_reference);
    //TODO: check if dalvik has a jumbo version of throw-verification-error

insn_format20t returns [int size]
  : //e.g. goto/16 endloop:
    INSTRUCTION_FORMAT20t label_ref_or_offset {$size = Format.Format20t.size;}
    -> ^(I_STATEMENT_FORMAT20t[$start, "I_STATEMENT_FORMAT20t"] INSTRUCTION_FORMAT20t label_ref_or_offset);

insn_format21c_field returns [int size]
  : //e.g. sget-object v0, java/lang/System/out LJava/io/PrintStream;
    INSTRUCTION_FORMAT21c_FIELD REGISTER COMMA fully_qualified_field {$size = Format.Format21c.size;}
    -> ^(I_STATEMENT_FORMAT21c_FIELD[$start, "I_STATEMENT_FORMAT21c_FIELD"] INSTRUCTION_FORMAT21c_FIELD REGISTER fully_qualified_field);

insn_format21c_field_odex returns [int size]
  : //e.g. sget-object-volatile v0, java/lang/System/out LJava/io/PrintStream;
    INSTRUCTION_FORMAT21c_FIELD_ODEX REGISTER COMMA fully_qualified_field {$size = Format.Format21c.size;}
    {
      if (!allowOdex || Opcode.getOpcodeByName($INSTRUCTION_FORMAT21c_FIELD_ODEX.text) == null || apiLevel >= 14) {
        throwOdexedInstructionException(input, $INSTRUCTION_FORMAT21c_FIELD_ODEX.text);
      }
    }
    -> ^(I_STATEMENT_FORMAT21c_FIELD[$start, "I_STATEMENT_FORMAT21c_FIELD"] INSTRUCTION_FORMAT21c_FIELD_ODEX REGISTER fully_qualified_field);

insn_format21c_string returns [int size]
  : //e.g. const-string v1, "Hello World!"
    INSTRUCTION_FORMAT21c_STRING REGISTER COMMA STRING_LITERAL {$size = Format.Format21c.size;}
    -> ^(I_STATEMENT_FORMAT21c_STRING[$start, "I_STATEMENT_FORMAT21c_STRING"] INSTRUCTION_FORMAT21c_STRING REGISTER STRING_LITERAL);

insn_format21c_type returns [int size]
  : //e.g. const-class v2, Lorg/jf/HelloWorld2/HelloWorld2;
    INSTRUCTION_FORMAT21c_TYPE REGISTER COMMA reference_type_descriptor {$size = Format.Format21c.size;}
    -> ^(I_STATEMENT_FORMAT21c_TYPE[$start, "I_STATEMENT_FORMAT21c"] INSTRUCTION_FORMAT21c_TYPE REGISTER reference_type_descriptor);

insn_format21h returns [int size]
  : //e.g. const/high16 v1, 1234
    INSTRUCTION_FORMAT21h REGISTER COMMA integral_literal {$size = Format.Format21h.size;}
    -> ^(I_STATEMENT_FORMAT21h[$start, "I_STATEMENT_FORMAT21h"] INSTRUCTION_FORMAT21h REGISTER integral_literal);

insn_format21s returns [int size]
  : //e.g. const/16 v1, 1234
    INSTRUCTION_FORMAT21s REGISTER COMMA integral_literal {$size = Format.Format21s.size;}
    -> ^(I_STATEMENT_FORMAT21s[$start, "I_STATEMENT_FORMAT21s"] INSTRUCTION_FORMAT21s REGISTER integral_literal);

insn_format21t returns [int size]
  : //e.g. if-eqz v0, endloop:
    INSTRUCTION_FORMAT21t REGISTER COMMA (label_ref_or_offset) {$size = Format.Format21t.size;}
    -> ^(I_STATEMENT_FORMAT21t[$start, "I_STATEMENT_FORMAT21t"] INSTRUCTION_FORMAT21t REGISTER label_ref_or_offset);

insn_format22b returns [int size]
  : //e.g. add-int v0, v1, 123
    INSTRUCTION_FORMAT22b REGISTER COMMA REGISTER COMMA integral_literal {$size = Format.Format22b.size;}
    -> ^(I_STATEMENT_FORMAT22b[$start, "I_STATEMENT_FORMAT22b"] INSTRUCTION_FORMAT22b REGISTER REGISTER integral_literal);

insn_format22c_field returns [int size]
  : //e.g. iput-object v1, v0 org/jf/HelloWorld2/HelloWorld2.helloWorld Ljava/lang/String;
    INSTRUCTION_FORMAT22c_FIELD REGISTER COMMA REGISTER COMMA fully_qualified_field {$size = Format.Format22c.size;}
    -> ^(I_STATEMENT_FORMAT22c_FIELD[$start, "I_STATEMENT_FORMAT22c_FIELD"] INSTRUCTION_FORMAT22c_FIELD REGISTER REGISTER fully_qualified_field);

insn_format22c_field_odex returns [int size]
  : //e.g. iput-object-volatile v1, v0 org/jf/HelloWorld2/HelloWorld2.helloWorld Ljava/lang/String;
    INSTRUCTION_FORMAT22c_FIELD_ODEX REGISTER COMMA REGISTER COMMA fully_qualified_field {$size = Format.Format22c.size;}
    {
      if (!allowOdex || Opcode.getOpcodeByName($INSTRUCTION_FORMAT22c_FIELD_ODEX.text) == null || apiLevel >= 14) {
        throwOdexedInstructionException(input, $INSTRUCTION_FORMAT22c_FIELD_ODEX.text);
      }
    }
    -> ^(I_STATEMENT_FORMAT22c_FIELD[$start, "I_STATEMENT_FORMAT22c_FIELD"] INSTRUCTION_FORMAT22c_FIELD_ODEX REGISTER REGISTER fully_qualified_field);

insn_format22c_type returns [int size]
  : //e.g. instance-of v0, v1, Ljava/lang/String;
    INSTRUCTION_FORMAT22c_TYPE REGISTER COMMA REGISTER COMMA nonvoid_type_descriptor {$size = Format.Format22c.size;}
    -> ^(I_STATEMENT_FORMAT22c_TYPE[$start, "I_STATEMENT_FORMAT22c_TYPE"] INSTRUCTION_FORMAT22c_TYPE REGISTER REGISTER nonvoid_type_descriptor);

insn_format22cs_field returns [int size]
  : //e.g. iget-quick v0, v1, field@0xc
    INSTRUCTION_FORMAT22cs_FIELD REGISTER COMMA REGISTER COMMA FIELD_OFFSET
    {
      throwOdexedInstructionException(input, $INSTRUCTION_FORMAT22cs_FIELD.text);
    };

insn_format22s returns [int size]
  : //e.g. add-int/lit16 v0, v1, 12345
    instruction_format22s REGISTER COMMA REGISTER COMMA integral_literal {$size = Format.Format22s.size;}
    -> ^(I_STATEMENT_FORMAT22s[$start, "I_STATEMENT_FORMAT22s"] instruction_format22s REGISTER REGISTER integral_literal);

insn_format22t returns [int size]
  : //e.g. if-eq v0, v1, endloop:
    INSTRUCTION_FORMAT22t REGISTER COMMA REGISTER COMMA label_ref_or_offset {$size = Format.Format22t.size;}
    -> ^(I_STATEMENT_FORMAT22t[$start, "I_STATEMENT_FFORMAT22t"] INSTRUCTION_FORMAT22t REGISTER REGISTER label_ref_or_offset);

insn_format22x returns [int size]
  : //e.g. move/from16 v1, v1234
    INSTRUCTION_FORMAT22x REGISTER COMMA REGISTER {$size = Format.Format22x.size;}
    -> ^(I_STATEMENT_FORMAT22x[$start, "I_STATEMENT_FORMAT22x"] INSTRUCTION_FORMAT22x REGISTER REGISTER);

insn_format23x returns [int size]
  : //e.g. add-int v1, v2, v3
    INSTRUCTION_FORMAT23x REGISTER COMMA REGISTER COMMA REGISTER {$size = Format.Format23x.size;}
    -> ^(I_STATEMENT_FORMAT23x[$start, "I_STATEMENT_FORMAT23x"] INSTRUCTION_FORMAT23x REGISTER REGISTER REGISTER);

insn_format30t returns [int size]
  : //e.g. goto/32 endloop:
    INSTRUCTION_FORMAT30t label_ref_or_offset {$size = Format.Format30t.size;}
    -> ^(I_STATEMENT_FORMAT30t[$start, "I_STATEMENT_FORMAT30t"] INSTRUCTION_FORMAT30t label_ref_or_offset);

insn_format31c returns [int size]
  : //e.g. const-string/jumbo v1 "Hello World!"
    INSTRUCTION_FORMAT31c REGISTER COMMA STRING_LITERAL {$size = Format.Format31c.size;}
    ->^(I_STATEMENT_FORMAT31c[$start, "I_STATEMENT_FORMAT31c"] INSTRUCTION_FORMAT31c REGISTER STRING_LITERAL);

insn_format31i returns [int size]
  : //e.g. const v0, 123456
    instruction_format31i REGISTER COMMA fixed_32bit_literal {$size = Format.Format31i.size;}
    -> ^(I_STATEMENT_FORMAT31i[$start, "I_STATEMENT_FORMAT31i"] instruction_format31i REGISTER fixed_32bit_literal);

insn_format31t returns [int size]
  : //e.g. fill-array-data v0, ArrayData:
    INSTRUCTION_FORMAT31t REGISTER COMMA label_ref_or_offset {$size = Format.Format31t.size;}
    {
      if ($INSTRUCTION_FORMAT31t.text.equals("packed-switch")) {
        CommonTree root = new CommonTree(new CommonToken(I_PACKED_SWITCH_DECLARATION, "I_PACKED_SWITCH_DECLARATION"));
        CommonTree address = new CommonTree(new CommonToken(I_ADDRESS, Integer.toString($method::currentAddress)));
        root.addChild(address);
        root.addChild($label_ref_or_offset.tree.dupNode());
        $statements_and_directives::packedSwitchDeclarations.add(root);
      } else if ($INSTRUCTION_FORMAT31t.text.equals("sparse-switch")) {
        CommonTree root = new CommonTree(new CommonToken(I_SPARSE_SWITCH_DECLARATION, "I_SPARSE_SWITCH_DECLARATION"));
        CommonTree address = new CommonTree(new CommonToken(I_ADDRESS, Integer.toString($method::currentAddress)));
        root.addChild(address);
        root.addChild($label_ref_or_offset.tree.dupNode());
        $statements_and_directives::sparseSwitchDeclarations.add(root);
      }
    }
    -> ^(I_STATEMENT_FORMAT31t[$start, "I_STATEMENT_FORMAT31t"] INSTRUCTION_FORMAT31t REGISTER label_ref_or_offset);

insn_format32x returns [int size]
  : //e.g. move/16 v4567, v1234
    INSTRUCTION_FORMAT32x REGISTER COMMA REGISTER {$size = Format.Format32x.size;}
    -> ^(I_STATEMENT_FORMAT32x[$start, "I_STATEMENT_FORMAT32x"] INSTRUCTION_FORMAT32x REGISTER REGISTER);

insn_format35c_method returns [int size]
  : //e.g. invoke-virtual {v0,v1} java/io/PrintStream/print(Ljava/lang/Stream;)V
    INSTRUCTION_FORMAT35c_METHOD OPEN_BRACE register_list CLOSE_BRACE COMMA fully_qualified_method {$size = Format.Format35c.size;}
    -> ^(I_STATEMENT_FORMAT35c_METHOD[$start, "I_STATEMENT_FORMAT35c_METHOD"] INSTRUCTION_FORMAT35c_METHOD register_list fully_qualified_method);

insn_format35c_type returns [int size]
  : //e.g. filled-new-array {v0,v1}, I
    INSTRUCTION_FORMAT35c_TYPE OPEN_BRACE register_list CLOSE_BRACE COMMA nonvoid_type_descriptor {$size = Format.Format35c.size;}
    -> ^(I_STATEMENT_FORMAT35c_TYPE[$start, "I_STATEMENT_FORMAT35c_TYPE"] INSTRUCTION_FORMAT35c_TYPE register_list nonvoid_type_descriptor);

insn_format35c_method_odex returns [int size]
  : //e.g. invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    INSTRUCTION_FORMAT35c_METHOD_ODEX OPEN_BRACE register_list CLOSE_BRACE COMMA fully_qualified_method
    {
      throwOdexedInstructionException(input, $INSTRUCTION_FORMAT35c_METHOD_ODEX.text);
    };

insn_format35mi_method returns [int size]
  : //e.g. execute-inline {v0, v1}, inline@0x4
    INSTRUCTION_FORMAT35mi_METHOD OPEN_BRACE register_list CLOSE_BRACE COMMA INLINE_INDEX
    {
      throwOdexedInstructionException(input, $INSTRUCTION_FORMAT35mi_METHOD.text);
    };

insn_format35ms_method returns [int size]
  : //e.g. invoke-virtual-quick {v0, v1}, vtable@0x4
    INSTRUCTION_FORMAT35ms_METHOD OPEN_BRACE register_list CLOSE_BRACE COMMA VTABLE_INDEX
    {
      throwOdexedInstructionException(input, $INSTRUCTION_FORMAT35ms_METHOD.text);
    };

insn_format3rc_method returns [int size]
  : //e.g. invoke-virtual/range {v25..v26}, java/lang/StringBuilder/append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    INSTRUCTION_FORMAT3rc_METHOD OPEN_BRACE register_range CLOSE_BRACE COMMA fully_qualified_method {$size = Format.Format3rc.size;}
    -> ^(I_STATEMENT_FORMAT3rc_METHOD[$start, "I_STATEMENT_FORMAT3rc_METHOD"] INSTRUCTION_FORMAT3rc_METHOD register_range fully_qualified_method);

insn_format3rc_method_odex returns [int size]
  : //e.g. invoke-object-init/range {p0}, Ljava/lang/Object;-><init>()V
    INSTRUCTION_FORMAT3rc_METHOD_ODEX OPEN_BRACE register_list CLOSE_BRACE COMMA fully_qualified_method
    {
      throwOdexedInstructionException(input, $INSTRUCTION_FORMAT3rc_METHOD_ODEX.text);
    };

insn_format3rc_type returns [int size]
  : //e.g. filled-new-array/range {v0..v6}, I
    INSTRUCTION_FORMAT3rc_TYPE OPEN_BRACE register_range CLOSE_BRACE COMMA nonvoid_type_descriptor {$size = Format.Format3rc.size;}
    -> ^(I_STATEMENT_FORMAT3rc_TYPE[$start, "I_STATEMENT_FORMAT3rc_TYPE"] INSTRUCTION_FORMAT3rc_TYPE register_range nonvoid_type_descriptor);

insn_format3rmi_method returns [int size]
  : //e.g. execute-inline/range {v0 .. v10}, inline@0x14
    INSTRUCTION_FORMAT3rmi_METHOD OPEN_BRACE register_range CLOSE_BRACE COMMA INLINE_INDEX
    {
      throwOdexedInstructionException(input, $INSTRUCTION_FORMAT3rmi_METHOD.text);
    };

insn_format3rms_method returns [int size]
  : //e.g. invoke-virtual-quick/range {v0 .. v10}, vtable@0x14
    INSTRUCTION_FORMAT3rms_METHOD OPEN_BRACE register_range CLOSE_BRACE COMMA VTABLE_INDEX
    {
      throwOdexedInstructionException(input, $INSTRUCTION_FORMAT3rms_METHOD.text);
    };

insn_format41c_type returns [int size]
  : //e.g. const-class/jumbo v2, Lorg/jf/HelloWorld2/HelloWorld2;
    INSTRUCTION_FORMAT41c_TYPE REGISTER COMMA reference_type_descriptor {$size = Format.Format41c.size;}
    -> ^(I_STATEMENT_FORMAT41c_TYPE[$start, "I_STATEMENT_FORMAT41c"] INSTRUCTION_FORMAT41c_TYPE REGISTER reference_type_descriptor);

insn_format41c_field returns [int size]
  : //e.g. sget-object/jumbo v0, Ljava/lang/System;->out:Ljava/io/PrintStream;
    INSTRUCTION_FORMAT41c_FIELD REGISTER COMMA fully_qualified_field {$size = Format.Format41c.size;}
    -> ^(I_STATEMENT_FORMAT41c_FIELD[$start, "I_STATEMENT_FORMAT41c_FIELD"] INSTRUCTION_FORMAT41c_FIELD REGISTER fully_qualified_field);

insn_format41c_field_odex returns [int size]
  : //e.g. sget-object-volatile/jumbo v0, Ljava/lang/System;->out:Ljava/io/PrintStream;
    INSTRUCTION_FORMAT41c_FIELD_ODEX REGISTER COMMA fully_qualified_field {$size = Format.Format41c.size;}
    {
      throwOdexedInstructionException(input, $INSTRUCTION_FORMAT41c_FIELD_ODEX.text);
    };

insn_format51l returns [int size]
  : //e.g. const-wide v0, 5000000000L
    INSTRUCTION_FORMAT51l REGISTER COMMA fixed_literal {$size = Format.Format51l.size;}
    -> ^(I_STATEMENT_FORMAT51l[$start, "I_STATEMENT_FORMAT51l"] INSTRUCTION_FORMAT51l REGISTER fixed_literal);

insn_format52c_type returns [int size]
  : //e.g. instance-of/jumbo v0, v1, Ljava/lang/String;
    INSTRUCTION_FORMAT52c_TYPE REGISTER COMMA REGISTER COMMA nonvoid_type_descriptor {$size = Format.Format52c.size;}
    -> ^(I_STATEMENT_FORMAT52c_TYPE[$start, "I_STATEMENT_FORMAT52c_TYPE"] INSTRUCTION_FORMAT52c_TYPE REGISTER REGISTER nonvoid_type_descriptor);

insn_format52c_field returns [int size]
  : //e.g. iput-object/jumbo v1, v0 Lorg/jf/HelloWorld2/HelloWorld2;->helloWorld:Ljava/lang/String;
    INSTRUCTION_FORMAT52c_FIELD REGISTER COMMA REGISTER COMMA fully_qualified_field {$size = Format.Format52c.size;}
    -> ^(I_STATEMENT_FORMAT52c_FIELD[$start, "I_STATEMENT_FORMAT52c_FIELD"] INSTRUCTION_FORMAT52c_FIELD REGISTER REGISTER fully_qualified_field);

insn_format52c_field_odex returns [int size]
  : //e.g. iput-object-volatile/jumbo v1, v0 Lorg/jf/HelloWorld2/HelloWorld2;->helloWorld:Ljava/lang/String;
    INSTRUCTION_FORMAT52c_FIELD_ODEX REGISTER COMMA REGISTER COMMA fully_qualified_field {$size = Format.Format52c.size;}
    {
      throwOdexedInstructionException(input, $INSTRUCTION_FORMAT52c_FIELD_ODEX.text);
    };

insn_format5rc_method returns [int size]
  : //e.g. invoke-virtual/jumbo {v25..v26}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    INSTRUCTION_FORMAT5rc_METHOD OPEN_BRACE register_range CLOSE_BRACE COMMA fully_qualified_method {$size = Format.Format5rc.size;}
    -> ^(I_STATEMENT_FORMAT5rc_METHOD[$start, "I_STATEMENT_FORMAT5rc_METHOD"] INSTRUCTION_FORMAT5rc_METHOD register_range fully_qualified_method);

insn_format5rc_method_odex returns [int size]
  : //e.g. invoke-object-init/jumbo {v25}, Ljava/lang/Object-><init>()V
    INSTRUCTION_FORMAT5rc_METHOD_ODEX OPEN_BRACE register_range CLOSE_BRACE COMMA fully_qualified_method {$size = Format.Format5rc.size;}
    {
      throwOdexedInstructionException(input, $INSTRUCTION_FORMAT5rc_METHOD_ODEX.text);
    };

insn_format5rc_type returns [int size]
  : //e.g. filled-new-array/jumbo {v0..v6}, I
    INSTRUCTION_FORMAT5rc_TYPE OPEN_BRACE register_range CLOSE_BRACE COMMA nonvoid_type_descriptor {$size = Format.Format5rc.size;}
    -> ^(I_STATEMENT_FORMAT5rc_TYPE[$start, "I_STATEMENT_FORMAT5rc_TYPE"] INSTRUCTION_FORMAT5rc_TYPE register_range nonvoid_type_descriptor);

insn_array_data_directive returns [int size]
    @init {boolean needsNop = false;}
  :   ARRAY_DATA_DIRECTIVE
    {
      if (($method::currentAddress \% 2) != 0) {
        needsNop = true;
        $size = 2;
      } else {
        $size = 0;
      }
    }

    integral_literal (fixed_literal {$size+=$fixed_literal.size;})* END_ARRAY_DATA_DIRECTIVE
    {$size = (($size + 1)/2)*2 + 8;}

    /*add a nop statement before this if needed to force the correct alignment*/
    -> {needsNop}? ^(I_STATEMENT_FORMAT10x[$start,  "I_STATEMENT_FORMAT10x"] INSTRUCTION_FORMAT10x[$start, "nop"])
       ^(I_STATEMENT_ARRAY_DATA ^(I_ARRAY_ELEMENT_SIZE integral_literal) ^(I_ARRAY_ELEMENTS fixed_literal*))

    -> ^(I_STATEMENT_ARRAY_DATA[$start, "I_STATEMENT_ARRAY_DATA"] ^(I_ARRAY_ELEMENT_SIZE integral_literal)
       ^(I_ARRAY_ELEMENTS fixed_literal*));

insn_packed_switch_directive returns [int size]
    @init {boolean needsNop = false; int targetCount = 0;}
    :   PACKED_SWITCH_DIRECTIVE
    {
      targetCount = 0;
      if (($method::currentAddress \% 2) != 0) {
        needsNop = true;
        $size = 2;
      } else {
        $size = 0;
      }
    }

    fixed_32bit_literal

    (switch_target += label_ref_or_offset {$size+=4; targetCount++;})*

    END_PACKED_SWITCH_DIRECTIVE {$size = $size + 8;}

    /*add a nop statement before this if needed to force the correct alignment*/
    -> {needsNop}? ^(I_STATEMENT_FORMAT10x[$start,  "I_STATEMENT_FORMAT10x"] INSTRUCTION_FORMAT10x[$start, "nop"])
         ^(I_STATEMENT_PACKED_SWITCH[$start, "I_STATEMENT_PACKED_SWITCH"]
         ^(I_PACKED_SWITCH_START_KEY[$start, "I_PACKED_SWITCH_START_KEY"] fixed_32bit_literal)
         ^(I_PACKED_SWITCH_TARGETS[$start, "I_PACKED_SWITCH_TARGETS"]
           I_PACKED_SWITCH_TARGET_COUNT[$start, Integer.toString(targetCount)] $switch_target*)
       )

    -> ^(I_STATEMENT_PACKED_SWITCH[$start, "I_STATEMENT_PACKED_SWITCH"]
         ^(I_PACKED_SWITCH_START_KEY[$start, "I_PACKED_SWITCH_START_KEY"] fixed_32bit_literal)
         ^(I_PACKED_SWITCH_TARGETS[$start, "I_PACKED_SWITCH_TARGETS"]
           I_PACKED_SWITCH_TARGET_COUNT[$start, Integer.toString(targetCount)] $switch_target*)
       );

insn_sparse_switch_directive returns [int size]
    @init {boolean needsNop = false; int targetCount = 0;}
  :   SPARSE_SWITCH_DIRECTIVE
    {
      targetCount = 0;
      if (($method::currentAddress \% 2) != 0) {
        needsNop = true;
        $size = 2;
      } else {
        $size = 0;
      }
    }

    (fixed_32bit_literal ARROW switch_target += label_ref_or_offset {$size += 8; targetCount++;})*

    END_SPARSE_SWITCH_DIRECTIVE {$size = $size + 4;}

    /*add a nop statement before this if needed to force the correct alignment*/
    -> {needsNop}?
       ^(I_STATEMENT_FORMAT10x[$start,  "I_STATEMENT_FORMAT10x"] INSTRUCTION_FORMAT10x[$start, "nop"])
       ^(I_STATEMENT_SPARSE_SWITCH[$start, "I_STATEMENT_SPARSE_SWITCH"]
         I_SPARSE_SWITCH_TARGET_COUNT[$start, Integer.toString(targetCount)]
         ^(I_SPARSE_SWITCH_KEYS[$start, "I_SPARSE_SWITCH_KEYS"] fixed_32bit_literal*)
         ^(I_SPARSE_SWITCH_TARGETS $switch_target*)
       )

    -> ^(I_STATEMENT_SPARSE_SWITCH[$start, "I_STATEMENT_SPARSE_SWITCH"]
       I_SPARSE_SWITCH_TARGET_COUNT[$start, Integer.toString(targetCount)]
       ^(I_SPARSE_SWITCH_KEYS[$start, "I_SPARSE_SWITCH_KEYS"] fixed_32bit_literal*)
       ^(I_SPARSE_SWITCH_TARGETS $switch_target*));