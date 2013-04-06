/**
 *  Copyright 2010 Ryszard Wiśniewski <brut.alll@gmail.com>
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
package brut.directory;

import org.apache.commons.compress.archivers.zip.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipException;

public class ZipExtFile extends ZipFile {

    public ZipExtFile(File f) throws IOException {
        super(f);
    }

    public ZipExtFile(String name) throws IOException {
        super(name);
    }

    public ZipExtFile(String name, String encoding) throws IOException {
        super(name, encoding);
    }

    public ZipExtFile(File f, String encoding) throws IOException {
        super(f, encoding);
    }

    public ZipExtFile(File f, String encoding, boolean useUnicodeExtraFields) throws IOException {
        super(f, encoding, useUnicodeExtraFields);
    }

    @Override
    /**
     * @author Panxiaobo
     */
    public InputStream getInputStream(ZipArchiveEntry ze)
            throws IOException, ZipException {
        ze.getGeneralPurposeBit().useEncryption(false);
        return super.getInputStream(ze);
    }

    @Override
    public ZipArchiveEntry getEntry(String name) {
        return super.getEntry(name);
    }

}
