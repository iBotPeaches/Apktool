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

//package brut.directory;
//
//import java.io.InputStream;
//import java.io.OutputStream;
//import java.util.LinkedHashSet;
//import java.util.Map;
//import java.util.Set;
//
//public class ChangesWrapperDirectory implements Directory {
//    private Directory mOriginal;
//    private Directory mChanges;
//    private Set<String> mRemoved;
//
//    public ChangesWrapperDirectory(Directory original, Directory changes) {
//        this(original, changes, new LinkedHashSet<String>());
//    }
//
//    public ChangesWrapperDirectory(Directory original, Directory changes,
//            Set<String> removed) {
//        super();
//        mOriginal = original;
//        mChanges = changes;
//        mRemoved = removed;
//    }
//
//    public Directory getOriginal() {
//        return mOriginal;
//    }
//
//    public Directory getChanges() {
//        return mChanges;
//    }
//
//    public Set<String> getRemoved() {
//        return mRemoved;
//    }
//
//    @Override
//    public boolean containsDir(String path) {
//        return ! getRemoved().contains(path) && (getOriginal().containsDir(path) || getChanges().containsDir(path));
//    }
//
//    @Override
//    public boolean containsFile(String path) {
//        return ! getRemoved().contains(path) && (getOriginal().containsFile(path) || getChanges().containsFile(path));
//    }
//
//    @Override
//    public Directory createDir(String path) throws PathAlreadyExists,
//            DirectoryException {
//        throw new UnsupportedOperationException();
//    }
//
//    @Override
//    public Directory getDir(String path) throws PathNotExist {
//        throw new UnsupportedOperationException();
//    }
//
//    @Override
//    public Map<String, Directory> getDirs() {
//        return getDirs(false);
//    }
//
//    @Override
//    public Map<String, Directory> getDirs(boolean recursive) {
//        throw new UnsupportedOperationException();
//    }
//
//    @Override
//    public InputStream getFileInput(String path) throws PathNotExist,
//            DirectoryException {
//        if (getRemoved().contains(path)) {
//            throw new PathNotExist(path);
//        }
//        if (getChanges().containsFile(path)) {
//            return getChanges().getFileInput(path);
//        }
//        return getOriginal().getFileInput(path);
//    }
//
//    @Override
//    public OutputStream getFileOutput(String path) throws DirectoryException {
//        getRemoved().remove(path);
//        return getChanges().getFileOutput(path);
//    }
//
//    @Override
//    public Set<String> getFiles() {
//        return getFiles(false);
//    }
//
//    @Override
//    public Set<String> getFiles(boolean recursive) {
//        Set<String> files = new LinkedHashSet<String>(getOriginal().getFiles(recursive));
//        files.addAll(getChanges().getFiles(recursive));
//        files.removeAll(getRemoved());
//        return files;
//    }
//
//    @Override
//    public boolean removeFile(String path) {
//        if(! containsFile(path)) {
//            return false;
//        }
//
//        getChanges().removeFile(path);
//        getRemoved().add(path);
//        return true;
//    }
//
//}
