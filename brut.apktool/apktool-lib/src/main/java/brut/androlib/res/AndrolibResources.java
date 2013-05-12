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

package brut.androlib.res;

import brut.androlib.AndrolibException;
import brut.androlib.err.CantFindFrameworkResException;
import brut.androlib.res.data.*;
import brut.androlib.res.decoder.*;
import brut.androlib.res.decoder.ARSCDecoder.ARSCData;
import brut.androlib.res.decoder.ARSCDecoder.FlagsOffset;
import brut.androlib.res.util.*;
import brut.androlib.res.xml.ResValuesXmlSerializable;
import brut.common.BrutException;
import brut.directory.*;
import brut.util.*;
import java.io.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.zip.*;

import java.io.File;
import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.apache.commons.io.IOUtils;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlSerializer;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
final public class AndrolibResources {
	public ResTable getResTable(ExtFile apkFile) throws AndrolibException {
		return getResTable(apkFile, true);
	}

	public ResTable getResTable(ExtFile apkFile, boolean loadMainPkg)
			throws AndrolibException {
		ResTable resTable = new ResTable(this);
		if (loadMainPkg) {
			loadMainPkg(resTable, apkFile);
		}
		return resTable;
	}

	public ResPackage loadMainPkg(ResTable resTable, ExtFile apkFile)
			throws AndrolibException {
		LOGGER.info("Loading resource table...");
		ResPackage[] pkgs = getResPackagesFromApk(apkFile, resTable,
				sKeepBroken);
		ResPackage pkg = null;

		switch (pkgs.length) {
		case 1:
			pkg = pkgs[0];
			break;
		case 2:
			if (pkgs[0].getName().equals("android")) {
				LOGGER.warning("Skipping \"android\" package group");
				pkg = pkgs[1];
			} else if (pkgs[0].getName().equals("com.htc")) {
				LOGGER.warning("Skipping \"htc\" package group");
				pkg = pkgs[1];
			}
			break;
		}

		if (pkg == null) {
			throw new AndrolibException(
					"Arsc files with zero or multiple packages");
		}

		resTable.addPackage(pkg, true);
		LOGGER.info("Loaded.");
		return pkg;
	}

	public ResPackage loadFrameworkPkg(ResTable resTable, int id,
			String frameTag) throws AndrolibException {
		File apk = getFrameworkApk(id, frameTag);

		LOGGER.info("Loading resource table from file: " + apk);
		ResPackage[] pkgs = getResPackagesFromApk(new ExtFile(apk), resTable,
				true);

		if (pkgs.length != 1) {
			throw new AndrolibException(
					"Arsc files with zero or multiple packages");
		}

		ResPackage pkg = pkgs[0];
		if (pkg.getId() != id) {
			throw new AndrolibException("Expected pkg of id: "
					+ String.valueOf(id) + ", got: " + pkg.getId());
		}

		resTable.addPackage(pkg, false);
		LOGGER.info("Loaded.");
		return pkg;
	}

	public void decodeManifest(ResTable resTable, ExtFile apkFile, File outDir)
			throws AndrolibException {

		Duo<ResFileDecoder, AXmlResourceParser> duo = getManifestFileDecoder();
		ResFileDecoder fileDecoder = duo.m1;

		// Set ResAttrDecoder
		duo.m2.setAttrDecoder(new ResAttrDecoder());
		ResAttrDecoder attrDecoder = duo.m2.getAttrDecoder();

		// Fake ResPackage
		attrDecoder.setCurrentPackage(new ResPackage(resTable, 0, null));

		Directory inApk, out;
		try {
			inApk = apkFile.getDirectory();
			out = new FileDirectory(outDir);

			LOGGER.info("Decoding AndroidManifest.xml with only framework resources...");
			fileDecoder.decodeManifest(inApk, "AndroidManifest.xml", out,
					"AndroidManifest.xml");

		} catch (DirectoryException ex) {
			throw new AndrolibException(ex);
		}
	}

	public void remove_application_debug(String filePath)
		throws AndrolibException {
		
		// change application:debug to true
		try {
			DocumentBuilderFactory docFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			Document doc = docBuilder.parse(filePath.toString());
		
			Node application = doc.getElementById("application");
			
			// load attr
			NamedNodeMap attr = application.getAttributes();
			Node debugAttr = attr.getNamedItem("debug");
			
			// remove application:debug
			if (debugAttr != null) {
				attr.removeNamedItem("debug");
			}
			
			// save manifest
			TransformerFactory transformerFactory = TransformerFactory
					.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(new File(filePath));
			transformer.transform(source, result);

        } catch (SAXException | ParserConfigurationException | IOException | TransformerException ex) {
            throw new AndrolibException(ex);
        }
    }

	public void adjust_package_manifest(ResTable resTable, String filePath)
			throws AndrolibException {

		// check if packages different, and that package is not equal to
		// "android"
		Map<String, String> packageInfo = resTable.getPackageInfo();
		if ((packageInfo.get("cur_package").equalsIgnoreCase(packageInfo.get("orig_package")) || 
		    ("android".equalsIgnoreCase(packageInfo.get("cur_package")) || 
		        ("com.htc".equalsIgnoreCase(packageInfo.get("cur_package")))))) {

			LOGGER.info("Regular manifest package...");
		} else {
			try {

				LOGGER.info("Renamed manifest package found! Fixing...");
				DocumentBuilderFactory docFactory = DocumentBuilderFactory
						.newInstance();
				DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
				Document doc = docBuilder.parse(filePath.toString());

				// Get the manifest line
				Node manifest = doc.getFirstChild();

				// update package attribute
				NamedNodeMap attr = manifest.getAttributes();
				Node nodeAttr = attr.getNamedItem("package");
				mPackageRenamed = nodeAttr.getNodeValue();
				nodeAttr.setNodeValue(packageInfo.get("cur_package"));

				// re-save manifest.
				TransformerFactory transformerFactory = TransformerFactory
						.newInstance();
				Transformer transformer = transformerFactory.newTransformer();
				DOMSource source = new DOMSource(doc);
				StreamResult result = new StreamResult(new File(filePath));
				transformer.transform(source, result);

            } catch (SAXException | ParserConfigurationException | IOException | TransformerException ex) {
                throw new AndrolibException(ex);
            }
        }
	}

    public void remove_manifest_versions(String filePath)
            throws AndrolibException {

        File f = new File(filePath);

        if (f.exists()) {
            // remove versionCode and versionName
            try {
                DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
                Document doc = docBuilder.parse(filePath.toString());

                Node manifest = doc.getFirstChild();

                // load attr
                NamedNodeMap attr = manifest.getAttributes();
                Node vCode = attr.getNamedItem("android:versionCode");
                Node vName = attr.getNamedItem("android:versionName");

                // remove versionCode
                if (vCode != null) {
                    attr.removeNamedItem("android:versionCode");
                }
                if (vName != null) {
                    attr.removeNamedItem("android:versionName");
                }

                // save manifest
                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                Transformer transformer = transformerFactory.newTransformer();
                DOMSource source = new DOMSource(doc);
                StreamResult result = new StreamResult(new File(filePath));
                transformer.transform(source, result);

            } catch (SAXException | ParserConfigurationException | IOException | TransformerException ex) {
                throw new AndrolibException(ex);
            }
        }
    }

	public void decode(ResTable resTable, ExtFile apkFile, File outDir)
			throws AndrolibException {
		Duo<ResFileDecoder, AXmlResourceParser> duo = getResFileDecoder();
		ResFileDecoder fileDecoder = duo.m1;
		ResAttrDecoder attrDecoder = duo.m2.getAttrDecoder();

		attrDecoder.setCurrentPackage(resTable.listMainPackages().iterator()
				.next());

		Directory inApk, in = null, out;
		try {
			inApk = apkFile.getDirectory();
			out = new FileDirectory(outDir);

			LOGGER.info("Decoding AndroidManifest.xml with resources...");

			fileDecoder.decodeManifest(inApk, "AndroidManifest.xml", out,
					"AndroidManifest.xml");

			// fix package (Android 4.2)
			adjust_package_manifest(resTable, outDir.getAbsolutePath()
					+ File.separator + "AndroidManifest.xml");

            // Remove versionName / versionCode (aapt API 16)
            remove_manifest_versions(outDir.getAbsolutePath()
                    + File.separator + "/AndroidManifest.xml");

			if (inApk.containsDir("res")) {
				in = inApk.getDir("res");
			}
			out = out.createDir("res");
		} catch (DirectoryException ex) {
			throw new AndrolibException(ex);
		}

		ExtMXSerializer xmlSerializer = getResXmlSerializer();
		for (ResPackage pkg : resTable.listMainPackages()) {
			attrDecoder.setCurrentPackage(pkg);

			LOGGER.info("Decoding file-resources...");
			for (ResResource res : pkg.listFiles()) {
				fileDecoder.decode(res, in, out);
			}

			LOGGER.info("Decoding values */* XMLs...");
			for (ResValuesFile valuesFile : pkg.listValuesFiles()) {
				generateValuesFile(valuesFile, out, xmlSerializer);
			}
			generatePublicXml(pkg, out, xmlSerializer);
			LOGGER.info("Done.");
		}

		AndrolibException decodeError = duo.m2.getFirstError();
		if (decodeError != null) {
			throw decodeError;
		}
	}

	public void setSdkInfo(Map<String, String> map) {
		if (map != null) {
			mMinSdkVersion = map.get("minSdkVersion");
			mTargetSdkVersion = map.get("targetSdkVersion");
			mMaxSdkVersion = map.get("maxSdkVersion");
		}
	}
	
	public void setVersionInfo(Map<String, String> map) {
	  if (map != null) {
	    mVersionCode = map.get("versionCode").toString();
	    mVersionName = map.get("versionName").toString();
	  }
	}

	public void setPackageInfo(Map<String, String> map) {
		if (map != null) {
			mPackageRenamed = map.get("package");
		}
	}

	public void setPackageId(Map<String, String> map) {
        if (map != null) {
            mPackageId = map.get("cur_package_id");
        }
	}

    public void aaptPackage(File apkFile, File manifest, File resDir,
                            File rawDir, File assetDir, File[] include,
                            HashMap<String, Boolean> flags, String aaptPath)
            throws AndrolibException {

        boolean customAapt = false;
        List<String> cmd = new ArrayList<String>();

        // path for aapt binary
        if (!aaptPath.isEmpty()) {
            File aaptFile = new File(aaptPath);
            if (aaptFile.canRead() && aaptFile.exists()) {
                aaptFile.setExecutable(true);
                cmd.add(aaptFile.getPath());
                customAapt = true;

                if (flags.get("verbose")) {
                    LOGGER.info(aaptFile.getPath()
                            + " being used as aapt location.");
                }
            } else {
                LOGGER.warning("aapt location could not be found. Defaulting back to default");

                try {
                    cmd.add(getAaptBinaryFile().getAbsolutePath());
                } catch (BrutException ignored) {
                    cmd.add("aapt");
                }
            }
        } else {
            try {
                cmd.add(getAaptBinaryFile().getAbsolutePath());
            } catch (BrutException ignored) {
                cmd.add("aapt");
            }
        }

		cmd.add("p");

		if (flags.get("verbose")) { // output aapt verbose
			cmd.add("-v");
		}
		if (flags.get("update")) {
			cmd.add("-u");
		}
		if (flags.get("debug")) { // inject debuggable="true" into manifest
			cmd.add("--debug-mode");
		}

        // force package id so that some frameworks build with correct id
        // disable if user adds own aapt (can't know if they have this feature)
		if (mPackageId != null && customAapt == false) {
			cmd.add("--forced-package-id");
			cmd.add(mPackageId);
		}
		if (mMinSdkVersion != null) {
			cmd.add("--min-sdk-version");
			cmd.add(mMinSdkVersion);
		}
		if (mTargetSdkVersion != null) {
			cmd.add("--target-sdk-version");
			cmd.add(mTargetSdkVersion);
		}
		if (mMaxSdkVersion != null) {
			cmd.add("--max-sdk-version");
			cmd.add(mMaxSdkVersion);
			
			// if we have max sdk version, set --max-res-version
			// so we can ignore anything over that during build.
			cmd.add("--max-res-version");
			cmd.add(mMaxSdkVersion);
		}
		if (mPackageRenamed != null) {
			cmd.add("--rename-manifest-package");
			cmd.add(mPackageRenamed);
		}
		if (mVersionCode != null) {
		  cmd.add("--version-code");
		  cmd.add(mVersionCode);
		}
		if (mVersionName != null) {
		  cmd.add("--version-name");
		  cmd.add(mVersionName);
		}
		cmd.add("-F");
		cmd.add(apkFile.getAbsolutePath());

		if (flags.get("framework")) {
			cmd.add("-x");
		}

		if (!(flags.get("compression"))) {
			cmd.add("-0");
			cmd.add("arsc");
		}

		if (include != null) {
			for (File file : include) {
				cmd.add("-I");
				cmd.add(file.getPath());
			}
		}
		if (resDir != null) {
			cmd.add("-S");
			cmd.add(resDir.getAbsolutePath());
		}
		if (manifest != null) {
			cmd.add("-M");
			cmd.add(manifest.getAbsolutePath());
		}
		if (assetDir != null) {
			cmd.add("-A");
			cmd.add(assetDir.getAbsolutePath());
		}
		if (rawDir != null) {
			cmd.add(rawDir.getAbsolutePath());
		}
		try {
			OS.exec(cmd.toArray(new String[0]));
            if (flags.get("verbose")) {
                LOGGER.info("command ran: ");
                LOGGER.info(cmd.toString());
            }
		} catch (BrutException ex) {
			throw new AndrolibException(ex);
		}
	}

	public boolean detectWhetherAppIsFramework(File appDir)
			throws AndrolibException {
		File publicXml = new File(appDir, "res/values/public.xml");
		if (!publicXml.exists()) {
			return false;
		}

		Iterator<String> it;
		try {
			it = IOUtils.lineIterator(new FileReader(new File(appDir,
					"res/values/public.xml")));
		} catch (FileNotFoundException ex) {
			throw new AndrolibException(
					"Could not detect whether app is framework one", ex);
		}
		it.next();
		it.next();
		return it.next().contains("0x01");
	}

	public void tagSmaliResIDs(ResTable resTable, File smaliDir)
			throws AndrolibException {
		new ResSmaliUpdater().tagResIDs(resTable, smaliDir);
	}

	public void updateSmaliResIDs(ResTable resTable, File smaliDir)
			throws AndrolibException {
		new ResSmaliUpdater().updateResIDs(resTable, smaliDir);
	}

	public Duo<ResFileDecoder, AXmlResourceParser> getResFileDecoder() {
		ResStreamDecoderContainer decoders = new ResStreamDecoderContainer();
		decoders.setDecoder("raw", new ResRawStreamDecoder());
		decoders.setDecoder("9patch", new Res9patchStreamDecoder());

		AXmlResourceParser axmlParser = new AXmlResourceParser();
		axmlParser.setAttrDecoder(new ResAttrDecoder());
		decoders.setDecoder("xml", new XmlPullStreamDecoder(axmlParser,
				getResXmlSerializer()));

		return new Duo<ResFileDecoder, AXmlResourceParser>(new ResFileDecoder(
				decoders), axmlParser);
	}

	public Duo<ResFileDecoder, AXmlResourceParser> getManifestFileDecoder() {
		ResStreamDecoderContainer decoders = new ResStreamDecoderContainer();

		AXmlResourceParser axmlParser = new AXmlResourceParser();

		decoders.setDecoder("xml", new XmlPullStreamDecoder(axmlParser,
				getResXmlSerializer()));

		return new Duo<ResFileDecoder, AXmlResourceParser>(new ResFileDecoder(
				decoders), axmlParser);
	}

	public ExtMXSerializer getResXmlSerializer() {
		ExtMXSerializer serial = new ExtMXSerializer();
		serial.setProperty(ExtXmlSerializer.PROPERTY_SERIALIZER_INDENTATION,
				"    ");
		serial.setProperty(ExtXmlSerializer.PROPERTY_SERIALIZER_LINE_SEPARATOR,
				System.getProperty("line.separator"));
		serial.setProperty(ExtXmlSerializer.PROPERTY_DEFAULT_ENCODING, "utf-8");
		serial.setDisabledAttrEscape(true);
		return serial;
	}

	private void generateValuesFile(ResValuesFile valuesFile, Directory out,
			ExtXmlSerializer serial) throws AndrolibException {
		try {
			OutputStream outStream = out.getFileOutput(valuesFile.getPath());
			serial.setOutput((outStream), null);
			serial.startDocument(null, null);
			serial.startTag(null, "resources");

			for (ResResource res : valuesFile.listResources()) {
				if (valuesFile.isSynthesized(res)) {
					continue;
				}
				((ResValuesXmlSerializable) res.getValue())
						.serializeToResValuesXml(serial, res);
			}

			serial.endTag(null, "resources");
			serial.newLine();
			serial.endDocument();
			serial.flush();
			outStream.close();
		} catch (IOException ex) {
			throw new AndrolibException("Could not generate: "
					+ valuesFile.getPath(), ex);
		} catch (DirectoryException ex) {
			throw new AndrolibException("Could not generate: "
					+ valuesFile.getPath(), ex);
		}
	}

	private void generatePublicXml(ResPackage pkg, Directory out,
			XmlSerializer serial) throws AndrolibException {
		try {
			OutputStream outStream = out.getFileOutput("values/public.xml");
			serial.setOutput(outStream, null);
			serial.startDocument(null, null);
			serial.startTag(null, "resources");

			for (ResResSpec spec : pkg.listResSpecs()) {
				serial.startTag(null, "public");
				serial.attribute(null, "type", spec.getType().getName());
				serial.attribute(null, "name", spec.getName());
				serial.attribute(null, "id",
						String.format("0x%08x", spec.getId().id));
				serial.endTag(null, "public");
			}

			serial.endTag(null, "resources");
			serial.endDocument();
			serial.flush();
			outStream.close();
		} catch (IOException ex) {
			throw new AndrolibException("Could not generate public.xml file",
					ex);
		} catch (DirectoryException ex) {
			throw new AndrolibException("Could not generate public.xml file",
					ex);
		}
	}

	private ResPackage[] getResPackagesFromApk(ExtFile apkFile,
			ResTable resTable, boolean keepBroken) throws AndrolibException {
		try {
			return ARSCDecoder.decode(
					apkFile.getDirectory().getFileInput("resources.arsc"),
					false, keepBroken, resTable).getPackages();
		} catch (DirectoryException ex) {
			throw new AndrolibException(
					"Could not load resources.arsc from file: " + apkFile, ex);
		}
	}

	public File getFrameworkApk(int id, String frameTag)
			throws AndrolibException {
		File dir = getFrameworkDir();
		File apk;

		if (frameTag != null) {
			apk = new File(dir, String.valueOf(id) + '-' + frameTag + ".apk");
			if (apk.exists()) {
				return apk;
			}
		}

		apk = new File(dir, String.valueOf(id) + ".apk");
		if (apk.exists()) {
			return apk;
		}

		if (id == 1) {
			InputStream in = null;
			OutputStream out = null;
			try {
				in = AndrolibResources.class
						.getResourceAsStream("/brut/androlib/android-framework.jar");
				out = new FileOutputStream(apk);
				IOUtils.copy(in, out);
				return apk;
			} catch (IOException ex) {
				throw new AndrolibException(ex);
			} finally {
				if (in != null) {
					try {
						in.close();
					} catch (IOException ex) {
					}
				}
				if (out != null) {
					try {
						out.close();
					} catch (IOException ex) {
					}
				}
			}
		}

		throw new CantFindFrameworkResException(id);
	}

    public void installFramework(File frameFile, String tag)
            throws AndrolibException {
        InputStream in = null;
        ZipArchiveOutputStream out = null;
        try {
            ZipExtFile zip = new ZipExtFile(frameFile);
            ZipArchiveEntry entry = zip.getEntry("resources.arsc");

            if (entry == null) {
                throw new AndrolibException("Can't find resources.arsc file");
            }

            in = zip.getInputStream(entry);
            byte[] data = IOUtils.toByteArray(in);

            ARSCData arsc = ARSCDecoder.decode(new ByteArrayInputStream(data),
                    true, true);
            publicizeResources(data, arsc.getFlagsOffsets());

            File outFile = new File(getFrameworkDir(), String.valueOf(arsc
                    .getOnePackage().getId())
                    + (tag == null ? "" : '-' + tag)
                    + ".apk");

            out = new ZipArchiveOutputStream(new FileOutputStream(outFile));
            out.setMethod(ZipOutputStream.STORED);
            CRC32 crc = new CRC32();
            crc.update(data);
            entry = new ZipArchiveEntry("resources.arsc");
            entry.setSize(data.length);
            entry.setCrc(crc.getValue());
            out.putArchiveEntry(entry);
            out.write(data);

            out.closeArchiveEntry();
            zip.close();
            LOGGER.info("Framework installed to: " + outFile);
        } catch (ZipException ex) {
            throw new AndrolibException(ex);
        } catch (IOException ex) {
            throw new AndrolibException(ex);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ex) {
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ex) {
                }
            }
        }
    }

	public void publicizeResources(File arscFile) throws AndrolibException {
		byte[] data = new byte[(int) arscFile.length()];

		InputStream in = null;
		OutputStream out = null;
		try {
			in = new FileInputStream(arscFile);
			in.read(data);

			publicizeResources(data);

			out = new FileOutputStream(arscFile);
			out.write(data);
		} catch (IOException ex) {
			throw new AndrolibException(ex);
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException ex) {
				}
			}
			if (out != null) {
				try {
					out.close();
				} catch (IOException ex) {
				}
			}
		}
	}

	public void publicizeResources(byte[] arsc) throws AndrolibException {
		publicizeResources(arsc,
				ARSCDecoder.decode(new ByteArrayInputStream(arsc), true, true)
						.getFlagsOffsets());
	}

	public void publicizeResources(byte[] arsc, FlagsOffset[] flagsOffsets)
			throws AndrolibException {
		for (FlagsOffset flags : flagsOffsets) {
			int offset = flags.offset + 3;
			int end = offset + 4 * flags.count;
			while (offset < end) {
				arsc[offset] |= (byte) 0x40;
				offset += 4;
			}
		}
	}

	private File getFrameworkDir() throws AndrolibException {
		String path;

		// if a framework path was specified on the command line, use it
		if (sFrameworkFolder != null) {
			path = sFrameworkFolder;
		} else if (OSDetection.isMacOSX()) {
			// store in user-home, for Mac OS X
			path = System.getProperty("user.home") + File.separatorChar + "Library/apktool/framework";
		} else {
			path = System.getProperty("user.home") + File.separatorChar + "apktool" + File.separatorChar + "framework";
		}
		
		File dir = new File(path);
		if (!dir.exists()) {
			if (!dir.mkdirs()) {
				if (sFrameworkFolder != null) {
					System.out.println("Can't create Framework directory: "
							+ dir);
				}
				throw new AndrolibException("Can't create directory: " + dir);
			}
		}
		return dir;
	}
	
	/**
	 * Using a prebuilt aapt and forcing its use, allows us to prevent bugs from older aapt's
     * along with having a finer control over the build procedure.
     *
     * Aapt can still be overridden via --aapt/-a on build, but specific features will be disabled
     *
	 * @url https://github.com/iBotPeaches/platform_frameworks_base
	 * @return
	 * @throws AndrolibException
	 */
	public File getAaptBinaryFile() throws AndrolibException {
	  try {
  	  if (OSDetection.isMacOSX()) {
  	    mAaptBinary = Jar
  	        .getResourceAsFile("/prebuilt/aapt/macosx/aapt");
  	  } else if (OSDetection.isUnix()) {
  	    mAaptBinary = Jar
  	        .getResourceAsFile("/prebuilt/aapt/linux/aapt");
  	  } else if (OSDetection.isWindows()) {
  	    mAaptBinary = Jar
  	        .getResourceAsFile("/prebuilt/aapt/windows/aapt.exe");
  	  } else {
  	    return null;
  	  }
	  } catch (BrutException ex) {
	    throw new AndrolibException(ex);
	  }
	  mAaptBinary.setExecutable(true);
	  return mAaptBinary;
	}

	public File getAndroidResourcesFile() throws AndrolibException {
		try {
			return Jar
					.getResourceAsFile("/brut/androlib/android-framework.jar");
		} catch (BrutException ex) {
			throw new AndrolibException(ex);
		}
	}

	public void setFrameworkFolder(String path) {
		sFrameworkFolder = path;
	}

	// TODO: dirty static hack. I have to refactor decoding mechanisms.
	public static boolean sKeepBroken = false;
	public static String sFrameworkFolder = null;

	private final static Logger LOGGER = Logger
			.getLogger(AndrolibResources.class.getName());

	private String mPackageId = null;
	private String mMinSdkVersion = null;
	private String mMaxSdkVersion = null;
	private String mTargetSdkVersion = null;
	private String mVersionCode = null;
	private String mVersionName = null;

	private String mPackageRenamed = null;
	private File mAaptBinary = null;

}
