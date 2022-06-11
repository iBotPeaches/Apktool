package brut.util;

import brut.common.BrutException;

import java.io.File;

public interface AaptProvider {
    File getAapt2() throws BrutException;
    default File getAapt1() throws BrutException {
        throw new BrutException(new UnsupportedOperationException("Aapt1 is not supported on this device"));
    };
}
