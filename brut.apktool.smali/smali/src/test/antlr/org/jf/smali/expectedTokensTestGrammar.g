/*
 * The string related lexical rules are derived from rules from the
 * Java 1.6 grammar which can be found here:
 * http://openjdk.java.net/projects/compiler-grammar/antlrworks/Java.g
 *
 * Specifically, these rules:
 *
 * HEX_PREFIX, HEX_DIGIT, ESCAPE_SEQUENCE, STRING_LITERAL, BASE_STRING_LITERAL
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
 
grammar expectedTokensTestGrammar;

@lexer::header {
package org.jf.smali;
}

@parser::header {
package org.jf.smali;

import java.util.Collections;
}

@parser::members {
	public static class ExpectedToken {
		public final String tokenName;
		public final String tokenText;
		
		public ExpectedToken(String tokenName, String tokenText) {
			this.tokenName = tokenName;
			this.tokenText = tokenText;
		}
		
		public ExpectedToken(String tokenName) {
			this.tokenName = tokenName;
			this.tokenText = null;
		}
	}

	private final ArrayList<ExpectedToken> expectedTokens = new ArrayList<ExpectedToken>();
	
	public List<ExpectedToken> getExpectedTokens() {
		return Collections.unmodifiableList(expectedTokens);
	}
}


fragment HEX_DIGIT
	:	('0'..'9')|('A'..'F')|('a'..'f');

fragment HEX_DIGITS
	:	HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT;

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


STRING_LITERAL
	@init {StringBuilder sb = new StringBuilder();}
	:	BASE_STRING_LITERAL[sb] {setText(sb.toString());};

fragment BASE_STRING_LITERAL[StringBuilder sb]
	:	'"'
		(	ESCAPE_SEQUENCE[sb]
		|	~( '\\' | '"' | '\r' | '\n' ) {sb.append((char)input.LA(-1));}
		)*
		'"';

TOKEN_NAME
	:	(('a'..'z')|('A' .. 'Z')|'_'|('0'..'9'))+;
	
WHITE_SPACE
	:	(' '|'\t'|'\n'|'\r')+ {$channel = HIDDEN;};	

top	:	token*;

token	:	TOKEN_NAME ( '(' STRING_LITERAL ')' )
		{
			expectedTokens.add(new ExpectedToken($TOKEN_NAME.getText(), $STRING_LITERAL.getText()));
		} |
		TOKEN_NAME
		{
			expectedTokens.add(new ExpectedToken($TOKEN_NAME.getText()));
		};