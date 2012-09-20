.class public LMain;
.super Ljava/lang/Object;
.implements LInterface;

#expected output:
#in interfaceMethod()

.method public constructor <init>()V
    .registers 1
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

.method public static main([Ljava/lang/String;)V
    .registers 3

    sget-object v0, Ljava/lang/System;->out:Ljava/io/PrintStream;

    new-instance v1, LMain;
    invoke-direct {v1}, LMain;-><init>()V
    invoke-interface {v1}, LInterface;->interfaceMethod()Ljava/lang/String;
    move-result-object v1

    invoke-virtual {v0, v1}, Ljava/io/PrintStream;->println(Ljava/lang/Object;)V

    return-void
.end method


.method public interfaceMethod()Ljava/lang/String;
    .registers 1

    const-string v0, "in interfaceMethod()"
    return-object v0
.end method