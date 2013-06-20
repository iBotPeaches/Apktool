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
 * INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import org.antlr.runtime.ANTLRInputStream;
import org.antlr.runtime.CommonToken;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.jf.smali.expectedTokensTestGrammarLexer;
import org.jf.smali.expectedTokensTestGrammarParser;
import org.jf.smali.smaliFlexLexer;
import org.jf.smali.smaliParser;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;

import static org.jf.smali.expectedTokensTestGrammarParser.ExpectedToken;

public class LexerTest {
    private static final HashMap<String, Integer> tokenTypesByName;

    static {
        tokenTypesByName = new HashMap<String, Integer>();

        for (int i=0; i<smaliParser.tokenNames.length; i++) {
            tokenTypesByName.put(smaliParser.tokenNames[i], i);
        }
    }

    @Test
    public void DirectiveTest() {
        runTest("DirectiveTest");
    }

    @Test
    public void ByteLiteralTest() {
        runTest("ByteLiteralTest");
    }

    @Test
    public void ShortLiteralTest() {
        runTest("ShortLiteralTest");
    }

    @Test
    public void IntegerLiteralTest() {
        runTest("IntegerLiteralTest");
    }

    @Test
    public void LongLiteralTest() {
        runTest("LongLiteralTest");
    }

    @Test
    public void FloatLiteralTest() {
        runTest("FloatLiteralTest");
    }

    @Test
    public void CharLiteralTest() {
        runTest("CharLiteralTest");
    }

    @Test
    public void StringLiteralTest() {
        runTest("StringLiteralTest");
    }

    @Test
    public void MiscTest() {
        runTest("MiscTest");
    }

    @Test
    public void CommentTest() {
        runTest("CommentTest", false);
    }

    @Test
    public void InstructionTest() {
        runTest("InstructionTest", true);
    }

    @Test
    public void TypeAndIdentifierTest() {
        runTest("TypeAndIdentifierTest");
    }

    @Test
    public void SymbolTest() {
        runTest("SymbolTest", false);
    }

    @Test
    public void RealSmaliFileTest() {
        runTest("RealSmaliFileTest", true);
    }

    public void runTest(String test) {
        runTest(test, true);
    }

    public void runTest(String test, boolean discardHiddenTokens) {
        String smaliFile = String.format("LexerTest%s%s.smali", File.separatorChar, test);
        String tokensFile = String.format("LexerTest%s%s.tokens", File.separatorChar, test);

        expectedTokensTestGrammarLexer expectedTokensLexer = null;
        try {
            expectedTokensLexer = new expectedTokensTestGrammarLexer(new ANTLRInputStream(
                    LexerTest.class.getClassLoader().getResourceAsStream(tokensFile)));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        CommonTokenStream expectedTokensStream = new CommonTokenStream(expectedTokensLexer);

        expectedTokensTestGrammarParser expectedTokensParser =
                new expectedTokensTestGrammarParser(expectedTokensStream);
        try {
            expectedTokensParser.top();
        } catch (RecognitionException ex) {
            throw new RuntimeException(ex);
        }

        List<ExpectedToken> expectedTokens = expectedTokensParser.getExpectedTokens();

        InputStream smaliStream = LexerTest.class.getClassLoader().getResourceAsStream(smaliFile);
        if (smaliStream == null) {
            Assert.fail("Could not load " + smaliFile);
        }
        smaliFlexLexer lexer = new smaliFlexLexer(smaliStream);
        lexer.setSourceFile(new File(test + ".smali"));
        lexer.setSuppressErrors(true);

        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        tokenStream.fill();
        List tokens = tokenStream.getTokens();

        int expectedTokenIndex = 0;
        CommonToken token;
        for (int i=0; i<tokens.size()-1; i++) {
            token = (CommonToken)tokens.get(i);

            if (discardHiddenTokens && token.getChannel() == smaliParser.HIDDEN) {
                continue;
            }

            if (expectedTokenIndex >= expectedTokens.size()) {
                Assert.fail("Too many tokens");
            }

            if (token.getType() == smaliParser.INVALID_TOKEN) {
                Assert.assertTrue("Encountered an INVALID_TOKEN not on the error channel",
                        token.getChannel() == smaliParser.ERROR_CHANNEL);
            }

            ExpectedToken expectedToken = expectedTokens.get(expectedTokenIndex++);
            if (!tokenTypesByName.containsKey(expectedToken.tokenName)) {
                Assert.fail("Unknown token: " + expectedToken.tokenName);
            }
            int expectedTokenType = tokenTypesByName.get(expectedToken.tokenName);

            if (token.getType() != expectedTokenType) {
                Assert.fail(String.format("Invalid token at index %d. Expecting %s, got %s(%s)",
                        expectedTokenIndex-1, expectedToken.tokenName, getTokenName(token.getType()), token.getText()));
            }

            if (expectedToken.tokenText != null) {
                if (!expectedToken.tokenText.equals(token.getText())) {
                    Assert.fail(
                            String.format("Invalid token text at index %d. Expecting text \"%s\", got \"%s\"",
                                    expectedTokenIndex - 1, expectedToken.tokenText, token.getText()));
                }
            }
        }

        if (expectedTokenIndex < expectedTokens.size()) {
            Assert.fail(String.format("Not enough tokens. Expecting %d tokens, but got %d", expectedTokens.size(),
                    expectedTokenIndex));
        }
    }



    private static String getTokenName(int tokenType) {
        return smaliParser.tokenNames[tokenType];
    }
}
