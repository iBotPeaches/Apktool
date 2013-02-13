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

import org.xmlpull.v1.XmlPullParser;

import android.util.AttributeSet;

/**
 * The XML parsing interface returned for an XML resource. This is a standard
 * XmlPullParser interface, as well as an extended AttributeSet interface and an
 * additional close() method on this interface for the client to indicate when
 * it is done reading the resource.
 */
public interface XmlResourceParser extends XmlPullParser, AttributeSet {
	/**
	 * Close this interface to the resource. Calls on the interface are no
	 * longer value after this call.
	 */
	public void close();
}
