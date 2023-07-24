package brut.androlib.apk;

import brut.androlib.exceptions.AndrolibException;
import org.junit.Test;

import static org.junit.Assert.*;

public class DoNotCompressHieroglyphTest {

    @Test
    public void testHieroglyph() throws AndrolibException {
        ApkInfo apkInfo = ApkInfo.load(
            this.getClass().getResourceAsStream("/apk/donotcompress_with_hieroglyph.yml"));
        assertEquals("2.0.0", apkInfo.version);
        assertEquals("testapp.apk", apkInfo.getApkFileName());
        assertEquals(2, apkInfo.doNotCompress.size());
        assertEquals("assets/AllAssetBundles/Andriod/tx_1001_冰原1", apkInfo.doNotCompress.get(0));
        assertEquals("assets/AllAssetBundles/Andriod/tx_1001_冰原1.manifest", apkInfo.doNotCompress.get(1));
    }
}
