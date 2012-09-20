.class public LMain;
.super Ljava/lang/Object;

#expected output (using the dalvik's default stack size)
#@RecursiveAnnotation(value=@RecursiveAnnotation(value=@RecursiveAnnotation(value=@RecursiveAnnotation(value=@RecursiveAnnotation(value=@RecursiveAnnotation(value=@RecursiveAnnotation(value=@RecursiveAnnotation(value=@RecursiveAnnotation(value=@RecursiveAnnotation(value=@RecursiveAnnotation(value=@RecursiveAnnotation(value=@RecursiveAnnotation(value=@RecursiveAnnotation(value=@RecursiveAnnotation(value=@RecursiveAnnotation(value=@RecursiveAnnotation(value=@RecursiveAnnotation(value=@RecursiveAnnotation(value=@RecursiveAnnotation(value=@RecursiveAnnotation(value=@RecursiveAnnotation(value=@RecursiveAnnotation(value=@RecursiveAnnotation(value=@RecursiveAnnotation(value=@RecursiveAnnotation(value=@RecursiveAnnotation(value=@RecursiveAnnotation(value=@RecursiveAnnotation(value=@RecursiveAnnotation(value=java.lang.StackOverflowError))))))))))))))))))))))))))))))

.method public static main([Ljava/lang/String;)V
    .registers 3

    sget-object v0, Ljava/lang/System;->out:Ljava/io/PrintStream;

    const-class v1, LMain;
    const-class v2, LRecursiveAnnotation;

    invoke-virtual {v1, v2}, Ljava/lang/Class;->getAnnotation(Ljava/lang/Class;)Ljava/lang/annotation/Annotation;
    move-result-object v1

    invoke-virtual {v0, v1}, Ljava/io/PrintStream;->println(Ljava/lang/Object;)V

    return-void
.end method

.annotation runtime LRecursiveAnnotation;
.end annotation
