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

package brut.androlib;

import brut.androlib.err.InFileNotFoundException;
import brut.androlib.err.OutDirExistsException;
import brut.androlib.res.AndrolibResources;
import brut.androlib.res.data.ResPackage;
import brut.androlib.res.data.ResTable;
import brut.androlib.res.util.ExtFile;
import brut.common.BrutException;
import brut.directory.DirectoryException;
import brut.util.OS;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public class ApkDecoder {
	public ApkDecoder() {
		this(new Androlib());
	}

	public ApkDecoder(Androlib androlib) {
		mAndrolib = androlib;
	}

	public ApkDecoder(File apkFile) {
		this(apkFile, new Androlib());
	}

	public ApkDecoder(File apkFile, Androlib androlib) {
		mAndrolib = androlib;
		setApkFile(apkFile);
	}

	public void setApkFile(File apkFile) {
		mApkFile = new ExtFile(apkFile);
		mResTable = null;
	}

	public void setOutDir(File outDir) throws AndrolibException {
		mOutDir = outDir;
	}

	public void decode() throws AndrolibException, IOException {
		File outDir = getOutDir();

		if (!mForceDelete && outDir.exists()) {
			throw new OutDirExistsException();
		}

		if (!mApkFile.isFile() || !mApkFile.canRead()) {
			throw new InFileNotFoundException();
		}

		try {
			OS.rmdir(outDir);
		} catch (BrutException ex) {
			throw new AndrolibException(ex);
		}
		outDir.mkdirs();

		if (hasSources()) {
			switch (mDecodeSources) {
			case DECODE_SOURCES_NONE:
				mAndrolib.decodeSourcesRaw(mApkFile, outDir, mDebug);
				break;
			case DECODE_SOURCES_SMALI:
				mAndrolib.decodeSourcesSmali(mApkFile, outDir, mDebug, mBakDeb);
				break;
			case DECODE_SOURCES_JAVA:
				mAndrolib.decodeSourcesJava(mApkFile, outDir, mDebug);
				break;
			}
		}

		if (hasResources()) {

			// read the resources.arsc checking for STORED vs DEFLATE
			// compression
			// this will determine whether we compress on rebuild or not.
			JarFile jf = new JarFile(mApkFile.getAbsoluteFile());
			JarEntry je = jf.getJarEntry("resources.arsc");
			if (je != null) {
				int compression = je.getMethod();
				mCompressResources = (compression != ZipEntry.STORED)
						&& (compression == ZipEntry.DEFLATED);
			}
			jf.close();

			switch (mDecodeResources) {
			case DECODE_RESOURCES_NONE:
				mAndrolib.decodeResourcesRaw(mApkFile, outDir);
				break;
			case DECODE_RESOURCES_FULL:
				mAndrolib.decodeResourcesFull(mApkFile, outDir, getResTable());
				break;
			}
		} else {
			// if there's no resources.asrc, decode the manifest without looking
			// up attribute references
			if (hasManifest()) {
				switch (mDecodeResources) {
				case DECODE_RESOURCES_NONE:
					mAndrolib.decodeManifestRaw(mApkFile, outDir);
					break;
				case DECODE_RESOURCES_FULL:
					mAndrolib.decodeManifestFull(mApkFile, outDir,
							getResTable());
					break;
				}
			}
		}

		mAndrolib.decodeRawFiles(mApkFile, outDir);
		writeMetaFile();
	}

	public void setDecodeSources(short mode) throws AndrolibException {
		if (mode != DECODE_SOURCES_NONE && mode != DECODE_SOURCES_SMALI
				&& mode != DECODE_SOURCES_JAVA) {
			throw new AndrolibException("Invalid decode sources mode: " + mode);
		}
		mDecodeSources = mode;
	}

	public void setDecodeResources(short mode) throws AndrolibException {
		if (mode != DECODE_RESOURCES_NONE && mode != DECODE_RESOURCES_FULL) {
			throw new AndrolibException("Invalid decode resources mode");
		}
		mDecodeResources = mode;
	}

	public void setDebugMode(boolean debug) {
		mDebug = debug;
	}

	public void setBaksmaliDebugMode(boolean bakdeb) {
		mBakDeb = bakdeb;
	}

	public void setForceDelete(boolean forceDelete) {
		mForceDelete = forceDelete;
	}

	public void setFrameworkTag(String tag) throws AndrolibException {
		mFrameTag = tag;
		if (mResTable != null) {
			getResTable().setFrameTag(tag);
		}
	}

	public void setKeepBrokenResources(boolean keepBrokenResources) {
		mKeepBrokenResources = keepBrokenResources;
	}

	public void setFrameworkDir(String dir) {
		mFrameworkDir = dir;
	}

	public ResTable getResTable() throws AndrolibException {
		if (mResTable == null) {
			boolean hasResources = hasResources();
			boolean hasManifest = hasManifest();
			if (!(hasManifest || hasResources)) {
				throw new AndrolibException(
						"Apk doesn't contain either AndroidManifest.xml file or resources.arsc file");
			}
			AndrolibResources.sKeepBroken = mKeepBrokenResources;
			AndrolibResources.sFrameworkFolder = mFrameworkDir;
			mResTable = mAndrolib.getResTable(mApkFile, hasResources);
			mResTable.setFrameTag(mFrameTag);
		}
		return mResTable;
	}

	public boolean hasSources() throws AndrolibException {
		try {
			return mApkFile.getDirectory().containsFile("classes.dex");
		} catch (DirectoryException ex) {
			throw new AndrolibException(ex);
		}
	}

	public boolean hasManifest() throws AndrolibException {
		try {
			return mApkFile.getDirectory().containsFile("AndroidManifest.xml");
		} catch (DirectoryException ex) {
			throw new AndrolibException(ex);
		}
	}

	public boolean hasResources() throws AndrolibException {
		try {
			return mApkFile.getDirectory().containsFile("resources.arsc");
		} catch (DirectoryException ex) {
			throw new AndrolibException(ex);
		}
	}

	public final static short DECODE_SOURCES_NONE = 0x0000;
	public final static short DECODE_SOURCES_SMALI = 0x0001;
	public final static short DECODE_SOURCES_JAVA = 0x0002;

	public final static short DECODE_RESOURCES_NONE = 0x0100;
	public final static short DECODE_RESOURCES_FULL = 0x0101;

	private File getOutDir() throws AndrolibException {
		if (mOutDir == null) {
			throw new AndrolibException("Out dir not set");
		}
		return mOutDir;
	}

	private void writeMetaFile() throws AndrolibException {
		Map<String, Object> meta = new LinkedHashMap<String, Object>();
		meta.put("version", Androlib.getVersion());
		meta.put("apkFileName", mApkFile.getName());

		if (mDecodeResources != DECODE_RESOURCES_NONE
				&& (hasManifest() || hasResources())) {
			meta.put("isFrameworkApk",
					Boolean.valueOf(mAndrolib.isFrameworkApk(getResTable())));
			putUsesFramework(meta);
			putSdkInfo(meta);
			putPackageInfo(meta);
			putCompressionInfo(meta);
		}

		mAndrolib.writeMetaFile(mOutDir, meta);
	}

	private void putUsesFramework(Map<String, Object> meta)
			throws AndrolibException {
		Set<ResPackage> pkgs = getResTable().listFramePackages();
		if (pkgs.isEmpty()) {
			return;
		}

		Integer[] ids = new Integer[pkgs.size()];
		int i = 0;
		for (ResPackage pkg : pkgs) {
			ids[i++] = pkg.getId();
		}
		Arrays.sort(ids);

		Map<String, Object> uses = new LinkedHashMap<String, Object>();
		uses.put("ids", ids);

		if (mFrameTag != null) {
			uses.put("tag", mFrameTag);
		}

		meta.put("usesFramework", uses);
	}

	private void putSdkInfo(Map<String, Object> meta) throws AndrolibException {
		Map<String, String> info = getResTable().getSdkInfo();
		if (info.size() > 0) {
			meta.put("sdkInfo", info);
		}
	}

	private void putPackageInfo(Map<String, Object> meta)
			throws AndrolibException {
		Map<String, String> info = getResTable().getPackageInfo();
		if (info.size() > 0) {
			meta.put("packageInfo", info);
		}
	}

	private void putCompressionInfo(Map<String, Object> meta)
			throws AndrolibException {
		meta.put("compressionType", getCompressionType());
	}

	private boolean getCompressionType() {
		return mCompressResources;
	}

	private final Androlib mAndrolib;

	private ExtFile mApkFile;
	private File mOutDir;
	private ResTable mResTable;
	private short mDecodeSources = DECODE_SOURCES_SMALI;
	private short mDecodeResources = DECODE_RESOURCES_FULL;
	private boolean mDebug = false;
	private boolean mForceDelete = false;
	private String mFrameTag;
	private boolean mKeepBrokenResources = false;
	private String mFrameworkDir = null;
	private boolean mBakDeb = true;
	private boolean mCompressResources = false;
}
