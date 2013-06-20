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

package org.jf.baksmali.Adaptors;

import org.jf.baksmali.baksmaliOptions;
import org.jf.util.IndentingWriter;

import javax.annotation.Nonnull;
import java.io.IOException;

public class LabelMethodItem extends MethodItem {
    private final baksmaliOptions options;
    private final String labelPrefix;
    private int labelSequence;

    public LabelMethodItem(@Nonnull baksmaliOptions options, int codeAddress, @Nonnull String labelPrefix) {
        super(codeAddress);
        this.options = options;
        this.labelPrefix = labelPrefix;
    }

    public double getSortOrder() {
        return 0;
    }

    public int compareTo(MethodItem methodItem) {
        int result = super.compareTo(methodItem);

        if (result == 0) {
            if (methodItem instanceof LabelMethodItem) {
                result = labelPrefix.compareTo(((LabelMethodItem)methodItem).labelPrefix);
            }
        }
        return result;
    }

    public int hashCode() {
        //force it to call equals when two labels are at the same address
        return getCodeAddress();
    }

    public boolean equals(Object o) {
        if (!(o instanceof LabelMethodItem)) {
            return false;
        }
        return this.compareTo((MethodItem)o) == 0;
    }


    public boolean writeTo(IndentingWriter writer) throws IOException {
        writer.write(':');
        writer.write(labelPrefix);
        if (options.useSequentialLabels) {
            writer.printUnsignedLongAsHex(labelSequence);
        } else {
            writer.printUnsignedLongAsHex(this.getLabelAddress());
        }
        return true;
    }

    public String getLabelPrefix() {
        return labelPrefix;
    }

    public int getLabelAddress() {
        return this.getCodeAddress();
    }

    public int getLabelSequence() {
        return labelSequence;
    }

    public void setLabelSequence(int labelSequence) {
        this.labelSequence = labelSequence;
    }
}
