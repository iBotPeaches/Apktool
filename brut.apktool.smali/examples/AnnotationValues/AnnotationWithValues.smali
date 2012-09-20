.class public abstract interface annotation LAnnotationWithValues;
.super Ljava/lang/Object;
.implements Ljava/lang/annotation/Annotation;

.method public abstract booleanValue()Z
.end method

.method public abstract byteValue()B
.end method

.method public abstract charValue()C
.end method

.method public abstract shortValue()S
.end method

.method public abstract intValue()I
.end method

.method public abstract longValue()J
.end method

.method public abstract floatValue()F
.end method

.method public abstract doubleValue()D
.end method

.method public abstract stringValue()Ljava/lang/String;
.end method

.method public abstract subAnnotationValue()LSubAnnotation;
.end method

.method public abstract typeValue()Ljava/lang/Class;
.end method

.method public abstract methodValue()Ljava/lang/reflect/Method;
.end method

#dalvik doesn't seem to like field values
#.method public abstract fieldValue()Ljava/lang/reflect/Field;
#.end method

.method public abstract enumValue()LEnum;
.end method

.annotation system Ldalvik/annotation/AnnotationDefault;
    value = .subannotation LAnnotationWithValues;
                booleanValue = false
                byteValue = 1t
                charValue = '2'
                shortValue = 3s
                intValue = 4
                longValue = 5l
                floatValue = 6.0f
                doubleValue = 7.0
                stringValue = "8"
                subAnnotationValue = .subannotation LSubAnnotation;
                                            stringValue = "9"
                                     .end subannotation
                typeValue = L10;
                methodValue = L10;->11()V
                enumValue = .enum LEnum;->12:LEnum;                
            .end subannotation
.end annotation

