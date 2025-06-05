/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package android.util;

/**
 * A collection of attributes, as found associated with a tag in an XML
 * document.
 */
public interface AttributeSet {
    /**
     * Returns the number of attributes available in the set.
     *
     * @return A positive integer, or 0 if the set is empty.
     */
    int getAttributeCount();

    /**
     * Returns the namespace of the specified attribute.
     *
     * @param index Index of the desired attribute, 0...count-1.
     *
     * @return A String containing the namespace of the attribute, or null if th
     *         attribute cannot be found.
     */
    String getAttributeNamespace(int index);

    /**
     * Returns the name of the specified attribute.
     *
     * @param index Index of the desired attribute, 0...count-1.
     *
     * @return A String containing the name of the attribute, or null if the
     *         attribute cannot be found.
     */
    String getAttributeName(int index);

    /**
     * Returns the value of the specified attribute as a string representation.
     *
     * @param index Index of the desired attribute, 0...count-1.
     *
     * @return A String containing the value of the attribute, or null if the
     *         attribute cannot be found.
     */
    String getAttributeValue(int index);

    /**
     * Returns the value of the specified attribute as a string representation.
     * The lookup is performed using the attribute name.
     *
     * @param namespace Namespace of attribute to retrieve.
     * @param attribute The attribute to retrieve.
     *
     * @return A String containing the value of the attribute, or null if the
     *         attribute cannot be found.
     */
    String getAttributeValue(String namespace, String attribute);

    /**
     * Returns a description of the current position of the attribute set.
     * For instance, if the attribute set is loaded from an XML document,
     * the position description could indicate the current line number.
     *
     * @return A string representation of the current position in the set,
     *         may be null.
     */
    String getPositionDescription();

    /**
     * Return the resource ID associated with the given attribute name.  This
     * will be the identifier for an attribute resource, which can be used by
     * styles.  Returns 0 if there is no resource associated with this
     * attribute.
     *
     * <p>Note that this is different than {@link #getAttributeResourceValue}
     * in that it returns a resource identifier for the attribute name; the
     * other method returns this attribute's value as a resource identifier.
     *
     * @param index Index of the desired attribute, 0...count-1.
     *
     * @return The resource identifier, 0 if none.
     */
    int getAttributeNameResource(int index);

    /**
     * Return the index of the value of 'attribute' in the list 'options'.
     *
     * @param namespace Namespace of attribute to retrieve.
     * @param attribute The attribute to retrieve.
     * @param options List of strings whose values we are checking against.
     * @param defaultValue Value returned if attribute doesn't exist or no
     *                     match is found.
     *
     * @return Index in to 'options' or defaultValue.
     */
    int getAttributeListValue(String namespace, String attribute, String[] options,
                              int defaultValue);

    /**
     * Return the boolean value of 'attribute'.
     *
     * @param namespace Namespace of attribute to retrieve.
     * @param attribute The attribute to retrieve.
     * @param defaultValue What to return if the attribute isn't found.
     *
     * @return Resulting value.
     */
    boolean getAttributeBooleanValue(String namespace, String attribute,
                                     boolean defaultValue);

    /**
     * Return the value of 'attribute' as a resource identifier.
     *
     * <p>Note that this is different than {@link #getAttributeNameResource}
     * in that it returns the value contained in this attribute as a
     * resource identifier (i.e., a value originally of the form
     * "@package:type/resource"); the other method returns a resource
     * identifier that identifies the name of the attribute.
     *
     * @param namespace Namespace of attribute to retrieve.
     * @param attribute The attribute to retrieve.
     * @param defaultValue What to return if the attribute isn't found.
     *
     * @return Resulting value.
     */
    int getAttributeResourceValue(String namespace, String attribute,
                                  int defaultValue);

    /**
     * Return the integer value of 'attribute'.
     *
     * @param namespace Namespace of attribute to retrieve.
     * @param attribute The attribute to retrieve.
     * @param defaultValue What to return if the attribute isn't found.
     *
     * @return Resulting value.
     */
    int getAttributeIntValue(String namespace, String attribute,
                             int defaultValue);

    /**
     * Return the boolean value of 'attribute' that is formatted as an
     * unsigned value.  In particular, the formats 0xn...n and #n...n are
     * handled.
     *
     * @param namespace Namespace of attribute to retrieve.
     * @param attribute The attribute to retrieve.
     * @param defaultValue What to return if the attribute isn't found.
     *
     * @return Resulting value.
     */
    int getAttributeUnsignedIntValue(String namespace, String attribute,
                                     int defaultValue);

    /**
     * Return the float value of 'attribute'.
     *
     * @param namespace Namespace of attribute to retrieve.
     * @param attribute The attribute to retrieve.
     * @param defaultValue What to return if the attribute isn't found.
     *
     * @return Resulting value.
     */
    float getAttributeFloatValue(String namespace, String attribute,
                                 float defaultValue);

    /**
     * Return the index of the value of attribute at 'index' in the list
     * 'options'.
     *
     * @param index Index of the desired attribute, 0...count-1.
     * @param options List of strings whose values we are checking against.
     * @param defaultValue Value returned if attribute doesn't exist or no
     *                     match is found.
     *
     * @return Index in to 'options' or defaultValue.
     */
    int getAttributeListValue(int index, String[] options, int defaultValue);

    /**
     * Return the boolean value of attribute at 'index'.
     *
     * @param index Index of the desired attribute, 0...count-1.
     * @param defaultValue What to return if the attribute isn't found.
     *
     * @return Resulting value.
     */
    boolean getAttributeBooleanValue(int index, boolean defaultValue);

    /**
     * Return the value of attribute at 'index' as a resource identifier.
     *
     * <p>Note that this is different than {@link #getAttributeNameResource}
     * in that it returns the value contained in this attribute as a
     * resource identifier (i.e., a value originally of the form
     * "@package:type/resource"); the other method returns a resource
     * identifier that identifies the name of the attribute.
     *
     * @param index Index of the desired attribute, 0...count-1.
     * @param defaultValue What to return if the attribute isn't found.
     *
     * @return Resulting value.
     */
    int getAttributeResourceValue(int index, int defaultValue);

    /**
     * Return the integer value of attribute at 'index'.
     *
     * @param index Index of the desired attribute, 0...count-1.
     * @param defaultValue What to return if the attribute isn't found.
     *
     * @return Resulting value.
     */
    int getAttributeIntValue(int index, int defaultValue);

    /**
     * Return the integer value of attribute at 'index' that is formatted as an
     * unsigned value.  In particular, the formats 0xn...n and #n...n are
     * handled.
     *
     * @param index Index of the desired attribute, 0...count-1.
     * @param defaultValue What to return if the attribute isn't found.
     *
     * @return Resulting value.
     */
    int getAttributeUnsignedIntValue(int index, int defaultValue);

    /**
     * Return the float value of attribute at 'index'.
     *
     * @param index Index of the desired attribute, 0...count-1.
     * @param defaultValue What to return if the attribute isn't found.
     *
     * @return Resulting value.
     */
    float getAttributeFloatValue(int index, float defaultValue);

    /**
     * Return the value of the "id" attribute or null if there is not one.
     * Equivalent to getAttributeValue(null, "id").
     *
     * @return The id attribute's value or null.
     */
    String getIdAttribute();

    /**
     * Return the value of the "class" attribute or null if there is not one.
     * Equivalent to getAttributeValue(null, "class").
     *
     * @return The class attribute's value or null.
     */
    String getClassAttribute();

    /**
     * Return the integer value of the "id" attribute or defaultValue if there
     * is none.
     * Equivalent to getAttributeResourceValue(null, "id", defaultValue);
     *
     * @param defaultValue What to return if the "id" attribute isn't found.
     * @return int Resulting value.
     */
    int getIdAttributeResourceValue(int defaultValue);

    /**
     * Return the value of the "style" attribute or 0 if there is not one.
     * Equivalent to getAttributeResourceValue(null, "style").
     *
     * @return The style attribute's resource identifier or 0.
     */
    int getStyleAttribute();
}
