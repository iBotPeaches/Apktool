/**
 *  Copyright 2011 Ryszard Wiśniewski <brut.alll@gmail.com>
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

package brut.androlib.src;

import brut.androlib.AndrolibException;
import brut.androlib.mod.SmaliMod;
import java.io.*;
import org.antlr.runtime.RecognitionException;
import org.jf.dexlib.CodeItem;
import org.jf.dexlib.DexFile;
import org.jf.dexlib.Util.ByteArrayAnnotatedOutput;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public class DexFileBuilder {
	public void addSmaliFile(File smaliFile) throws AndrolibException {
		try {
			addSmaliFile(new FileInputStream(smaliFile),
					smaliFile.getAbsolutePath());
		} catch (FileNotFoundException ex) {
			throw new AndrolibException(ex);
		}
	}

	public void addSmaliFile(InputStream smaliStream, String name)
			throws AndrolibException {
		try {
			if (!SmaliMod.assembleSmaliFile(smaliStream, name, mDexFile, false,
					false, false)) {
				throw new AndrolibException("Could not smali file: " + name);
			}
		} catch (IOException ex) {
			throw new AndrolibException(ex);
		} catch (RecognitionException ex) {
			throw new AndrolibException(ex);
		}
	}

	public void writeTo(File dexFile) throws AndrolibException {
		try {
			OutputStream out = new FileOutputStream(dexFile);
			out.write(getAsByteArray());
			out.close();
		} catch (IOException ex) {
			throw new AndrolibException("Could not write dex to file: "
					+ dexFile, ex);
		}
	}

	public byte[] getAsByteArray() {
		mDexFile.place();
		for (CodeItem codeItem : mDexFile.CodeItemsSection.getItems()) {
			codeItem.fixInstructions(true, true);
		}

		mDexFile.place();

		ByteArrayAnnotatedOutput out = new ByteArrayAnnotatedOutput();
		mDexFile.writeTo(out);
		byte[] bytes = out.toByteArray();

		DexFile.calcSignature(bytes);
		DexFile.calcChecksum(bytes);

		return bytes;
	}

	private final DexFile mDexFile = new DexFile();
}
