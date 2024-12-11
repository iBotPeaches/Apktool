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
    private int[] mData;
    private int mDataLength;
    private int mDepth;

    public NamespaceStack() {
        mData = new int[32];
    }

    public void reset() {
        mDataLength = 0;
        mDepth = 0;
    }

    public int getCurrentCount() {
        if (mDataLength == 0) {
            return 0;
        }
        int offset = mDataLength - 1;
        return mData[offset];
    }

    public int getAccumulatedCount(int depth) {
        if (mDataLength == 0 || depth < 0) {
            return 0;
        }
        if (depth > mDepth) {
            depth = mDepth;
        }
        int accumulatedCount = 0;
        int offset = 0;
        for (; depth != 0; --depth) {
            int count = mData[offset];
            accumulatedCount += count;
            offset += (2 + count * 2);
        }
        return accumulatedCount;
    }

    public void push(int prefix, int uri) {
        if (mDepth == 0) {
            increaseDepth();
        }
        ensureDataCapacity(2);
        int offset = mDataLength - 1;
        int count = mData[offset];
        mData[offset - 1 - count * 2] = count + 1;
        mData[offset] = prefix;
        mData[offset + 1] = uri;
        mData[offset + 2] = count + 1;
        mDataLength += 2;
    }

    public boolean pop() {
        if (mDataLength == 0) {
            return false;
        }
        int offset = mDataLength - 1;
        int count = mData[offset];
        if (count == 0) {
            return false;
        }
        count -= 1;
        offset -= 2;
        mData[offset] = count;
        offset -= (1 + count * 2);
        mData[offset] = count;
        mDataLength -= 2;
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
        return mDepth;
    }

    public void increaseDepth() {
        ensureDataCapacity(2);
        int offset = mDataLength;
        mData[offset] = 0;
        mData[offset + 1] = 0;
        mDataLength += 2;
        mDepth += 1;
    }

    public void decreaseDepth() {
        if (mDataLength == 0) {
            return;
        }
        int offset = mDataLength - 1;
        int count = mData[offset];
        if ((offset - 1 - count * 2) == 0) {
            return;
        }
        mDataLength -= 2 + count * 2;
        mDepth -= 1;
    }

    private void ensureDataCapacity(int capacity) {
        int available = (mData.length - mDataLength);
        if (available > capacity) {
            return;
        }
        int newLength = (mData.length + available) * 2;
        int[] newData = new int[newLength];
        System.arraycopy(mData, 0, newData, 0, mDataLength);
        mData = newData;
    }

    private int find(int prefixOrUri, boolean prefix) {
        if (mDataLength == 0) {
            return -1;
        }
        int offset = mDataLength - 1;
        for (int i = mDepth; i != 0; --i) {
            int count = mData[offset];
            offset -= 2;
            for (; count != 0; --count) {
                if (prefix) {
                    if (mData[offset] == prefixOrUri) {
                        return mData[offset + 1];
                    }
                } else {
                    if (mData[offset + 1] == prefixOrUri) {
                        return mData[offset];
                    }
                }
                offset -= 2;
            }
        }
        return -1;
    }

    private int get(int index, boolean prefix) {
        if (mDataLength == 0 || index < 0) {
            return -1;
        }
        int offset = 0;
        for (int i = mDepth; i != 0; --i) {
            int count = mData[offset];
            if (index >= count) {
                index -= count;
                offset += (2 + count * 2);
                continue;
            }
            offset += (1 + index * 2);
            if (!prefix) {
                offset += 1;
            }
            return mData[offset];
        }
        return -1;
    }
}
