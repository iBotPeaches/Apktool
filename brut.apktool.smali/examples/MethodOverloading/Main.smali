.class public LMain;
.super Ljava/lang/Object;

#expected output:
#returning a string
#42

.method public constructor <init>()V
    .registers 1
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

.method public static main([Ljava/lang/String;)V
    .registers 4

    sget-object v0, Ljava/lang/System;->out:Ljava/io/PrintStream;

    new-instance v1, LMain;
    invoke-direct {v1}, LMain;-><init>()V
    invoke-virtual {v1}, LMain;->overloadTest()Ljava/lang/String;
    move-result-object v2

    invoke-virtual {v0, v2}, Ljava/io/PrintStream;->println(Ljava/lang/Object;)V

    invoke-virtual {v1}, LMain;->overloadTest()I
    move-result v2

    invoke-static {v2}, Ljava/lang/Integer;->toString(I)Ljava/lang/String;
    move-result-object v2

    invoke-virtual {v0, v2}, Ljava/io/PrintStream;->println(Ljava/lang/Object;)V

    return-void
.end method


.method public overloadTest()Ljava/lang/String;
    .registers 1

    const-string v0, "returning a string"
    return-object v0
.end method

.method public overloadTest()I
    .registers 1

    const v0, 42
    return v0
.end method