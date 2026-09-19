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
package brut.yaml;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

public class YamlPullParser implements Closeable {
    private final BufferedReader mReader;
    private Block[] mBlocks;
    private int mDepth;
    private int mPosition;
    private Entry mCurrent;
    private Entry mLookahead;
    private boolean mClosed;

    public YamlPullParser(InputStream in) {
        mReader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        mBlocks = new Block[4];
        mDepth = -1;
    }

    @Override
    public void close() throws IOException {
        mReader.close();
        mClosed = true;
    }

    private boolean nextLine() throws IOException {
        if (mClosed) {
            return false;
        }
        if (mLookahead != null) {
            mCurrent = mLookahead;
            mLookahead = null;
            return true;
        }
        String line;
        while ((line = mReader.readLine()) != null) {
            mPosition++;
            // Skip empty lines and comments.
            int end = line.length();
            if (end == 0 || line.charAt(0) == '#') {
                continue;
            }
            // Strip inline comments.
            char quote = 0;
            for (int i = 0; i < end; i++) {
                char ch = line.charAt(i);
                if (quote != 0) {
                    if (ch == quote) {
                        quote = 0;
                    }
                } else if (ch == '"' || ch == '\'') {
                    quote = ch;
                } else if (i > 0 && ch == '#' && Character.isWhitespace(line.charAt(i - 1))) {
                    end = i;
                    break;
                }
            }
            // Trim whitespace from end.
            while (end > 0 && Character.isWhitespace(line.charAt(end - 1))) {
                end--;
            }
            // Skip the line if nothing is left.
            if (end == 0) {
                continue;
            }
            // Determine the indentation (spaces only) and whether it's a sequence item.
            // A sequence item consists of a hyphen, optionally followed by whitespace and a value.
            // The hyphen and whitespace are included in the indentation.
            int indent = 0, start = 0;
            boolean isItem = false;
            for (int i = 0; i < end; i++) {
                char ch = line.charAt(i);
                if (ch != ' ') {
                    if (Character.isWhitespace(ch)) {
                        throw new YamlSyntaxException(mPosition, "Only spaces can be used for indentation.");
                    }
                    if (ch == '-' && (i == end - 1 || Character.isWhitespace(line.charAt(i + 1)))) {
                        indent = i + 2;
                        start = Math.min(indent, end);
                        isItem = true;
                    } else {
                        start = indent = i;
                    }
                    break;
                }
            }
            // Check whether the indentation deviates from that established for the current block.
            int depth = mDepth;
            int blockIndent, blockType;
            if (depth == -1) {
                blockIndent = blockType = -1;
            } else {
                blockIndent = mBlocks[depth].indent;
                blockType = mBlocks[depth].type;
            }
            if (indent != blockIndent) {
                if (isItem && blockType == Block.SEQ) {
                    throw new YamlSyntaxException(mPosition, "Sequence item indentation is inconsistent.");
                }
                if (indent > blockIndent) {
                    // A new nested block may only follow a mapping entry with no value.
                    if (mCurrent != null && (mCurrent.key == null || mCurrent.value != null)) {
                        throw new YamlSyntaxException(mPosition, "Unexpected nested block.");
                    }
                    if (isItem && indent - 2 < blockIndent) {
                        throw new YamlSyntaxException(mPosition, "Sequence item is shallower than its parent.");
                    }
                    // This line establishes a new nested block.
                    depth++;
                    if (mBlocks.length <= depth) {
                        Block[] newBlocks = new Block[depth + 4];
                        System.arraycopy(mBlocks, 0, newBlocks, 0, mBlocks.length);
                        mBlocks = newBlocks;
                    }
                    blockType = isItem ? Block.SEQ : Block.MAP;
                    mBlocks[depth] = new Block(indent, blockType);
                } else {
                    // The line doesn't belong to the current block.
                    int newDepth = -1;
                    for (int i = depth - 1; i >= 0; i--) {
                        if (indent == mBlocks[i].indent) {
                            newDepth = i;
                            break;
                        }
                    }
                    if (newDepth == -1) {
                        throw new YamlSyntaxException(mPosition, "Indentation does not match any block.");
                    }
                    Arrays.fill(mBlocks, newDepth + 1, depth + 1, null);
                    depth = newDepth;
                    blockType = mBlocks[depth].type;
                }
                mDepth = depth;
            }
            // The line's type must match the block it belongs to.
            if (isItem != (blockType == Block.SEQ)) {
                throw new YamlSyntaxException(mPosition, isItem
                    ? "Sequence item cannot appear in a mapping block."
                    : "Mapping entry cannot appear in a sequence block.");
            }
            // Truncate the line to the new length, excluding indentation.
            line = line.substring(start, end);
            end -= start;
            // Parse key and value.
            String key, value;
            if (isItem) {
                key = null;
                value = line.trim();
            } else {
                // A mapping entry consists of a key and a colon, optionally followed by whitespace and a value.
                quote = 0;
                int keyEnd = -1;
                for (int i = 0; i < end; i++) {
                    char ch = line.charAt(i);
                    if (quote != 0) {
                        if (ch == quote) {
                            quote = 0;
                        }
                    } else if (ch == '"' || ch == '\'') {
                        quote = ch;
                    } else if (ch == ':' && (i == end - 1 || Character.isWhitespace(line.charAt(i + 1)))) {
                        if (keyEnd == -1) {
                            keyEnd = i;
                        } else {
                            throw new YamlSyntaxException(mPosition, "Mapping value looks like another mapping entry.");
                        }
                    }
                }
                if (keyEnd == -1) {
                    throw new YamlSyntaxException(mPosition, "Missing key.");
                }
                key = line.substring(0, keyEnd).trim();
                if (key.isEmpty()) {
                    throw new YamlSyntaxException(mPosition, "Empty key.");
                }
                value = line.substring(keyEnd + 1).trim();
            }
            // An empty value is implicitly set to null.
            if (value.isEmpty()) {
                value = null;
            }
            // Create the new entry.
            mCurrent = new Entry(depth, key, value);
            return true;
        }
        mClosed = true;
        return false;
    }

    public String getKey() {
        String key;
        if (mClosed || mCurrent == null || (key = mCurrent.key) == null) {
            throw new IllegalStateException();
        }
        return YamlUtils.unescapeString(key);
    }

    public String getString() {
        if (mClosed || mCurrent == null) {
            throw new IllegalStateException();
        }
        String value = mCurrent.value;
        if (value == null || value.equals("null")) {
            return null;
        }
        return YamlUtils.unescapeString(value);
    }

    public int getInt() {
        if (mClosed || mCurrent == null) {
            throw new IllegalStateException();
        }
        String value = mCurrent.value;
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException ignored) {
            }
        }
        throw new YamlSyntaxException(mPosition, "Invalid integer value: " + value);
    }

    public boolean getBool() {
        if (mClosed || mCurrent == null) {
            throw new IllegalStateException();
        }
        String value = mCurrent.value;
        if (value != null) {
            if (value.equals("true")) {
                return true;
            }
            if (value.equals("false")) {
                return false;
            }
        }
        throw new YamlSyntaxException(mPosition, "Invalid boolean value: " + value);
    }

    private interface Consumer<T> {
        void accept(T obj, YamlPullParser parser) throws IOException;
    }

    private <T> void readObject(T obj, Consumer<T> consumer) throws IOException {
        if (mClosed || (mCurrent != null && mCurrent.key == null)) {
            throw new IllegalStateException();
        }
        int blockDepth = mDepth + 1;
        while (nextLine()) {
            // If dedented to a parent block, set the lookahead and exit the loop.
            if (mCurrent.depth < blockDepth) {
                mLookahead = mCurrent;
                return;
            }
            // Skip any nested blocks that weren't consumed.
            if (mCurrent.depth > blockDepth) {
                continue;
            }
            if (mCurrent.key == null) {
                throw new YamlSyntaxException(mPosition, "Expected a mapping entry, found a sequence item.");
            }
            consumer.accept(obj, this);
        }
    }

    public <T extends YamlSerializable> void readObject(T obj) throws IOException {
        readObject(obj, YamlSerializable::onEntry);
    }

    private <T> void readMap(Map<String, T> map, Function<YamlPullParser, T> mapper) throws IOException {
        readObject(map, (obj, parser) -> {
            obj.put(parser.mCurrent.key, mapper.apply(parser));
        });
    }

    public void readStringMap(Map<String, String> map) throws IOException {
        readMap(map, YamlPullParser::getString);
    }

    public void readIntMap(Map<String, Integer> map) throws IOException {
        readMap(map, YamlPullParser::getInt);
    }

    public void readBoolMap(Map<String, Boolean> map) throws IOException {
        readMap(map, YamlPullParser::getBool);
    }

    private <T> void readSeq(Collection<T> coll, Function<YamlPullParser, T> mapper) throws IOException {
        if (mClosed || mCurrent == null || mCurrent.key == null) {
            throw new IllegalStateException();
        }
        int blockDepth = mDepth + 1;
        while (nextLine()) {
            // If dedented to a parent block, set the lookahead and exit the loop.
            if (mCurrent.depth < blockDepth) {
                mLookahead = mCurrent;
                return;
            }
            // Skip any nested blocks that weren't consumed.
            if (mCurrent.depth > blockDepth) {
                continue;
            }
            if (mCurrent.key != null) {
                throw new YamlSyntaxException(mPosition, "Expected a sequence item, found a mapping entry.");
            }
            coll.add(mapper.apply(this));
        }
    }

    public void readStringSeq(Collection<String> coll) throws IOException {
        readSeq(coll, YamlPullParser::getString);
    }

    public void readIntSeq(Collection<Integer> coll) throws IOException {
        readSeq(coll, YamlPullParser::getInt);
    }

    private static final class Block {
        public static final int MAP = 0;
        public static final int SEQ = 1;

        public final int indent;
        public final int type;

        public Block(int indent, int type) {
            this.indent = indent;
            this.type = type;
        }
    }

    private static final class Entry {
        public final int depth;
        public final String key;
        public final String value;

        public Entry(int depth, String key, String value) {
            this.depth = depth;
            this.key = key;
            this.value = value;
        }
    }
}
