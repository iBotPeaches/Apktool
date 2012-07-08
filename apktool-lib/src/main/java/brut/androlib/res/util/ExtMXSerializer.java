/**
 *  Copyright 2011 Ryszard Wiśniewski <brut.alll@gmail.com>
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

package brut.androlib.res.util;

import java.io.*;

import org.xmlpull.mxp1_serializer.MXSerializer;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlSerializer;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public class ExtMXSerializer extends MXSerializer implements ExtXmlSerializer {
    @Override
    public void startDocument(String encoding, Boolean standalone) throws
            IOException, IllegalArgumentException, IllegalStateException {
        if (!enableLineOpt || mNewLine >= 1) {
            super.startDocument(encoding != null ? encoding : mDefaultEncoding,
                    standalone);
            this.newLine();
        }
    }

    @Override
    protected void writeAttributeValue(String value, Writer out)
            throws IOException {
        if (mIsDisabledAttrEscape) {
            out.write(value);
            return;
        }
        super.writeAttributeValue(value, out);
    }

    @Override
    public void setOutput(OutputStream os, String encoding) throws IOException {
        super.setOutput(os, encoding != null ? encoding : mDefaultEncoding);
    }

    @Override
    public Object getProperty(String name) throws IllegalArgumentException {
        if (PROPERTY_DEFAULT_ENCODING.equals(name)) {
            return mDefaultEncoding;
        }
        return super.getProperty(name);
    }

    @Override
    public void setProperty(String name, Object value)
            throws IllegalArgumentException, IllegalStateException {
        if (PROPERTY_DEFAULT_ENCODING.equals(name)) {
            mDefaultEncoding = (String) value;
        } else {
            super.setProperty(name, value);
        }
    }

    public ExtXmlSerializer newLine() throws IOException {
        super.out.write(lineSeparator);
        mCurLine ++;
        dbg("Nline: " + mCurLine);
        return this;
    }

    //XmlPullParser.START_TAG("</")
    // if(doIndent) writeIndent();
    @Override
    protected void writeIndent() throws IOException {
        if(!enableLineOpt || mCurLine < mNewLine) {
            super.writeIndent();
            mCurLine ++;
            dbg("Iline: "+mCurLine);
        }
    }

    @Override
    public ExtXmlSerializer setLineNumber(int newLine, int event) throws IOException {
        dbg(/*"curline: " + mCurLine + */", event: " + XmlPullParser.TYPES[event] + 
                ", newline: " + newLine);
        if (newLine == -1) {//Can`t fount line number info
            enableLineOpt = false;
        } else {
            enableLineOpt = true;
            if (event == XmlPullParser.START_DOCUMENT) {
                if (newLine > 1) {
                    mNewLine = 1;
                } else {
                    mNewLine = 0;
                }
            } else {
                mNewLine = newLine;
                if (!startTagIncomplete) {//XmlPullParser.START_TAG("<")
                    dbg(", old event:"+XmlPullParser.TYPES[mLastEvent]);
                    if(mLastEvent != XmlPullParser.END_TAG) {
                        moveToLine (newLine);
                    } else {
                        moveToLine (newLine - 1);
                    }
                }
            }
            mLastEvent = event;
        }
        return this;
    }

    //XmlPullParser.END_TAG(" />") and XmlPullParser.START_TAG("><")
    @Override
    protected void writeNamespaceDeclarations() throws IOException {
        super.writeNamespaceDeclarations();
        if (enableLineOpt) {
            if (mLastEvent == XmlPullParser.END_TAG) {
                moveToLine (mNewLine);
            } else {
                moveToLine (mNewLine - 1);
            }
        }
    }

    private ExtXmlSerializer moveToLine(int newLine) throws IOException {
        int addLines = newLine - mCurLine;
        dbg(", addLines: " + addLines);
        for (; addLines > 0; addLines --) {
            newLine();
        }
        return this;
    }

    @Override
    protected void reset() {
        super.reset();
        mCurLine = 1;
        mLastEvent = XmlPullParser.START_DOCUMENT;
        enableLineOpt = false;
    }

    public void setDisabledAttrEscape(boolean disabled) {
        mIsDisabledAttrEscape = disabled;
    }

    @Override
    public XmlSerializer text(String text) throws IOException {
        if (enableLineOpt) {
            mCurLine += (getTextLineNum(text) - 1);
        }
        return super.text(text);
    }

    private int getTextLineNum(String text) {
        String str = "." + text + ".";
        int linenum = str.split("\\n").length + str.split("\\r").length 
                - str.split("\\n\\r").length;//(Unix(LF)-1) + (Mac(CR)-1) - (Win(CRLF)-1) + 1
        return linenum;
    }

	@Override
    public XmlSerializer text(char[] buf, int start, int len)
        throws IOException {
        if (enableLineOpt) {
            mCurLine += (getTextLineNum(new String(buf, start, len)) - 1);
        }
        return super.text(buf, start, len);
    }

    @Override
    public void ignorableWhitespace(String text) throws IOException {
        if (enableLineOpt) {
            mCurLine += (getTextLineNum(text) - 1);
        }
        super.ignorableWhitespace(text);
    }

    private String mDefaultEncoding;
    private boolean mIsDisabledAttrEscape = false;
    private int mCurLine;
    private int mNewLine;
    private int mLastEvent;
    private boolean enableLineOpt = false;
    private final static boolean DBG = false;

    public ExtXmlSerializer dbg(String str) throws IOException {
        if(DBG)
            super.out.write("<!--"+str+"-->");
        return this;
    }
}
