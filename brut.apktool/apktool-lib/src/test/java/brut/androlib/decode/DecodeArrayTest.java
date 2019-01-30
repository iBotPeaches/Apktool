package brut.androlib.decode;

import brut.androlib.ApkDecoder;
import brut.androlib.BaseTest;
import brut.androlib.TestUtils;
import brut.androlib.res.data.ResTable;
import brut.androlib.res.data.value.ResArrayValue;
import brut.androlib.res.data.value.ResValue;
import brut.common.BrutException;
import brut.directory.ExtFile;
import brut.util.OS;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;

import static junit.framework.Assert.assertTrue;

public class DecodeArrayTest extends BaseTest {

    @BeforeClass
    public static void beforeClass() throws Exception {
        TestUtils.cleanFrameworkFile();
        sTmpDir = new ExtFile(OS.createTempDirectory());
        TestUtils.copyResourceDir(MissingVersionManifestTest.class, "decode/issue1994/", sTmpDir);
    }

    @AfterClass
    public static void afterClass() throws BrutException {
        OS.rmdir(sTmpDir);
    }

    @Test
    public void decodeStringArray() throws BrutException {
        String apk = "issue1994.apk";
        ApkDecoder apkDecoder = new ApkDecoder(new File(sTmpDir + File.separator + apk));

        ResTable resTable = apkDecoder.getResTable();
        ResValue value = resTable.getResSpec(0x7f020001).getDefaultResource().getValue();

        assertTrue("Not a ResArrayValue. Found: " + value.getClass(), value instanceof ResArrayValue);
    }

    @Test
    public void decodeArray() throws BrutException {
        String apk = "issue1994.apk";
        ApkDecoder apkDecoder = new ApkDecoder(new File(sTmpDir + File.separator + apk));

        ResTable resTable = apkDecoder.getResTable();
        ResValue value = resTable.getResSpec(0x7f020000).getDefaultResource().getValue();

        assertTrue("Not a ResArrayValue. Found: " + value.getClass(), value instanceof ResArrayValue);
    }
}
