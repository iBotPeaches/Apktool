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
package brut.xmlpull;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class SimpleXmlPullEventHandler implements XmlPullEventHandler {

    @Override
    public boolean onEvent(XmlPullParser in, XmlSerializer out) throws XmlPullParserException {
        return false;
    }

    @Override
    public void beforeAttributes(XmlPullParser in, XmlSerializer out) throws XmlPullParserException {
    }

    @Override
    public boolean onAttribute(XmlPullParser in, XmlSerializer out, String ns, String name, String value)
            throws XmlPullParserException {
        return false;
    }

    @Override
    public void afterAttributes(XmlPullParser in, XmlSerializer out) throws XmlPullParserException {
    }
}
