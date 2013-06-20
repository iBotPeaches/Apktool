/*
 * [The "BSD licence"]
 * Copyright (c) 2011 Ben Gruver
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

package org.jf.baksmali.Adaptors;

import org.jf.dexlib2.ReferenceType;
import org.jf.dexlib2.util.SyntheticAccessorResolver;
import org.jf.util.ExceptionWithContext;
import org.jf.util.IndentingWriter;

import java.io.IOException;

public class SyntheticAccessCommentMethodItem extends MethodItem {
    private final SyntheticAccessorResolver.AccessedMember accessedMember;

    public SyntheticAccessCommentMethodItem(SyntheticAccessorResolver.AccessedMember accessedMember, int codeAddress) {
        super(codeAddress);
        this.accessedMember = accessedMember;
    }

    public double getSortOrder() {
        //just before the pre-instruction register information, if any
        return 99.8;
    }

    public boolean writeTo(IndentingWriter writer) throws IOException {
        writer.write("# ");
        switch (accessedMember.accessedMemberType) {
            case SyntheticAccessorResolver.METHOD:
                writer.write("invokes: ");
                break;
            case SyntheticAccessorResolver.GETTER:
                writer.write("getter for: ");
                break;
            case SyntheticAccessorResolver.SETTER:
                writer.write("setter for: ");
                break;
            case SyntheticAccessorResolver.PREFIX_INCREMENT:
                writer.write("++operator for: ");
                break;
            case SyntheticAccessorResolver.POSTFIX_INCREMENT:
                writer.write("operator++ for: ");
                break;
            case SyntheticAccessorResolver.PREFIX_DECREMENT:
                writer.write("--operator for: ");
                break;
            case SyntheticAccessorResolver.POSTFIX_DECREMENT:
                writer.write("operator-- for: ");
                break;
            case SyntheticAccessorResolver.ADD_ASSIGNMENT:
                writer.write("+= operator for: ");
                break;
            case SyntheticAccessorResolver.SUB_ASSIGNMENT:
                writer.write("-= operator for: ");
                break;
            case SyntheticAccessorResolver.MUL_ASSIGNMENT:
                writer.write("*= operator for: ");
                break;
            case SyntheticAccessorResolver.DIV_ASSIGNMENT:
                writer.write("/= operator for: ");
                break;
            case SyntheticAccessorResolver.REM_ASSIGNMENT:
                writer.write("%= operator for: ");
                break;
            case SyntheticAccessorResolver.AND_ASSIGNMENT:
                writer.write("&= operator for: ");
                break;
            case SyntheticAccessorResolver.OR_ASSIGNMENT:
                writer.write("|= operator for: ");
                break;
            case SyntheticAccessorResolver.XOR_ASSIGNMENT:
                writer.write("^= operator for: ");
                break;
            case SyntheticAccessorResolver.SHL_ASSIGNMENT:
                writer.write("<<= operator for: ");
                break;
            case SyntheticAccessorResolver.SHR_ASSIGNMENT:
                writer.write(">>= operator for: ");
                break;
            case SyntheticAccessorResolver.USHR_ASSIGNMENT:
                writer.write(">>>= operator for: ");
                break;
            default:
                throw new ExceptionWithContext("Unknown access type: %d", accessedMember.accessedMemberType);
        }

        int referenceType;
        if (accessedMember.accessedMemberType == SyntheticAccessorResolver.METHOD) {
            referenceType = ReferenceType.METHOD;
        } else {
            referenceType = ReferenceType.FIELD;
        }
        ReferenceFormatter.writeReference(writer, referenceType, accessedMember.accessedMember);
        return true;
    }
}
