/**
 *  Copyright (C) 2018 Ryszard Wiśniewski <brut.alll@gmail.com>
 *  Copyright (C) 2018 Connor Tumbleson <connor.tumbleson@gmail.com>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package brut.androlib.res.decoder;

import brut.androlib.AndrolibException;
import brut.androlib.err.CantFind9PatchChunk;
import brut.util.ExtDataInput;
import java.io.*;
import org.apache.commons.io.IOUtils;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public class Res9patchStreamDecoder implements ResStreamDecoder {
	@Override
	public void decode(InputStream in, OutputStream out) throws AndrolibException {
		try {
			byte[] data = IOUtils.toByteArray(in);
			out.write(data);
		} catch (IOException ex) {
			throw new AndrolibException(ex);
		}
	}

	private NinePatch getNinePatch(byte[] data) throws AndrolibException, IOException {
		ExtDataInput di = new ExtDataInput(new ByteArrayInputStream(data));
		find9patchChunk(di, NP_CHUNK_TYPE);
		return NinePatch.decode(di);
	}

	private void find9patchChunk(DataInput di, int magic) throws AndrolibException, IOException {
		di.skipBytes(8);
		while (true) {
			int size;
			try {
				size = di.readInt();
			} catch (IOException ex) {
				throw new CantFind9PatchChunk("Cant find nine patch chunk", ex);
			}
			int type = di.readInt();
			if (NP_CHUNK_TYPE == type || OI_CHUNK_TYPE == type) {
				return;
			}
			di.skipBytes(size + 4);
		}
	}

	private static final int NP_CHUNK_TYPE = 0x6e705463; // npTc
	private static final int OI_CHUNK_TYPE = 0x6e704c62; // npLb

	private static class NinePatch {
		public final int padLeft, padRight, padTop, padBottom;
		public final int[] xDivs, yDivs, colors;

		public NinePatch(int padLeft, int padRight, int padTop, int padBottom, int[] xDivs, int[] yDivs, int[] colors) {
			this.padLeft = padLeft;
			this.padRight = padRight;
			this.padTop = padTop;
			this.padBottom = padBottom;
			this.xDivs = xDivs;
			this.yDivs = yDivs;
			this.colors = colors;
		}

		public static NinePatch decode(ExtDataInput di) throws IOException {
			di.skipBytes(1); // wasDeserialized

			byte numXDivs = di.readByte();
			byte numYDivs = di.readByte();
			byte numColors = di.readByte();
			System.out.println("numXDivs:" + numXDivs + "  numYDivs:" + numYDivs + "  numColors:" + numColors);

			di.skipBytes(8); // xDivs/yDivs offset

			int padLeft = di.readInt();
			int padRight = di.readInt();
			int padTop = di.readInt();
			int padBottom = di.readInt();
			di.skipBytes(4); // colorsOffset
			int[] xDivs = di.readIntArray(numXDivs);
			int[] yDivs = di.readIntArray(numYDivs);
			int[] colors = di.readIntArray(numColors);

			return new NinePatch(padLeft, padRight, padTop, padBottom, xDivs, yDivs, colors);
		}
	}
}
