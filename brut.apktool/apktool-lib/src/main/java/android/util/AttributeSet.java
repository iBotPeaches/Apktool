/*
 * Copyright 2008 Android4ME
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package android.util;

/**
 * @author Dmitry Skiba
 * 
 */
public interface AttributeSet {
	int getAttributeCount();

	String getAttributeName(int index);

	String getAttributeValue(int index);

	String getPositionDescription();

	int getAttributeNameResource(int index);

	int getAttributeListValue(int index, String options[], int defaultValue);

	boolean getAttributeBooleanValue(int index, boolean defaultValue);

	int getAttributeResourceValue(int index, int defaultValue);

	int getAttributeIntValue(int index, int defaultValue);

	int getAttributeUnsignedIntValue(int index, int defaultValue);

	float getAttributeFloatValue(int index, float defaultValue);

	String getIdAttribute();

	String getClassAttribute();

	int getIdAttributeResourceValue(int index);

	int getStyleAttribute();

	String getAttributeValue(String namespace, String attribute);

	int getAttributeListValue(String namespace, String attribute,
			String options[], int defaultValue);

	boolean getAttributeBooleanValue(String namespace, String attribute,
			boolean defaultValue);

	int getAttributeResourceValue(String namespace, String attribute,
			int defaultValue);

	int getAttributeIntValue(String namespace, String attribute,
			int defaultValue);

	int getAttributeUnsignedIntValue(String namespace, String attribute,
			int defaultValue);

	float getAttributeFloatValue(String namespace, String attribute,
			float defaultValue);

	// TODO: remove
	int getAttributeValueType(int index);

	int getAttributeValueData(int index);
}