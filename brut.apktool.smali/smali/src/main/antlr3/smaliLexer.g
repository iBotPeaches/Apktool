/*
 * The comment, number, string and character constant lexical rules are
 * derived from rules from the Java 1.6 grammar which can be found here:
 * http://openjdk.java.net/projects/compiler-grammar/antlrworks/Java.g
 *
 * Specifically, these rules:
 *
 * BASE_INTEGER, DECIMAL_EXPONENT, BINARY_EXPONENT, HEX_PREFIX, HEX_DIGIT,
 * BASE_FLOAT_OR_ID, BASE_FLOAT, ESCAPE_SEQUENCE, POSITIVE_INTEGER_LITERAL,
 * NEGATIVE_INTEGER_LITERAL, LONG_LITERAL, SHORT_LITERAL, BYTE_LITERAL,
 * FLOAT_LITERAL_OR_ID, DOUBLE_LITERAL_OR_ID, FLOAT_LITERAL, DOUBLE_LITERAL,
 * BOOL_LITERAL, STRING_LITERAL, BASE_STRING_LITERAL, CHAR_LITERAL,
 * BASE_CHAR_LITERAL
 *
 * These rules were originally copyrighted by Terence Parr, and are used here in
 * accordance with the following license
 *
 * [The "BSD licence"]
 * Copyright (c) 2007-2008 Terence Parr
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
 *
 *
 * The remainder of this grammar is released by me (Ben Gruver) under the
 * following license:
 *
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

lexer grammar smaliLexer;

options {
	superClass=ANTLRLexerWithErrorInterface;
}

@lexer::header {
	package org.jf.smali;

	import static org.jf.smali.LexerErrorInterface.ANTLRLexerWithErrorInterface;
}

@lexer::members {
	public static final int ERROR_CHANNEL = 100;
	public String getErrorHeader(RecognitionException e) {
		return getSourceName()+"["+ e.line+","+e.charPositionInLine+"]";
	}
}

/**********************************************************
* DIRECTIVES
**********************************************************/

CLASS_DIRECTIVE
	:	'.class';

SUPER_DIRECTIVE
	:	'.super';

IMPLEMENTS_DIRECTIVE
	:	'.implements';

SOURCE_DIRECTIVE
	:	'.source';

FIELD_DIRECTIVE
	:	'.field';

END_FIELD_DIRECTIVE
	:	'.end field';

SUBANNOTATION_DIRECTIVE
	:	'.subannotation';

END_SUBANNOTATION_DIRECTIVE
	:	'.end subannotation';

ANNOTATION_DIRECTIVE
	:	'.annotation';

END_ANNOTATION_DIRECTIVE
	:	'.end annotation';

ENUM_DIRECTIVE
	:	'.enum';

METHOD_DIRECTIVE
	:	'.method';

END_METHOD_DIRECTIVE
	:	'.end method';

REGISTERS_DIRECTIVE
	:	'.registers';

LOCALS_DIRECTIVE
	:	'.locals';

ARRAY_DATA_DIRECTIVE
	:	'.array-data';

END_ARRAY_DATA_DIRECTIVE
	:	'.end array-data';

PACKED_SWITCH_DIRECTIVE
	:	'.packed-switch';

END_PACKED_SWITCH_DIRECTIVE
	:	'.end packed-switch';

SPARSE_SWITCH_DIRECTIVE
	:	'.sparse-switch';

END_SPARSE_SWITCH_DIRECTIVE
	:	'.end sparse-switch';

CATCH_DIRECTIVE
	:	'.catch';

CATCHALL_DIRECTIVE
	:	'.catchall';

LINE_DIRECTIVE
	:	'.line';

PARAMETER_DIRECTIVE
	:	'.parameter';

END_PARAMETER_DIRECTIVE
	:	'.end parameter';

LOCAL_DIRECTIVE
	:	'.local';

END_LOCAL_DIRECTIVE
	:	'.end local';

RESTART_LOCAL_DIRECTIVE
	:	'.restart local';

PROLOGUE_DIRECTIVE
	:	'.prologue';

EPILOGUE_DIRECTIVE
	:	'.epilogue';

/**********************************************************
* LITERALS
**********************************************************/
fragment BASE_INTEGER
	:	'0'
	|	('1'..'9') ('0'..'9')*
	|	'0' ('0'..'7')+
	|	HEX_PREFIX HEX_DIGIT+;

fragment DECIMAL_EXPONENT
	:	('e'|'E') '-'? ('0'..'9')+;

fragment BINARY_EXPONENT
	:	('p'|'P') '-'? ('0'..'9')+;

fragment HEX_PREFIX
	:	'0x'|'0X';

fragment HEX_DIGIT
	:	('0'..'9')|('A'..'F')|('a'..'f');

fragment HEX_DIGITS
	:	HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT;

/*This can either be floating point numbers, or identifier*/
fragment BASE_FLOAT_OR_ID
	:	'-'? ('0'..'9')+ DECIMAL_EXPONENT
	|	HEX_PREFIX HEX_DIGIT+ BINARY_EXPONENT
	|	'-'? ('i' | 'I') ('n' | 'N') ('f' | 'F') ('i' | 'I') ('n' | 'N') ('i' | 'I') ('t' | 'T') ('y' | 'Y')
	|	('n' | 'N') ('a' | 'A') ('n' | 'N');

/*These can't be identifiers, due to the decimal point*/
fragment BASE_FLOAT
	:	'-'? ('0'..'9')+ '.' ('0'..'9')* DECIMAL_EXPONENT?
	|	'-'? '.' ('0'..'9')+ DECIMAL_EXPONENT?
	|	'-'? HEX_PREFIX HEX_DIGIT+ '.' HEX_DIGIT* BINARY_EXPONENT
	|	'-'? HEX_PREFIX '.' HEX_DIGIT+ BINARY_EXPONENT;

fragment ESCAPE_SEQUENCE[StringBuilder sb]
	:	'\\'
		(
			'b' {sb.append("\b");}
		|	't' {sb.append("\t");}
		|	'n' {sb.append("\n");}
		|	'f' {sb.append("\f");}
		|	'r' {sb.append("\r");}
		|	'\"' {sb.append("\"");}
		|	'\'' {sb.append("'");}
		|	'\\' {sb.append("\\");}
		|	'u' HEX_DIGITS {sb.append((char)Integer.parseInt($HEX_DIGITS.text, 16));}
		);

POSITIVE_INTEGER_LITERAL
	:	BASE_INTEGER;

NEGATIVE_INTEGER_LITERAL
	:	'-' BASE_INTEGER;

LONG_LITERAL
	:	'-'? BASE_INTEGER ('l'|'L');

SHORT_LITERAL
	:	'-'? BASE_INTEGER ('s'|'S');

BYTE_LITERAL
	:	'-'? BASE_INTEGER ('t'|'T');

FLOAT_LITERAL_OR_ID
	:	BASE_FLOAT_OR_ID ('f'|'F')
	|	'-'? ('0'..'9')+ ('f'|'F');

DOUBLE_LITERAL_OR_ID
	:	BASE_FLOAT_OR_ID ('d'|'D')?
	|	'-'? ('0'..'9')+ ('d'|'D');

FLOAT_LITERAL
	:	BASE_FLOAT ('f'|'F');

DOUBLE_LITERAL
	:	BASE_FLOAT ('d'|'D')?;

BOOL_LITERAL
	:	'true'
	|	'false';

NULL_LITERAL
	:	'null';

STRING_LITERAL
	@init {StringBuilder sb = new StringBuilder();}
	:	BASE_STRING_LITERAL[sb] {setText(sb.toString());};

fragment BASE_STRING_LITERAL[StringBuilder sb]
	:	'"' {sb.append('"');}
		(	ESCAPE_SEQUENCE[sb]
		|	~( '\\' | '"' | '\r' | '\n' ) {sb.append((char)input.LA(-1));}
		)*
		'"' {sb.append('"');};

CHAR_LITERAL
	@init {StringBuilder sb = new StringBuilder();}
	:	BASE_CHAR_LITERAL[sb] {setText(sb.toString());};

fragment BASE_CHAR_LITERAL[StringBuilder sb]
	:	'\'' {sb.append('\'');}
	        (	ESCAPE_SEQUENCE[sb]
	        |	~( '\\' | '\'' | '\r' | '\n' )  {sb.append((char)input.LA(-1));}
	        )
	        '\'' { sb.append('\''); };


/**********************************************************
* MISC
**********************************************************/
REGISTER
	:	('v'|'p') ('0'..'9')+;

ANNOTATION_VISIBILITY
	:	'build'
	|	'runtime'
	|	'system';

ACCESS_SPEC
	:	'public'
	|	'private'
	|	'protected'
	|	'static'
	|	'final'
	|	'synchronized'
	|	'bridge'
	|	'varargs'
	|	'native'
	|	'abstract'
	|	'strictfp'
	|	'synthetic'
	|	'constructor'
	|	'declared-synchronized'
	|	'interface'
	|	'enum'
	|	'annotation'
	|	'volatile'
	|	'transient';

VERIFICATION_ERROR_TYPE
    :   'no-error'
    |   'generic-error'
    |   'no-such-class'
    |   'no-such-field'
    |   'no-such-method'
    |   'illegal-class-access'
    |   'illegal-field-access'
    |   'illegal-method-access'
    |   'class-change-error'
    |   'instantiation-error';

INLINE_INDEX
	:	'inline@0x' HEX_DIGIT+;

VTABLE_INDEX
	:	'vtable@0x' HEX_DIGIT+;

FIELD_OFFSET
	:	'field@0x' HEX_DIGIT+;

OFFSET
	:	'+' BASE_INTEGER;

LINE_COMMENT
	:	'#'
		(
			~('\n'|'\r')*  ('\r\n' | '\r' | '\n')
		|	~('\n'|'\r')*
		)
		{$channel = HIDDEN;};

/**********************************************************
* Instructions
**********************************************************/
INSTRUCTION_FORMAT10t
	:	'goto';

INSTRUCTION_FORMAT10x
	:	'return-void'
	|	'nop';

INSTRUCTION_FORMAT10x_ODEX
	:	'return-void-barrier';

INSTRUCTION_FORMAT11n
	:	'const/4';

INSTRUCTION_FORMAT11x
	:	'move-result'
	|	'move-result-wide'
	|	'move-result-object'
	|	'move-exception'
	|	'return'
	|	'return-wide'
	|	'return-object'
	|	'monitor-enter'
	|	'monitor-exit'
	|	'throw';

INSTRUCTION_FORMAT12x_OR_ID
	:	'move'
	|	'move-wide'
	|	'move-object'
	|	'array-length'
	|	'neg-int'
	|	'not-int'
	|	'neg-long'
	|	'not-long'
	|	'neg-float'
	|	'neg-double'
	|	'int-to-long'
	|	'int-to-float'
	|	'int-to-double'
	|	'long-to-int'
	|	'long-to-float'
	|	'long-to-double'
	|	'float-to-int'
	|	'float-to-long'
	|	'float-to-double'
	|	'double-to-int'
	|	'double-to-long'
	|	'double-to-float'
	|	'int-to-byte'
	|	'int-to-char'
	|	'int-to-short';

INSTRUCTION_FORMAT12x
	:	'add-int/2addr'
	|	'sub-int/2addr'
	|	'mul-int/2addr'
	|	'div-int/2addr'
	|	'rem-int/2addr'
	|	'and-int/2addr'
	|	'or-int/2addr'
	|	'xor-int/2addr'
	|	'shl-int/2addr'
	|	'shr-int/2addr'
	|	'ushr-int/2addr'
	|	'add-long/2addr'
	|	'sub-long/2addr'
	|	'mul-long/2addr'
	|	'div-long/2addr'
	|	'rem-long/2addr'
	|	'and-long/2addr'
	|	'or-long/2addr'
	|	'xor-long/2addr'
	|	'shl-long/2addr'
	|	'shr-long/2addr'
	|	'ushr-long/2addr'
	|	'add-float/2addr'
	|	'sub-float/2addr'
	|	'mul-float/2addr'
	|	'div-float/2addr'
	|	'rem-float/2addr'
	|	'add-double/2addr'
	|	'sub-double/2addr'
	|	'mul-double/2addr'
	|	'div-double/2addr'
	|	'rem-double/2addr';

INSTRUCTION_FORMAT20bc
    :   'throw-verification-error';

INSTRUCTION_FORMAT20t
	:	'goto/16';

INSTRUCTION_FORMAT21c_FIELD
	:	'sget'
	|	'sget-wide'
	|	'sget-object'
	|	'sget-boolean'
	|	'sget-byte'
	|	'sget-char'
	|	'sget-short'
	|	'sput'
	|	'sput-wide'
	|	'sput-object'
	|	'sput-boolean'
	|	'sput-byte'
	|	'sput-char'
	|	'sput-short';

INSTRUCTION_FORMAT21c_FIELD_ODEX
	:	'sget-volatile'
	|	'sget-wide-volatile'
	|	'sget-object-volatile'
	|	'sput-volatile'
	|	'sput-wide-volatile'
	|	'sput-object-volatile';

INSTRUCTION_FORMAT21c_STRING
	:	'const-string';

INSTRUCTION_FORMAT21c_TYPE
	:	'check-cast'
	|	'new-instance'
	|	'const-class';

INSTRUCTION_FORMAT21h
	:	'const/high16'
	|	'const-wide/high16';

INSTRUCTION_FORMAT21s
	:	'const/16'
	|	'const-wide/16';

INSTRUCTION_FORMAT21t
	:	'if-eqz'
	|	'if-nez'
	|	'if-ltz'
	|	'if-gez'
	|	'if-gtz'
	|	'if-lez';

INSTRUCTION_FORMAT22b
	:	'add-int/lit8'
	|	'rsub-int/lit8'
	|	'mul-int/lit8'
	|	'div-int/lit8'
	|	'rem-int/lit8'
	|	'and-int/lit8'
	|	'or-int/lit8'
	|	'xor-int/lit8'
	|	'shl-int/lit8'
	|	'shr-int/lit8'
	|	'ushr-int/lit8';

INSTRUCTION_FORMAT22c_FIELD
	:	'iget'
	|	'iget-wide'
	|	'iget-object'
	|	'iget-boolean'
	|	'iget-byte'
	|	'iget-char'
	|	'iget-short'
	|	'iput'
	|	'iput-wide'
	|	'iput-object'
	|	'iput-boolean'
	|	'iput-byte'
	|	'iput-char'
	|	'iput-short';

INSTRUCTION_FORMAT22c_FIELD_ODEX
	:	'iget-volatile'
	|	'iget-wide-volatile'
	|	'iget-object-volatile'
	|	'iput-volatile'
	|	'iput-wide-volatile'
	|	'iput-object-volatile';

INSTRUCTION_FORMAT22c_TYPE
	:	'instance-of'
	|	'new-array';


INSTRUCTION_FORMAT22cs_FIELD
	:	'iget-quick'
	|	'iget-wide-quick'
	|	'iget-object-quick'
	|	'iput-quick'
	|	'iput-wide-quick'
	|	'iput-object-quick';

INSTRUCTION_FORMAT22s_OR_ID
	:	'rsub-int';

INSTRUCTION_FORMAT22s
	:	'add-int/lit16'
	|	'mul-int/lit16'
	|	'div-int/lit16'
	|	'rem-int/lit16'
	|	'and-int/lit16'
	|	'or-int/lit16'
	|	'xor-int/lit16';

INSTRUCTION_FORMAT22t
	:	'if-eq'
	|	'if-ne'
	|	'if-lt'
	|	'if-ge'
	|	'if-gt'
	|	'if-le';

INSTRUCTION_FORMAT22x
	:	'move/from16'
	|	'move-wide/from16'
	|	'move-object/from16';

INSTRUCTION_FORMAT23x
	:	'cmpl-float'
	|	'cmpg-float'
	|	'cmpl-double'
	|	'cmpg-double'
	|	'cmp-long'
	|	'aget'
	|	'aget-wide'
	|	'aget-object'
	|	'aget-boolean'
	|	'aget-byte'
	|	'aget-char'
	|	'aget-short'
	|	'aput'
	|	'aput-wide'
	|	'aput-object'
	|	'aput-boolean'
	|	'aput-byte'
	|	'aput-char'
	|	'aput-short'
	|	'add-int'
	|	'sub-int'
	|	'mul-int'
	|	'div-int'
	|	'rem-int'
	|	'and-int'
	|	'or-int'
	|	'xor-int'
	|	'shl-int'
	|	'shr-int'
	|	'ushr-int'
	|	'add-long'
	|	'sub-long'
	|	'mul-long'
	|	'div-long'
	|	'rem-long'
	|	'and-long'
	|	'or-long'
	|	'xor-long'
	|	'shl-long'
	|	'shr-long'
	|	'ushr-long'
	|	'add-float'
	|	'sub-float'
	|	'mul-float'
	|	'div-float'
	|	'rem-float'
	|	'add-double'
	|	'sub-double'
	|	'mul-double'
	|	'div-double'
	|	'rem-double';

INSTRUCTION_FORMAT30t
	:	'goto/32';

INSTRUCTION_FORMAT31c
	:	'const-string/jumbo';

INSTRUCTION_FORMAT31i_OR_ID
	:	'const';

INSTRUCTION_FORMAT31i
	:	'const-wide/32';

INSTRUCTION_FORMAT31t
	:	'fill-array-data'
	|	'packed-switch'
	|	'sparse-switch';

INSTRUCTION_FORMAT32x
	:	'move/16'
	|	'move-wide/16'
	|	'move-object/16';

INSTRUCTION_FORMAT35c_METHOD
	:	'invoke-virtual'
	|	'invoke-super'
	|	'invoke-direct'
	|	'invoke-static'
	|	'invoke-interface';

INSTRUCTION_FORMAT35c_METHOD_ODEX
	:	'invoke-direct-empty';

INSTRUCTION_FORMAT35c_TYPE
	:	'filled-new-array';

INSTRUCTION_FORMAT35mi_METHOD
	:	'execute-inline';

INSTRUCTION_FORMAT35ms_METHOD
	:	'invoke-virtual-quick'
	|	'invoke-super-quick';

INSTRUCTION_FORMAT3rc_METHOD
	:	'invoke-virtual/range'
	|	'invoke-super/range'
	|	'invoke-direct/range'
	|	'invoke-static/range'
	|	'invoke-interface/range';

INSTRUCTION_FORMAT3rc_METHOD_ODEX
	:	'invoke-object-init/range';

INSTRUCTION_FORMAT3rc_TYPE
	:	'filled-new-array/range';

INSTRUCTION_FORMAT3rmi_METHOD
	:	'execute-inline/range';

INSTRUCTION_FORMAT3rms_METHOD
	:	'invoke-virtual-quick/range'
	|	'invoke-super-quick/range';

INSTRUCTION_FORMAT51l
	:	'const-wide';

/**********************************************************
* Types
**********************************************************/
fragment BASE_SIMPLE_NAME:
	(	'A'..'Z'
	|	'a'..'z'
	|	'0'..'9'
	|	'$'
	|	'-'
	|	'_'
	|	'\u00a1'..'\u1fff'
	|	'\u2010'..'\u2027'
	|	'\u2030'..'\ud7ff'
	|	'\ue000'..'\uffef'
	)+;

fragment BASE_PRIMITIVE_TYPE
	:	'Z'|'B'|'S'|'C'|'I'|'J'|'F'|'D';


fragment BASE_CLASS_DESCRIPTOR
	:	'L' (BASE_SIMPLE_NAME '/')* BASE_SIMPLE_NAME ';';

fragment BASE_ARRAY_DESCRIPTOR
	:	'['+ (BASE_PRIMITIVE_TYPE | BASE_CLASS_DESCRIPTOR);

fragment BASE_TYPE
	:	BASE_PRIMITIVE_TYPE
	|	BASE_CLASS_DESCRIPTOR
	|	BASE_ARRAY_DESCRIPTOR;

PRIMITIVE_TYPE
	:	BASE_PRIMITIVE_TYPE;

VOID_TYPE
	:	'V';

CLASS_DESCRIPTOR
	:	BASE_CLASS_DESCRIPTOR;

ARRAY_DESCRIPTOR
	:	BASE_ARRAY_DESCRIPTOR;

PARAM_LIST_OR_ID
	:	BASE_PRIMITIVE_TYPE BASE_PRIMITIVE_TYPE+;

PARAM_LIST
	:	BASE_TYPE BASE_TYPE+;

SIMPLE_NAME
	:	BASE_SIMPLE_NAME;

METHOD_NAME
	:	'<init>'
	|	'<clinit>';


/**********************************************************
* Symbols
**********************************************************/

DOTDOT
	:	'..';

ARROW
	:	'->';

EQUAL
	:	'=';

COLON
	:	':';

COMMA
	:	',';

OPEN_BRACE
	:	'{';

CLOSE_BRACE
	:	'}';

OPEN_PAREN
	:	'(';

CLOSE_PAREN
	:	')';

WHITE_SPACE
	:	(' '|'\t'|'\n'|'\r')+ {$channel = HIDDEN;};
