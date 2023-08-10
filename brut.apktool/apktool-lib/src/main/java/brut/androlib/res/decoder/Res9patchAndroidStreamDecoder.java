package brut.androlib.res.decoder;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import brut.androlib.exceptions.AndrolibException;
import brut.androlib.exceptions.CantFind9PatchChunkException;
import brut.androlib.res.data.ninepatch.NinePatchData;
import brut.androlib.res.data.ninepatch.OpticalInset;
import brut.util.ExtDataInput;
import org.apache.commons.io.IOUtils;

import java.io.*;

public class Res9patchAndroidStreamDecoder implements ResStreamDecoder {
    public void decode(InputStream in, OutputStream out) throws AndrolibException {
        try {
            byte[] data = IOUtils.toByteArray(in);

            if (data.length == 0) {
                return;
            }
            Bitmap bm = BitmapFactory.decodeByteArray(data, 0, data.length);
            int width = bm.getWidth(), height = bm.getHeight();

            Bitmap outImg = Bitmap.createBitmap(width + 2, height + 2, bm.getConfig());

            for (int w = 0; w < width; w++)
                for (int h = 0; h < height; h++) outImg.setPixel(w + 1, h + 1, bm.getPixel(w, h));

            NinePatchData np = getNinePatch(data);
            drawHLineA(outImg, height + 1, np.padLeft + 1, width - np.padRight);
            drawVLineA(outImg, width + 1, np.padTop + 1, height - np.padBottom);

            int[] xDivs = np.xDivs;
            if (xDivs.length == 0) {
                drawHLineA(outImg, 0, 1, width);
            } else {
                for (int i = 0; i < xDivs.length; i += 2) {
                    drawHLineA(outImg, 0, xDivs[i] + 1, xDivs[i + 1]);
                }
            }

            int[] yDivs = np.yDivs;
            if (yDivs.length == 0) {
                drawVLineA(outImg, 0, 1, height);
            } else {
                for (int i = 0; i < yDivs.length; i += 2) {
                    drawVLineA(outImg, 0, yDivs[i] + 1, yDivs[i + 1]);
                }
            }

            // Some images additionally use Optical Bounds
            // https://developer.android.com/about/versions/android-4.3.html#OpticalBounds
            try {
                OpticalInset oi = getOpticalInset(data);

                for (int i = 0; i < oi.layoutBoundsLeft; i++) {
                    int x = 1 + i;
                    outImg.setPixel(x, height + 1, OI_COLOR);
                }

                for (int i = 0; i < oi.layoutBoundsRight; i++) {
                    int x = width - i;
                    outImg.setPixel(x, height + 1, OI_COLOR);
                }

                for (int i = 0; i < oi.layoutBoundsTop; i++) {
                    int y = 1 + i;
                    outImg.setPixel(width + 1, y, OI_COLOR);
                }

                for (int i = 0; i < oi.layoutBoundsBottom; i++) {
                    int y = height - i;
                    outImg.setPixel(width + 1, y, OI_COLOR);
                }
            } catch (CantFind9PatchChunkException t) {
                // This chunk might not exist
            }

            outImg.compress(Bitmap.CompressFormat.PNG, 100, out);
            bm.recycle();
            outImg.recycle();
        } catch (IOException ex) {
            throw new AndrolibException(ex);
        }
    }

    private NinePatchData getNinePatch(byte[] data) throws AndrolibException,
            IOException {
        ExtDataInput di = new ExtDataInput(new ByteArrayInputStream(data));
        find9patchChunk(di, NP_CHUNK_TYPE);
        return NinePatchData.decode(di);
    }

    private OpticalInset getOpticalInset(byte[] data) throws AndrolibException,
            IOException {
        ExtDataInput di = new ExtDataInput(new ByteArrayInputStream(data));
        find9patchChunk(di, OI_CHUNK_TYPE);
        return OpticalInset.decode(di);
    }

    private void find9patchChunk(DataInput di, int magic) throws AndrolibException,
            IOException {
        di.skipBytes(8);
        while (true) {
            int size;
            try {
                size = di.readInt();
            } catch (IOException ex) {
                throw new CantFind9PatchChunkException("Cant find nine patch chunk", ex);
            }
            if (di.readInt() == magic) {
                return;
            }
            di.skipBytes(size + 4);
        }
    }

    private void drawHLineA(Bitmap bm, int y, int x1, int x2) {
        for (int x = x1; x <= x2; x++) {
            bm.setPixel(x, y, NP_COLOR);
        }
    }

    private void drawVLineA(Bitmap bm, int x, int y1, int y2) {
        for (int y = y1; y <= y2; y++) {
            bm.setPixel(x, y, NP_COLOR);
        }
    }

    private static final int NP_CHUNK_TYPE = 0x6e705463; // npTc
    private static final int OI_CHUNK_TYPE = 0x6e704c62; // npLb
    private static final int NP_COLOR = 0xff000000;
    private static final int OI_COLOR = 0xffff0000;
}
