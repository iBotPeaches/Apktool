.class public LMain;
.super Ljava/lang/Object;

#expected output:
#@AnnotationWithValues(booleanValue=false, byteValue=1, charValue=2, doubleValue=7.0, enumValue=12, floatValue=6.0, intValue=4, longValue=5, methodValue=public static void 10.11(), shortValue=3, stringValue=8, subAnnotationValue=@SubAnnotation(stringValue=9), typeValue=class 10)


.method public static main([Ljava/lang/String;)V
    .registers 3

    sget-object v0, Ljava/lang/System;->out:Ljava/io/PrintStream;

    const-class v1, LMain;
    const-class v2, LAnnotationWithValues;

    invoke-virtual {v1, v2}, Ljava/lang/Class;->getAnnotation(Ljava/lang/Class;)Ljava/lang/annotation/Annotation;
    move-result-object v1

    invoke-virtual {v0, v1}, Ljava/io/PrintStream;->println(Ljava/lang/Object;)V

    return-void
.end method

.annotation runtime LAnnotationWithValues;
.end annotation
