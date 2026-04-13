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
package brut.apktool.benchmark;

import brut.androlib.ApkBuilder;
import brut.androlib.ApkDecoder;
import brut.androlib.Config;
import brut.directory.ExtFile;
import brut.directory.FileDirectory;
import brut.directory.ZipRODirectory;
import brut.util.OS;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

import java.io.File;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

/**
 * JMH benchmarks for Apktool decode and build operations on the testapp.
 *
 * <p>Run via the benchmark fat JAR:
 * <pre>
 *   java -jar apktool-benchmarks.jar -rf json -rff results.json -prof gc
 * </pre>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 1, time = 10, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 10, timeUnit = TimeUnit.SECONDS)
@Fork(
    value = 1,
    jvmArgs = {
        "-Djdk.util.zip.disableZip64ExtraFieldValidation=true",
        "-Djava.awt.headless=true"
    }
)
public class ApktoolBenchmark {

    private File mTempDir;
    private File mTestApk;
    private File mDecodedDir;
    private Config mConfig;

    @Setup(Level.Trial)
    public void setup() throws Exception {
        mTempDir = OS.createTempDirectory();
        mConfig = new Config("Benchmark");
        mConfig.setFrameworkDirectory(new File(mTempDir, "framework").getAbsolutePath());

        // Copy testapp source resources from classpath to a temp directory.
        File testappSrcDir = new File(mTempDir, "testapp-src");
        OS.mkdir(testappSrcDir);
        copyTestapp(testappSrcDir);

        // Build testapp.apk from the decoded testapp directory.
        mTestApk = new File(mTempDir, "testapp.apk");
        new ApkBuilder(testappSrcDir, mConfig).build(new ExtFile(mTestApk));

        // Pre-decode testapp.apk so the build benchmark has a source directory.
        mDecodedDir = new File(mTempDir, "testapp-decoded");
        new ApkDecoder(new ExtFile(mTestApk), mConfig).decode(mDecodedDir);
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        OS.rmdir(mTempDir);
    }

    /**
     * Benchmarks the time to decode testapp.apk.
     */
    @Benchmark
    public void decodeTestApp() throws Exception {
        File outDir = Files.createTempDirectory(mTempDir.toPath(), "decode").toFile();
        try {
            new ApkDecoder(new ExtFile(mTestApk), mConfig).decode(outDir);
        } finally {
            OS.rmdir(outDir);
        }
    }

    /**
     * Benchmarks the time to build an APK from a decoded testapp directory.
     */
    @Benchmark
    public void buildTestApp() throws Exception {
        File outApk = Files.createTempFile(mTempDir.toPath(), "build", ".apk").toFile();
        try {
            new ApkBuilder(mDecodedDir, mConfig).build(new ExtFile(outApk));
        } finally {
            OS.rmfile(outApk);
        }
    }

    /**
     * Copies the testapp resource directory to the given output directory.
     * Handles both classpath directories (during development) and fat JARs (in CI).
     */
    private void copyTestapp(File outDir) throws Exception {
        URL testappURL = ApktoolBenchmark.class.getClassLoader().getResource("testapp");
        if (testappURL != null && "file".equals(testappURL.getProtocol())) {
            String path = URLDecoder.decode(testappURL.getFile(), "UTF-8");
            new FileDirectory(path).copyToDir(outDir);
        } else {
            // Extract testapp from inside the benchmark fat JAR.
            File benchmarkJar = new File(
                ApktoolBenchmark.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            new ZipRODirectory(benchmarkJar, "testapp/").copyToDir(outDir);
        }
    }
}
