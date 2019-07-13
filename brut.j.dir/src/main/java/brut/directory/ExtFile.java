/**
 *  Copyright (C) 2019 Ryszard Wiśniewski <brut.alll@gmail.com>
 *  Copyright (C) 2019 Connor Tumbleson <connor.tumbleson@gmail.com>
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

import java.io.File;
import java.io.IOException;
import java.net.URI;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public class ExtFile extends File {
    public ExtFile(File file) {
        super(file.getPath());
    }

    public ExtFile(URI uri) {
        super(uri);
    }

    public ExtFile(File parent, String child) {
        super(parent, child);
    }

    public ExtFile(String parent, String child) {
        super(parent, child);
    }

    public ExtFile(String pathname) {
        super(pathname);
    }

    public Directory getDirectory() throws DirectoryException {
        if (mDirectory == null) {
            if (isDirectory()) {
                mDirectory = new FileDirectory(this);
            } else {
                mDirectory = new ZipRODirectory(this);
            }
        }
        return mDirectory;
    }

    public void close() throws IOException {
        if (mDirectory != null) {
            mDirectory.close();
        }
    }

    private Directory mDirectory;
}
