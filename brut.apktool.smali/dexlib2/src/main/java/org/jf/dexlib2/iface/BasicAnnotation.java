package org.jf.dexlib2.iface;

import javax.annotation.Nonnull;
import java.util.Set;

/**
 * This represents a basic annotation, and serves as a common superclass for Annotation and AnnotationEncodedValue
 */
public interface BasicAnnotation {
    /**
     * Gets the type of this annotation.
     *
     * This will be the type descriptor of the class that defines this annotation.
     *
     * @return The type of this annotation
     */
    @Nonnull String getType();

    /**
     * Gets a set of the name/value elements associated with this annotation.
     *
     * The elements in the returned set will be unique with respect to the element name.
     *
     * @return A set of AnnotationElements
     */
    @Nonnull Set<? extends AnnotationElement> getElements();
}
