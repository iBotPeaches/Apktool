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
package brut.zip;

import brut.common.Log;
import com.google.common.collect.Maps;

import java.io.Closeable;
import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipArchive implements Closeable {
    private final Path mPath;
    private final ZipFile mZip;
    private Map<String, ZipArchiveEntry> mEntries;
    private ZipArchiveEntry[] mSortedEntries;
    private boolean mClosed;

    public ZipArchive(Path path) throws IOException {
        mPath = path;
        mZip = new ZipFile(path.toFile());
    }

    public Path getPath() {
        return mPath;
    }

    public String getComment() {
        return mZip.getComment();
    }

    InputStream getInputStream(ZipEntry entry) throws IOException {
        return mZip.getInputStream(entry);
    }

    @Override
    public void close() throws IOException {
        if (!mClosed) {
            mClosed = true;
            mEntries = null;
            mZip.close();
        }
    }

    private void ensureLoaded() {
        if (mClosed) {
            throw new IllegalStateException();
        }
        if (mEntries != null) {
            return;
        }
        Map<String, ZipArchiveEntry> entries = Maps.newHashMapWithExpectedSize(mZip.size());
        Enumeration<? extends ZipEntry> zipEntries = mZip.entries();
        while (zipEntries.hasMoreElements()) {
            ZipEntry entry = zipEntries.nextElement();
            if (!entry.isDirectory()) {
                String name = entry.getName();
                if (entries.put(name, new ZipArchiveEntry(this, name, entry)) != null) {
                    Log.w(Log.ROOT, "Duplicate ZIP entry: %s", name);
                }
            }
        }
        mEntries = entries;
    }

    private ZipArchiveEntry[] sortedEntries() {
        if (mSortedEntries == null) {
            ZipArchiveEntry[] sorted = mEntries.values().toArray(new ZipArchiveEntry[0]);
            Arrays.sort(sorted, Comparator.comparing(ZipArchiveEntry::getName));
            mSortedEntries = sorted;
        }
        return mSortedEntries;
    }

    private static int lowerBound(ZipArchiveEntry[] sorted, String key) {
        int low = 0, high = sorted.length;
        while (low < high) {
            int mid = (low + high) >>> 1;
            if (sorted[mid].getName().compareTo(key) < 0) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return low;
    }

    public boolean hasEntries() {
        return hasEntries(null);
    }

    public boolean hasEntries(String prefix) {
        ensureLoaded();
        if (prefix == null || prefix.isEmpty()) {
            return !mEntries.isEmpty();
        }
        ZipArchiveEntry[] sorted = sortedEntries();
        int i = lowerBound(sorted, prefix);
        return i < sorted.length && sorted[i].getName().startsWith(prefix);
    }

    public Stream<ZipArchiveEntry> entries() {
        return entries(null);
    }

    public Stream<ZipArchiveEntry> entries(String prefix) {
        ensureLoaded();
        ZipArchiveEntry[] sorted = sortedEntries();
        if (prefix == null || prefix.isEmpty()) {
            return Arrays.stream(sorted);
        }
        char[] chars = prefix.toCharArray();
        // End bound would wrap if incremented past \uffff.
        if (chars[chars.length - 1] == '\uffff') {
            throw new IllegalArgumentException("Prefix cannot end with \\uffff.");
        }
        chars[chars.length - 1]++;
        int from = lowerBound(sorted, prefix);
        int to = lowerBound(sorted, new String(chars));
        return Arrays.stream(sorted, from, to);
    }

    public boolean hasEntry(String name) {
        ensureLoaded();
        return mEntries.containsKey(name);
    }

    public ZipArchiveEntry getEntry(String name) throws ZipNoSuchEntryException {
        ensureLoaded();
        ZipArchiveEntry entry = mEntries.get(name);
        if (entry == null) {
            throw new ZipNoSuchEntryException(name);
        }
        return entry;
    }

    public void extract(Path dest) throws IOException {
        ensureLoaded();
        for (ZipArchiveEntry entry : mEntries.values()) {
            extractEntry(entry, dest);
        }
    }

    public void extract(Path dest, String name) throws IOException {
        ensureLoaded();
        ZipArchiveEntry entry = mEntries.get(name);
        if (entry == null) {
            throw new ZipNoSuchEntryException(name);
        }
        extractEntry(entry, dest);
    }

    public void extract(Path dest, String... names) throws IOException {
        ensureLoaded();
        ZipArchiveEntry[] entries = new ZipArchiveEntry[names.length];
        for (int i = 0; i < names.length; i++) {
            String name = names[i];
            ZipArchiveEntry entry = mEntries.get(name);
            if (entry == null) {
                throw new ZipNoSuchEntryException(name);
            }
            entries[i] = entry;
        }
        for (ZipArchiveEntry entry : entries) {
            extractEntry(entry, dest);
        }
    }

    private void extractEntry(ZipArchiveEntry entry, Path dest) throws IOException {
        String name;
        try {
            name = ZipUtils.normalizeForFs(entry.getName());
        } catch (IllegalArgumentException ex) {
            Log.w(Log.ROOT, "Could not extract ZIP entry: %s (%s)", entry.getName(), ex.getMessage());
            return;
        }
        entry.extract(dest.resolve(name));
    }

    @Override
    public String toString() {
        return String.format("ZipArchive{path=%s}", mPath);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ZipArchive other = (ZipArchive) obj;
        return mZip.equals(other.mZip);
    }

    @Override
    public int hashCode() {
        return mZip.hashCode();
    }
}
