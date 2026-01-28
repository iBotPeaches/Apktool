/*
 *  Copyright (C) 2010 Ryszard Wiśniewski <brut.alll@gmail.com>
 *  Copyright (C) 2010 Connor Tumbleson <connor.tumbleson@gmail.com>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package brut.androlib.res.decoder;

import brut.androlib.exceptions.AndrolibException;
import brut.androlib.exceptions.NinePatchNotFoundException;
import brut.androlib.res.data.LayoutBounds;
import brut.androlib.res.data.NinePatchData;
import brut.util.BinaryDataInputStream;
import org.apache.commons.io.IOUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteOrder;

public class ResNinePatchStreamDecoder implements ResStreamDecoder {

    @Override
    public void decode(InputStream in, OutputStream out) throws AndrolibException {
        try {
            byte[] data = IOUtils.toByteArray(in);
            if (data.length == 0) {
                return;
            }

            BufferedImage src = ImageIO.read(new ByteArrayInputStream(data));
            int w = src.getWidth(), h = src.getHeight();

            BufferedImage dst = new BufferedImage(w + 2, h + 2, BufferedImage.TYPE_INT_ARGB);
            if (src.getType() == BufferedImage.TYPE_CUSTOM) {
                Raster srcRaster = src.getRaster();
                WritableRaster dstRaster = dst.getRaster();
                int[] gray = null, alpha = null;
                for (int y = 0; y < src.getHeight(); y++) {
                    gray = srcRaster.getSamples(0, y, w, 1, 0, gray);
                    alpha = srcRaster.getSamples(0, y, w, 1, 1, alpha);

                    dstRaster.setSamples(1, y + 1, w, 1, 0, gray);
                    dstRaster.setSamples(1, y + 1, w, 1, 1, gray);
                    dstRaster.setSamples(1, y + 1, w, 1, 2, gray);
                    dstRaster.setSamples(1, y + 1, w, 1, 3, alpha);
                }
            } else {
                dst.createGraphics().drawImage(src, 1, 1, w, h, null);
            }

            NinePatchData np = findNinePatchData(data);
            drawHLine(dst, h + 1, np.paddingLeft + 1, w - np.paddingRight);
            drawVLine(dst, w + 1, np.paddingTop + 1, h - np.paddingBottom);

            int[] xDivs = np.xDivs;
            if (xDivs.length == 0) {
                drawHLine(dst, 0, 1, w);
            } else {
                for (int i = 0; i < xDivs.length; i += 2) {
                    drawHLine(dst, 0, xDivs[i] + 1, xDivs[i + 1]);
                }
            }

            int[] yDivs = np.yDivs;
            if (yDivs.length == 0) {
                drawVLine(dst, 0, 1, h);
            } else {
                for (int i = 0; i < yDivs.length; i += 2) {
                    drawVLine(dst, 0, yDivs[i] + 1, yDivs[i + 1]);
                }
            }

            // Some images optionally use optical inset/layout bounds.
            // https://developer.android.com/about/versions/android-4.3.html#OpticalBounds
            try {
                LayoutBounds lb = findLayoutBounds(data);

                for (int i = 0; i < lb.left; i++) {
                    int x = 1 + i;
                    dst.setRGB(x, h + 1, LayoutBounds.COLOR_TICK);
                }

                for (int i = 0; i < lb.right; i++) {
                    int x = w - i;
                    dst.setRGB(x, h + 1, LayoutBounds.COLOR_TICK);
                }

                for (int i = 0; i < lb.top; i++) {
                    int y = 1 + i;
                    dst.setRGB(w + 1, y, LayoutBounds.COLOR_TICK);
                }

                for (int i = 0; i < lb.bottom; i++) {
                    int y = h - i;
                    dst.setRGB(w + 1, y, LayoutBounds.COLOR_TICK);
                }
            } catch (NinePatchNotFoundException ignored) {
                // This chunk might not exist.
            }

            ImageIO.write(dst, "png", out);
        } catch (IOException | NullPointerException ex) {
            // The file is not a valid image.
            throw new AndrolibException(ex);
        }
    }

    private NinePatchData findNinePatchData(byte[] data)
            throws NinePatchNotFoundException, IOException {
        BinaryDataInputStream in = new BinaryDataInputStream(data, ByteOrder.BIG_ENDIAN);
        findChunk(in, NinePatchData.MAGIC);
        return NinePatchData.read(in);
    }

    private LayoutBounds findLayoutBounds(byte[] data)
            throws NinePatchNotFoundException, IOException {
        BinaryDataInputStream in = new BinaryDataInputStream(data, ByteOrder.BIG_ENDIAN);
        findChunk(in, LayoutBounds.MAGIC);
        return LayoutBounds.read(in);
    }

    private void findChunk(BinaryDataInputStream in, int magic)
            throws NinePatchNotFoundException, IOException {
        in.skipBytes(8);
        for (;;) {
            int size;
            try {
                size = in.readInt();
            } catch (EOFException ignored) {
                throw new NinePatchNotFoundException();
            }
            if (in.readInt() == magic) {
                return;
            }
            in.skipBytes(size + 4);
        }
    }

    private void drawHLine(BufferedImage im, int y, int x1, int x2) {
        for (int x = x1; x <= x2; x++) {
            im.setRGB(x, y, NinePatchData.COLOR_TICK);
        }
    }

    private void drawVLine(BufferedImage im, int x, int y1, int y2) {
        for (int y = y1; y <= y2; y++) {
            im.setRGB(x, y, NinePatchData.COLOR_TICK);
        }
    }
}
