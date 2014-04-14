package org.jf.dexlib2.writer.io;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface DexDataStore {
    @Nonnull OutputStream outputAt(int offset);
    @Nonnull InputStream readAt(int offset);
    void close() throws IOException;
}
