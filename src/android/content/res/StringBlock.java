/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.content.res;

import brut.util.Jar;


/**
 * Conveniences for retrieving data out of a compiled string resource.
 *
 * {@hide}
 */
final class StringBlock {
    static {
        Jar.load("/brut/androlib/libAndroid.so");
    }

    private final int mNative;
    private final boolean mOwnsNative;
    private CharSequence[] mStrings;

    public CharSequence get(int idx) {
        synchronized (this) {
            if (mStrings != null) {
                CharSequence res = mStrings[idx];
                if (res != null) {
                    return res;
                }
            } else {
                final int num = nativeGetSize(mNative);
                mStrings = new CharSequence[num];
            }
            String str = nativeGetString(mNative, idx);
            CharSequence res = str;
            mStrings[idx] = res;
            return res;
        }
    }

    protected void finalize() throws Throwable {
        if (mOwnsNative) {
            nativeDestroy(mNative);
        }
    }

    /**
     * Create from an existing string block native object.  This is
     * -extremely- dangerous -- only use it if you absolutely know what you
     *  are doing!  The given native object must exist for the entire lifetime
     *  of this newly creating StringBlock.
     */
    StringBlock(int obj, boolean useSparse) {
        mNative = obj;
        mOwnsNative = false;
    }

    private static final native int nativeGetSize(int obj);
    private static final native String nativeGetString(int obj, int idx);
    private static final native void nativeDestroy(int obj);
}
