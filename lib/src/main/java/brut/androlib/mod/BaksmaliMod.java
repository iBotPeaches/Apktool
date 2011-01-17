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

package brut.androlib.mod;

import brut.androlib.src.TypeName;
import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jf.baksmali.Adaptors.ClassDefinition;
import org.jf.baksmali.baksmali;
import org.jf.baksmali.fileNameHandler;
import org.jf.dexlib.ClassDefItem;
import org.jf.dexlib.Code.Analysis.ClassPath;
import org.jf.dexlib.DexFile;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public class BaksmaliMod {

    public static void disassembleDexFile(boolean debug, String dexFilePath, DexFile dexFile, boolean deodex, String outputDirectory,
                                          String[] classPathDirs, String bootClassPath, String extraBootClassPath,
                                          boolean noParameterRegisters, boolean useLocalsDirective,
                                          boolean useSequentialLabels, boolean outputDebugInfo, boolean addCodeOffsets,
                                          int registerInfo, boolean verify, boolean ignoreErrors)
    {
        baksmali.noParameterRegisters = noParameterRegisters;
        baksmali.useLocalsDirective = useLocalsDirective;
        baksmali.useSequentialLabels = useSequentialLabels;
        baksmali.outputDebugInfo = outputDebugInfo;
        baksmali.addCodeOffsets = addCodeOffsets;
        baksmali.deodex = deodex;
        baksmali.registerInfo = registerInfo;
        baksmali.bootClassPath = bootClassPath;
        baksmali.verify = verify;

        ClassPath.ClassPathErrorHandler classPathErrorHandler = null;
        if (ignoreErrors) {
            classPathErrorHandler = new ClassPath.ClassPathErrorHandler() {
                public void ClassPathError(String className, Exception ex) {
                    System.err.println(String.format("Skipping %s", className));
                    ex.printStackTrace(System.err);
                }
            };
        }

        boolean analyze = ! ClassPath.dontLoadClassPath
            && (registerInfo != 0 || deodex || verify);

        if (analyze) {
            try {
                String[] extraBootClassPathArray = null;
                if (extraBootClassPath != null && extraBootClassPath.length() > 0) {
                    assert extraBootClassPath.charAt(0) == ':';
                    extraBootClassPathArray = extraBootClassPath.substring(1).split(":");
                }

                if (dexFile.isOdex() && bootClassPath == null) {
                    //ext.jar is a special case - it is typically the 2nd jar in the boot class path, but it also
                    //depends on classes in framework.jar (typically the 3rd jar in the BCP). If the user didn't
                    //specify a -c option, we should add framework.jar to the boot class path by default, so that it
                    //"just works"
                    if (extraBootClassPathArray == null && isExtJar(dexFilePath)) {
                        extraBootClassPathArray = new String[] {"framework.jar"};
                    }
                    ClassPath.InitializeClassPathFromOdex(classPathDirs, extraBootClassPathArray, dexFilePath, dexFile,
                            classPathErrorHandler);
                } else {
                    String[] bootClassPathArray = null;
                    if (bootClassPath != null) {
                        bootClassPathArray = bootClassPath.split(":");
                    }
                    ClassPath.InitializeClassPath(classPathDirs, bootClassPathArray, extraBootClassPathArray,
                            dexFilePath, dexFile, classPathErrorHandler);
                }
            } catch (Exception ex) {
                System.err.println("\n\nError occured while loading boot class path files. Aborting.");
                ex.printStackTrace(System.err);
                System.exit(1);
            }
        }

        File outputDirectoryFile = new File(outputDirectory);
        if (!outputDirectoryFile.exists()) {
            if (!outputDirectoryFile.mkdirs()) {
                System.err.println("Can't create the output directory " + outputDirectory);
                System.exit(1);
            }
        }

        //sort the classes, so that if we're on a case-insensitive file system and need to handle classes with file
        //name collisions, then we'll use the same name for each class, if the dex file goes through multiple
        //baksmali/smali cycles for some reason. If a class with a colliding name is added or removed, the filenames
        //may still change of course
        ArrayList<ClassDefItem> classDefItems = new ArrayList<ClassDefItem>(dexFile.ClassDefsSection.getItems());
        Collections.sort(classDefItems, new Comparator<ClassDefItem>() {
            public int compare(ClassDefItem classDefItem1, ClassDefItem classDefItem2) {
                return classDefItem1.getClassType().getTypeDescriptor().compareTo(classDefItem1.getClassType().getTypeDescriptor());
            }
        });

        fileNameHandler fileNameHandler = new fileNameHandler(outputDirectoryFile);

        for (ClassDefItem classDefItem: classDefItems) {
            /**
             * The path for the disassembly file is based on the package name
             * The class descriptor will look something like:
             * Ljava/lang/Object;
             * Where the there is leading 'L' and a trailing ';', and the parts of the
             * package name are separated by '/'
             */

            if (analyze) {
                //If we are analyzing the bytecode, make sure that this class is loaded into the ClassPath. If it isn't
                //then there was some error while loading it, and we should skip it
                ClassPath.ClassDef classDef = ClassPath.getClassDef(classDefItem.getClassType(), false);
                if (classDef == null || classDef instanceof ClassPath.UnresolvedClassDef) {
                    continue;
                }
            }

            String classDescriptor = classDefItem.getClassType().getTypeDescriptor();

            //validate that the descriptor is formatted like we expect
            if (classDescriptor.charAt(0) != 'L' ||
                classDescriptor.charAt(classDescriptor.length()-1) != ';') {
                System.err.println("Unrecognized class descriptor - " + classDescriptor + " - skipping class");
                continue;
            }

            File smaliFile = fileNameHandler.getUniqueFilenameForClass(classDescriptor);

            if (debug) {
                String smaliPath = smaliFile.getPath();
                smaliFile = new File(
                    smaliPath.substring(0, smaliPath.length() - 6) + ".java");
            }

            //create and initialize the top level string template
            ClassDefinition classDefinition = new ClassDefinition(classDefItem);

            //write the disassembly
            Writer writer = null;
            try
            {
                File smaliParent = smaliFile.getParentFile();
                if (!smaliParent.exists()) {
                    if (!smaliParent.mkdirs()) {
                        System.err.println("Unable to create directory " + smaliParent.toString() + " - skipping class");
                        continue;
                    }
                }

                if (!smaliFile.exists()){
                    if (!smaliFile.createNewFile()) {
                        System.err.println("Unable to create file " + smaliFile.toString() + " - skipping class");
                        continue;
                    }
                }

                BufferedWriter bufWriter = new BufferedWriter(new OutputStreamWriter(
                        new FileOutputStream(smaliFile), "UTF8"));

                writer = new IndentingWriter(bufWriter);

                if (debug) {
                    TypeName name = TypeName.fromInternalName(
                        classDefItem.getClassType().getTypeDescriptor());
                    writer.write("package " + name.package_ + "; class "
                        + name.getName(true, true) + " {/*\n\n");
                }

                classDefinition.writeTo((IndentingWriter)writer);

                if (debug) {
                    writer.write("\n*/}\n");
                }
            } catch (Exception ex) {
                System.err.println("\n\nError occured while disassembling class " + classDescriptor.replace('/', '.') + " - skipping class");
                ex.printStackTrace();
            }
            finally
            {
                if (writer != null) {
                    try {
                        writer.close();
                    } catch (Throwable ex) {
                        System.err.println("\n\nError occured while closing file " + smaliFile.toString());
                        ex.printStackTrace();
                    }
                }
            }

            if (!ignoreErrors && classDefinition.hadValidationErrors()) {
                System.exit(1);
            }
        }
    }

    private static final Pattern extJarPattern = Pattern.compile("(?:^|\\\\|/)ext.(?:jar|odex)$");
    private static boolean isExtJar(String dexFilePath) {
        Matcher m = extJarPattern.matcher(dexFilePath);
        return m.find();
    }
}
