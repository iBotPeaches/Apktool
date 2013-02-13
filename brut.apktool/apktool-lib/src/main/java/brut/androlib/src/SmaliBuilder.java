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
import brut.androlib.res.util.ExtFile;
import brut.directory.DirectoryException;
import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public class SmaliBuilder {

	public static void build(ExtFile smaliDir, File dexFile,
			HashMap<String, Boolean> flags) throws AndrolibException {
		new SmaliBuilder(smaliDir, dexFile, flags).build();
	}

	private SmaliBuilder(ExtFile smaliDir, File dexFile,
			HashMap<String, Boolean> flags) {
		mSmaliDir = smaliDir;
		mDexFile = dexFile;
		mFlags = flags;
	}

	private void build() throws AndrolibException {
		try {
			mDexBuilder = new DexFileBuilder();
			for (String fileName : mSmaliDir.getDirectory().getFiles(true)) {
				buildFile(fileName);
			}
			mDexBuilder.writeTo(mDexFile);
		} catch (IOException ex) {
			throw new AndrolibException(ex);
		} catch (DirectoryException ex) {
			throw new AndrolibException(ex);
		}
	}

	private void buildFile(String fileName) throws AndrolibException,
			IOException {
		File inFile = new File(mSmaliDir, fileName);
		InputStream inStream = new FileInputStream(inFile);

		if (fileName.endsWith(".smali")) {
			mDexBuilder.addSmaliFile(inFile);
			return;
		}
		if (!fileName.endsWith(".java")) {
			LOGGER.warning("Unknown file type, ignoring: " + inFile);
			return;
		}

		StringBuilder out = new StringBuilder();
		List<String> lines = IOUtils.readLines(inStream);

		if (!mFlags.containsKey("debug")) {
			final String[] linesArray = lines.toArray(new String[0]);
			for (int i = 2; i < linesArray.length - 2; i++) {
				out.append(linesArray[i]).append('\n');
			}
		} else {
			lines.remove(lines.size() - 1);
			lines.remove(lines.size() - 1);
			ListIterator<String> it = lines.listIterator(2);

			out.append(".source \"").append(inFile.getName()).append("\"\n");
			while (it.hasNext()) {
				String line = it.next().trim();
				if (line.isEmpty() || line.charAt(0) == '#'
						|| line.startsWith(".source")) {
					continue;
				}
				if (line.startsWith(".method ")) {
					it.previous();
					DebugInjector.inject(it, out);
					continue;
				}

				out.append(line).append('\n');
			}
		}
		mDexBuilder.addSmaliFile(IOUtils.toInputStream(out.toString()),
				fileName);
	}

	private final ExtFile mSmaliDir;
	private final File mDexFile;
	private final HashMap<String, Boolean> mFlags;

	private DexFileBuilder mDexBuilder;

	private final static Logger LOGGER = Logger.getLogger(SmaliBuilder.class
			.getName());
}
