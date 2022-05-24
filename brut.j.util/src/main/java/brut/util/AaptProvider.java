package brut.util;

import brut.common.BrutException;

import java.io.File;

public interface AaptProvider {
    File getAapt2() throws BrutException;
}
