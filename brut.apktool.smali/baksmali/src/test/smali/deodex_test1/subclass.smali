.class public Lsubclass;

.super Lsuperclass;

.method public constructor <init>()V
    .registers 1
    invoke-direct {p0}, Lsuperclass;-><init>()V
    return-void
.end method

.method public somemethod()V
   .registers 2

    sget-object v0, Ljava/lang/System;->out:Ljava/io/PrintStream;

    const-string	v1, "subclass.somemethod"

    invoke-virtual {v0, v1}, Ljava/io/PrintStream;->println(Ljava/lang/String;)V

    return-void
.end method