package org.jf.dexlib2.writer.io;

import java.io.IOException;
import java.io.OutputStream;

public abstract class DeferredOutputStream extends OutputStream {
    public abstract void writeTo(OutputStream output) throws IOException;
}
