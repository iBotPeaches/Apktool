/*
 * [The "BSD licence"]
 * Copyright (c) 2010 Ben Gruver (JesusFreke)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.jf.baksmali;

import org.jf.baksmali.Adaptors.ClassDefinition;
import org.jf.dexlib.ClassDefItem;
import org.jf.dexlib.Code.Analysis.*;
import org.jf.dexlib.DexFile;
import org.jf.util.ClassFileNameHandler;
import org.jf.util.IndentingWriter;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class baksmali {
    public static boolean noParameterRegisters = false;
    public static boolean useLocalsDirective = false;
    public static boolean useSequentialLabels = false;
    public static boolean outputDebugInfo = true;
    public static boolean addCodeOffsets = false;
    public static boolean noAccessorComments = false;
    public static boolean deodex = false;
    public static boolean verify = false;
    public static InlineMethodResolver inlineResolver = null;
    public static int registerInfo = 0;
    public static String bootClassPath;

    public static SyntheticAccessorResolver syntheticAccessorResolver = null;

    public static void disassembleDexFile(String dexFilePath, DexFile dexFile, boolean deodex, String outputDirectory,
                                          String[] classPathDirs, String bootClassPath, String extraBootClassPath,
                                          boolean noParameterRegisters, boolean useLocalsDirective,
                                          boolean useSequentialLabels, boolean outputDebugInfo, boolean addCodeOffsets,
                                          boolean noAccessorComments, int registerInfo, boolean verify,
                                          boolean ignoreErrors, String inlineTable, boolean checkPackagePrivateAccess)
    {
        baksmali.noParameterRegisters = noParameterRegisters;
        baksmali.useLocalsDirective = useLocalsDirective;
        baksmali.useSequentialLabels = useSequentialLabels;
        baksmali.outputDebugInfo = outputDebugInfo;
        baksmali.addCodeOffsets = addCodeOffsets;
        baksmali.noAccessorComments = noAccessorComments;
        baksmali.deodex = deodex;
        baksmali.registerInfo = registerInfo;
        baksmali.bootClassPath = bootClassPath;
        baksmali.verify = verify;

        if (registerInfo != 0 || deodex || verify) {
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
                            checkPackagePrivateAccess);
                } else {
                    String[] bootClassPathArray = null;
                    if (bootClassPath != null) {
                        bootClassPathArray = bootClassPath.split(":");
                    }
                    ClassPath.InitializeClassPath(classPathDirs, bootClassPathArray, extraBootClassPathArray,
                            dexFilePath, dexFile, checkPackagePrivateAccess);
                }

                if (inlineTable != null) {
                    inlineResolver = new CustomInlineMethodResolver(inlineTable);
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

        if (!noAccessorComments) {
            syntheticAccessorResolver = new SyntheticAccessorResolver(dexFile);
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

        ClassFileNameHandler fileNameHandler = new ClassFileNameHandler(outputDirectoryFile, ".smali");

        for (ClassDefItem classDefItem: classDefItems) {
            /**
             * The path for the disassembly file is based on the package name
             * The class descriptor will look something like:
             * Ljava/lang/Object;
             * Where the there is leading 'L' and a trailing ';', and the parts of the
             * package name are separated by '/'
             */

            String classDescriptor = classDefItem.getClassType().getTypeDescriptor();

            //validate that the descriptor is formatted like we expect
            if (classDescriptor.charAt(0) != 'L' ||
                classDescriptor.charAt(classDescriptor.length()-1) != ';') {
                System.err.println("Unrecognized class descriptor - " + classDescriptor + " - skipping class");
                continue;
            }

            File smaliFile = fileNameHandler.getUniqueFilenameForClass(classDescriptor);

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
                classDefinition.writeTo((IndentingWriter)writer);
            } catch (Exception ex) {
                System.err.println("\n\nError occured while disassembling class " + classDescriptor.replace('/', '.') + " - skipping class");
                ex.printStackTrace();
                smaliFile.delete();
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
