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

package org.jf.smali;

import org.antlr.runtime.CommonToken;
import org.antlr.runtime.IntStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.Token;
import org.antlr.runtime.tree.CommonTree;

public class SemanticException extends RecognitionException {
    private String errorMessage;


    SemanticException(IntStream input, String errorMessage, Object... messageArguments) {
        super(input);
        this.errorMessage = String.format(errorMessage, messageArguments);
    }

    SemanticException(IntStream input, Exception ex) {
        super(input);
        this.errorMessage = ex.getMessage();
    }

    SemanticException(IntStream input, CommonTree tree, String errorMessage, Object... messageArguments) {
        super();
        this.input = input;
        this.token = tree.getToken();
        this.index = tree.getTokenStartIndex();
        this.line = token.getLine();
	    this.charPositionInLine = token.getCharPositionInLine();
        this.errorMessage = String.format(errorMessage, messageArguments);
    }

    SemanticException(IntStream input, Token token, String errorMessage, Object... messageArguments) {
        super();
        this.input = input;
        this.token = token;
        this.index = ((CommonToken)token).getStartIndex();
        this.line = token.getLine();
	    this.charPositionInLine = token.getCharPositionInLine();
        this.errorMessage = String.format(errorMessage, messageArguments);
    }

    public String getMessage() {
        return errorMessage;
    }
}
