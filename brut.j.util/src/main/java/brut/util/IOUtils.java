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
package brut.util;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.FileVisitResult;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.util.Iterator;
import java.util.stream.Stream;

public final class IOUtils {

    private IOUtils() {}

    public static Path toCanonicalPath(Path path) throws IOException {
        // Absolute but not normalized since the OS must resolve ".." after symlinks.
        Path absolute = path.toAbsolutePath();
        // Find the longest portion of the path that actually exists.
        Path existing = absolute;
        Path tail = null;
        while (existing != null && !Files.exists(existing, LinkOption.NOFOLLOW_LINKS)) {
            Path name = existing.getFileName();
            if (name == null) {
                // Reached a non-existent root (e.g. a missing drive on Windows).
                existing = null;
                break;
            }
            tail = tail != null ? name.resolve(tail) : name;
            existing = existing.getParent();
        }
        // If nothing in the path exists, just lexically normalize.
        if (existing == null) {
            return absolute.normalize();
        }
        // Otherwise, get the real path of the existing portion and append the tail.
        // No symlinks can remain in the tail, so lexical normalization is safe here.
        Path real = existing.toRealPath();
        return (tail != null ? real.resolve(tail) : real).normalize();
    }

    public static String getFileExtension(String path) {
        return getFileExtension(Paths.get(path));
    }

    public static String getFileExtension(Path path) {
        Path name = path.getFileName();
        if (name == null) {
            return "";
        }
        String fileName = name.toString();
        int lastDot = fileName.lastIndexOf('.');
        return lastDot != -1 ? fileName.substring(lastDot + 1) : "";
    }

    public static String getNameWithoutExtension(String path) {
        return getNameWithoutExtension(Paths.get(path));
    }

    public static String getNameWithoutExtension(Path path) {
        Path name = path.getFileName();
        if (name == null) {
            return "";
        }
        String fileName = name.toString();
        int lastDot = fileName.lastIndexOf('.');
        return lastDot != -1 ? fileName.substring(0, lastDot) : fileName;
    }

    public static Path createParentDirectories(Path path, FileAttribute<?>... attrs) throws IOException {
        Path parent = toCanonicalPath(path).getParent();
        if (parent != null) {
            Files.createDirectories(parent, attrs);
        }
        return path;
    }

    public static void deleteDirectory(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        if (!Files.isDirectory(path)) {
            throw new NotDirectoryException(path.toString());
        }
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.deleteIfExists(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc != null) {
                    throw exc;
                }
                Files.deleteIfExists(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public static void moveDirectory(Path src, Path dest) throws IOException {
        Path realSrc = src.toRealPath();
        if (!Files.isDirectory(realSrc)) {
            throw new NotDirectoryException(src.toString());
        }
        Path canonDest = toCanonicalPath(dest);
        if (canonDest.startsWith(realSrc)) {
            throw new IOException("Destination is the source directory or inside it: " + dest);
        }
        Files.walkFileTree(realSrc, new SimpleFileVisitor<Path>() {
            private Path targetOf(Path entry) throws IOException {
                Path target = resolveAcross(canonDest, realSrc.relativize(entry));
                // Only possible when dest is an ancestor of src and src contains an entry that repeats the
                // path from dest to src (e.g. src=/a/b, dest=/a, entry=/a/b/b, target=/a/b).
                if (target.equals(realSrc)) {
                    throw new IOException("Move target is the source directory: " + target);
                }
                return target;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Files.createDirectories(targetOf(dir));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.move(file, targetOf(file), StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc != null) {
                    throw exc;
                }
                Files.deleteIfExists(dir);
                return FileVisitResult.CONTINUE;
            }
        });
        if (Files.isSymbolicLink(src)) {
            Files.deleteIfExists(src);
        }
    }

    public static void copyDirectory(Path src, Path dest) throws IOException {
        Path realSrc = src.toRealPath();
        if (!Files.isDirectory(realSrc)) {
            throw new NotDirectoryException(src.toString());
        }
        Path canonDest = toCanonicalPath(dest);
        if (canonDest.startsWith(realSrc)) {
            throw new IOException("Destination is the source directory or inside it: " + dest);
        }
        Files.walkFileTree(realSrc, new SimpleFileVisitor<Path>() {
            private Path targetOf(Path entry) throws IOException {
                Path target = resolveAcross(canonDest, realSrc.relativize(entry));
                // Only possible when dest is an ancestor of src and src contains an entry that repeats the
                // path from dest to src (e.g. src=/a/b, dest=/a, entry=/a/b/b, target=/a/b).
                if (target.equals(realSrc)) {
                    throw new IOException("Copy target is the source directory: " + target);
                }
                return target;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Files.createDirectories(targetOf(dir));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.copy(file, targetOf(file), StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static Path resolveAcross(Path base, Path relative) {
        Path target = base;
        for (Path name : relative) {
            target = target.resolve(name.toString());
        }
        return target;
    }

    public static Path resolveDirectoryName(Path path) throws IOException {
        if (!Files.exists(path)) {
            throw new NoSuchFileException(path.toString());
        }
        if (!Files.isDirectory(path)) {
            throw new NotDirectoryException(path.toString());
        }
        path = path.toAbsolutePath().normalize();
        Path dirName = path.getFileName();
        return dirName != null ? dirName : path.getRoot();
    }

    public static boolean isNonEmptyDirectory(Path path) {
        if (!Files.isDirectory(path)) {
            return false;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
            return stream.iterator().hasNext();
        } catch (IOException ignored) {
            return true;
        }
    }

    public static long recursiveModifiedTime(Path path) throws IOException {
        long result = 0;
        try (Stream<Path> stream = Files.walk(path)) {
            Iterator<Path> it = stream.iterator();
            while (it.hasNext()) {
                long time = Files.getLastModifiedTime(it.next(), LinkOption.NOFOLLOW_LINKS).toMillis();
                if (time > result) {
                    result = time;
                }
            }
        }
        return result;
    }

    public static String sanitizePath(Path base, String path) {
        if (path.isEmpty()) {
            throw new IllegalArgumentException("Path is empty.");
        }
        Path original = Paths.get(path);
        if (original.isAbsolute()) {
            throw new IllegalArgumentException("Absolute paths are not allowed: " + path);
        }
        base = base.toAbsolutePath().normalize();
        Path resolved = base.resolve(original).normalize();
        if (!resolved.startsWith(base)) {
            throw new IllegalArgumentException("Path traverses outside the base directory: " + path);
        }
        return base.relativize(resolved).toString();
    }
}
