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

import org.jf.dexlib.Code.Analysis.SyntheticAccessorResolver;
import static org.jf.dexlib.Code.Analysis.SyntheticAccessorResolver.AccessedMember;
import org.jf.util.IndentingWriter;

import java.io.IOException;

public class SyntheticAccessCommentMethodItem extends MethodItem {
    private final AccessedMember accessedMember;

    public SyntheticAccessCommentMethodItem(AccessedMember accessedMember, int codeAddress) {
        super(codeAddress);
        this.accessedMember = accessedMember;
    }

    public double getSortOrder() {
        //just before the pre-instruction register information, if any
        return 99.8;
    }

    public boolean writeTo(IndentingWriter writer) throws IOException {
        writer.write('#');
        if (accessedMember.getAccessedMemberType() == SyntheticAccessorResolver.METHOD) {
            writer.write("calls: ");
        } else if (accessedMember.getAccessedMemberType() == SyntheticAccessorResolver.GETTER) {
            writer.write("getter for: ");
        } else {
            writer.write("setter for: ");
        }
        ReferenceFormatter.writeReference(writer, accessedMember.getAccessedMember());
        return true;
    }
}
