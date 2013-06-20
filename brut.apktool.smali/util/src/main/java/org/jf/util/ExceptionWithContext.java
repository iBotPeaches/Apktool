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

package org.jf.util;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * Exception which carries around structured context.
 */
public class ExceptionWithContext
        extends RuntimeException {
    /** non-null; human-oriented context of the exception */
    private StringBuffer context;

    /**
     * Augments the given exception with the given context, and return the
     * result. The result is either the given exception if it was an
     * {@link ExceptionWithContext}, or a newly-constructed exception if it
     * was not.
     *
     * @param ex non-null; the exception to augment
     * @param str non-null; context to add
     * @return non-null; an appropriate instance
     */
    public static ExceptionWithContext withContext(Throwable ex, String str, Object... formatArgs) {
        ExceptionWithContext ewc;

        if (ex instanceof ExceptionWithContext) {
            ewc = (ExceptionWithContext) ex;
        } else {
            ewc = new ExceptionWithContext(ex);
        }

        ewc.addContext(String.format(str, formatArgs));
        return ewc;
    }

    /**
     * Constructs an instance.
     *
     * @param message human-oriented message
     */
    public ExceptionWithContext(String message, Object... formatArgs) {
        this(null, message, formatArgs);
    }

    /**
     * Constructs an instance.
     *
     * @param cause null-ok; exception that caused this one
     */
    public ExceptionWithContext(Throwable cause) {
        this(cause, null);
    }

    /**
     * Constructs an instance.
     *
     * @param message human-oriented message
     * @param cause null-ok; exception that caused this one
     */
    public ExceptionWithContext(Throwable cause, String message, Object... formatArgs) {
        super((message != null) ? formatMessage(message, formatArgs) :
              (cause != null) ? cause.getMessage() : null,
              cause);

        if (cause instanceof ExceptionWithContext) {
            String ctx = ((ExceptionWithContext) cause).context.toString();
            context = new StringBuffer(ctx.length() + 200);
            context.append(ctx);
        } else {
            context = new StringBuffer(200);
        }
    }

    private static String formatMessage(String message, Object... formatArgs) {
        if (message == null) {
            return null;
        }
        return String.format(message, formatArgs);
    }

    /** {@inheritDoc} */
    @Override
    public void printStackTrace(PrintStream out) {
        super.printStackTrace(out);
        out.println(context);
    }

    /** {@inheritDoc} */
    @Override
    public void printStackTrace(PrintWriter out) {
        super.printStackTrace(out);
        out.println(context);
    }

    /**
     * Adds a line of context to this instance.
     *
     * @param str non-null; new context
     */
    public void addContext(String str) {
        if (str == null) {
            throw new NullPointerException("str == null");
        }

        context.append(str);
        if (!str.endsWith("\n")) {
            context.append('\n');
        }
    }

    /**
     * Gets the context.
     *
     * @return non-null; the context
     */
    public String getContext() {
        return context.toString();
    }

    /**
     * Prints the message and context.
     *
     * @param out non-null; where to print to
     */
    public void printContext(PrintStream out) {
        out.println(getMessage());
        out.print(context);
    }

    /**
     * Prints the message and context.
     *
     * @param out non-null; where to print to
     */
    public void printContext(PrintWriter out) {
        out.println(getMessage());
        out.print(context);
    }
}