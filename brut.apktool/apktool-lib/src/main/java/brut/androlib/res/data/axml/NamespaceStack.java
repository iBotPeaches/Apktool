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
package brut.androlib.res.data.axml;

/**
 * Namespace stack, holds prefix+uri pairs, as well as depth information.
 * All information is stored in one int[] array. Array consists of depth
 * frames: Data=DepthFrame*; DepthFrame=Count+[Prefix+Uri]*+Count;
 * Count='count of Prefix+Uri pairs'; Yes, count is stored twice, to enable
 * bottom-up traversal. increaseDepth adds depth frame, decreaseDepth
 * removes it. push/pop operations operate only in current depth frame.
 * decreaseDepth removes any remaining (not pop'ed) namespace pairs. findXXX
 * methods search all depth frames starting from the last namespace pair of
 * current depth frame. All functions that operate with int, use -1 as
 * 'invalid value'.
 * <p>
 * !! functions expect 'prefix'+'uri' pairs, not 'uri'+'prefix' !!
 */
public final class NamespaceStack {
    private int[] m_data;
    private int m_dataLength;
    private int m_depth;

    public NamespaceStack() {
        m_data = new int[32];
    }

    public void reset() {
        m_dataLength = 0;
        m_depth = 0;
    }

    public int getCurrentCount() {
        if (m_dataLength == 0) {
            return 0;
        }
        int offset = m_dataLength - 1;
        return m_data[offset];
    }

    public int getAccumulatedCount(int depth) {
        if (m_dataLength == 0 || depth < 0) {
            return 0;
        }
        if (depth > m_depth) {
            depth = m_depth;
        }
        int accumulatedCount = 0;
        int offset = 0;
        for (; depth != 0; --depth) {
            int count = m_data[offset];
            accumulatedCount += count;
            offset += (2 + count * 2);
        }
        return accumulatedCount;
    }

    public void push(int prefix, int uri) {
        if (m_depth == 0) {
            increaseDepth();
        }
        ensureDataCapacity(2);
        int offset = m_dataLength - 1;
        int count = m_data[offset];
        m_data[offset - 1 - count * 2] = count + 1;
        m_data[offset] = prefix;
        m_data[offset + 1] = uri;
        m_data[offset + 2] = count + 1;
        m_dataLength += 2;
    }

    public boolean pop() {
        if (m_dataLength == 0) {
            return false;
        }
        int offset = m_dataLength - 1;
        int count = m_data[offset];
        if (count == 0) {
            return false;
        }
        count -= 1;
        offset -= 2;
        m_data[offset] = count;
        offset -= (1 + count * 2);
        m_data[offset] = count;
        m_dataLength -= 2;
        return true;
    }

    public int getPrefix(int index) {
        return get(index, true);
    }

    public int getUri(int index) {
        return get(index, false);
    }

    public int findPrefix(int uri) {
        return find(uri, false);
    }

    public int getDepth() {
        return m_depth;
    }

    public void increaseDepth() {
        ensureDataCapacity(2);
        int offset = m_dataLength;
        m_data[offset] = 0;
        m_data[offset + 1] = 0;
        m_dataLength += 2;
        m_depth += 1;
    }

    public void decreaseDepth() {
        if (m_dataLength == 0) {
            return;
        }
        int offset = m_dataLength - 1;
        int count = m_data[offset];
        if ((offset - 1 - count * 2) == 0) {
            return;
        }
        m_dataLength -= 2 + count * 2;
        m_depth -= 1;
    }

    private void ensureDataCapacity(int capacity) {
        int available = (m_data.length - m_dataLength);
        if (available > capacity) {
            return;
        }
        int newLength = (m_data.length + available) * 2;
        int[] newData = new int[newLength];
        System.arraycopy(m_data, 0, newData, 0, m_dataLength);
        m_data = newData;
    }

    private int find(int prefixOrUri, boolean prefix) {
        if (m_dataLength == 0) {
            return -1;
        }
        int offset = m_dataLength - 1;
        for (int i = m_depth; i != 0; --i) {
            int count = m_data[offset];
            offset -= 2;
            for (; count != 0; --count) {
                if (prefix) {
                    if (m_data[offset] == prefixOrUri) {
                        return m_data[offset + 1];
                    }
                } else {
                    if (m_data[offset + 1] == prefixOrUri) {
                        return m_data[offset];
                    }
                }
                offset -= 2;
            }
        }
        return -1;
    }

    private int get(int index, boolean prefix) {
        if (m_dataLength == 0 || index < 0) {
            return -1;
        }
        int offset = 0;
        for (int i = m_depth; i != 0; --i) {
            int count = m_data[offset];
            if (index >= count) {
                index -= count;
                offset += (2 + count * 2);
                continue;
            }
            offset += (1 + index * 2);
            if (!prefix) {
                offset += 1;
            }
            return m_data[offset];
        }
        return -1;
    }
}
