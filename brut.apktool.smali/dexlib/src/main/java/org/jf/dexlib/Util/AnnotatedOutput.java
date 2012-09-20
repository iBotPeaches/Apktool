/*
 * Copyright (C) 2007 The Android Open Source Project
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

/*
 * As per the Apache license requirements, this file has been modified
 * from its original state.
 *
 * Such modifications are Copyright (C) 2010 Ben Gruver, and are released
 * under the original license
 */

package org.jf.dexlib.Util;

/**
 * Interface for a binary output destination that may be augmented
 * with textual annotations.
 */
public interface AnnotatedOutput
        extends Output {
    /**
     * Get whether this instance will actually keep annotations.
     *
     * @return <code>true</code> iff annotations are being kept
     */
    public boolean annotates();

    /**
     * Get whether this instance is intended to keep verbose annotations.
     * Annotators may use the result of calling this method to inform their
     * annotation activity.
     *
     * @return <code>true</code> iff annotations are to be verbose
     */
    public boolean isVerbose();

    /**
     * Add an annotation for the subsequent output. Any previously
     * open annotation will be closed by this call, and the new
     * annotation marks all subsequent output until another annotation
     * call.
     *
     * @param msg non-null; the annotation message
     */
    public void annotate(String msg);

    /**
     * Add an annotation for a specified amount of subsequent
     * output. Any previously open annotation will be closed by this
     * call. If there is already pending annotation from one or more
     * previous calls to this method, the new call "consumes" output
     * after all the output covered by the previous calls.
     *
     * @param amt &gt;= 0; the amount of output for this annotation to
     * cover
     * @param msg non-null; the annotation message
     */
    public void annotate(int amt, String msg);

    /**
     * End the most recent annotation. Subsequent output will be unannotated,
     * until the next call to {@link #annotate}.
     */
    public void endAnnotation();

    /**
     * Get the maximum width of the annotated output. This is advisory:
     * Implementations of this interface are encouraged to deal with too-wide
     * output, but annotaters are encouraged to attempt to avoid exceeding
     * the indicated width.
     *
     * @return &gt;= 1; the maximum width
     */
    public int getAnnotationWidth();

    public void setIndentAmount(int indentAmount);
    public void indent();
    public void deindent();
}