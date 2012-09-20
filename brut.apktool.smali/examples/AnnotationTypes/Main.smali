.class public LMain;
.super Ljava/lang/Object;


#expected output:
#@ClassAnnotation()
#@MethodAnnotation()
#@FieldAnnotation()
#@ParameterAnnotation()


.method public static main([Ljava/lang/String;)V
    .registers 1

    invoke-static {}, LMain;->testClassAnnotation()V

    invoke-static {}, LMain;->testMethodAnnotation()V

    invoke-static {}, LMain;->testFieldAnnotation()V

    const-string v0, ""

    invoke-static {v0}, LMain;->testParameterAnnotation(Ljava/lang/String;)V 

	return-void
.end method

.annotation runtime LClassAnnotation;
.end annotation

.method public static testClassAnnotation()V
    .registers 3

    sget-object v0, Ljava/lang/System;->out:Ljava/io/PrintStream;

    const-class v1, LMain;
    const-class v2, LClassAnnotation;

    invoke-virtual {v1, v2}, Ljava/lang/Class;->getAnnotation(Ljava/lang/Class;)Ljava/lang/annotation/Annotation;
    move-result-object v1

    invoke-virtual {v0, v1}, Ljava/io/PrintStream;->println(Ljava/lang/Object;)V

    return-void
.end method



.method public static testMethodAnnotation()V
    .registers 4

    .annotation runtime LMethodAnnotation;
    .end annotation

    sget-object v0, Ljava/lang/System;->out:Ljava/io/PrintStream;

    const-class v1, LMain;
    const-string v2, "testMethodAnnotation"

    const/4 v3, 0
    new-array v3, v3, [Ljava/lang/Class;

    invoke-virtual {v1, v2, v3}, Ljava/lang/Class;->getMethod(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;
    move-result-object v1

    const-class v2, LMethodAnnotation;

    invoke-virtual {v1, v2}, Ljava/lang/reflect/Method;->getAnnotation(Ljava/lang/Class;)Ljava/lang/annotation/Annotation;
    move-result-object v1

    invoke-virtual {v0, v1}, Ljava/io/PrintStream;->println(Ljava/lang/Object;)V

    return-void
.end method


.field public static fieldAnnotationTest:Ljava/lang/Object;
    .annotation runtime LFieldAnnotation;
    .end annotation
.end field

.method public static testFieldAnnotation()V
    .registers 3

    sget-object v0, Ljava/lang/System;->out:Ljava/io/PrintStream;

    const-class v1, LMain;
    const-string v2, "fieldAnnotationTest"

    invoke-virtual {v1, v2}, Ljava/lang/Class;->getField(Ljava/lang/String;)Ljava/lang/reflect/Field;
    move-result-object v1

    const-class v2, LFieldAnnotation;

    invoke-virtual {v1, v2}, Ljava/lang/reflect/Field;->getAnnotation(Ljava/lang/Class;)Ljava/lang/annotation/Annotation;
    move-result-object v1

    invoke-virtual {v0, v1}, Ljava/io/PrintStream;->println(Ljava/lang/Object;)V

    return-void
.end method


.method public static testParameterAnnotation(Ljava/lang/String;)V
    .registers 6

    .parameter
        .annotation runtime LParameterAnnotation;
        .end annotation
    .end parameter


    sget-object v0, Ljava/lang/System;->out:Ljava/io/PrintStream;

    const-class v1, LMain;
    const-string v2, "testParameterAnnotation"

    const/4 v3, 1
    new-array v3, v3, [Ljava/lang/Class;

    const-class v4, Ljava/lang/String;
    const/4 v5, 0
    aput-object v4, v3, v5

    invoke-virtual {v1, v2, v3}, Ljava/lang/Class;->getMethod(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;
    move-result-object v1


    invoke-virtual {v1}, Ljava/lang/reflect/Method;->getParameterAnnotations()[[Ljava/lang/annotation/Annotation;
    move-result-object v1

    aget-object v1, v1, v5
    aget-object v1, v1, v5

    invoke-virtual {v0, v1}, Ljava/io/PrintStream;->println(Ljava/lang/Object;)V

    return-void
.end method