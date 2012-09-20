.class public abstract interface annotation LRecursiveAnnotation;
.super Ljava/lang/Object;
.implements Ljava/lang/annotation/Annotation;

#this is a recursive annotation that has a default value of itself.
#Trying to print .toString() on an instance of this annotation
#will cause a stack overflow

.method public abstract value()LRecursiveAnnotation;
.end method

.annotation system Ldalvik/annotation/AnnotationDefault;
    value = .subannotation LRecursiveAnnotation;
                value = .subannotation LRecursiveAnnotation;
                        .end subannotation
            .end subannotation
.end annotation

