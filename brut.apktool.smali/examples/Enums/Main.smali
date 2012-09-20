.class public LMain;
.super Ljava/lang/Object;



.method public static main([Ljava/lang/String;)V
    .registers 2

    sget-object v0, Ljava/lang/System;->out:Ljava/io/PrintStream;
    sget-object v1, LEnum;->VALUE1:LEnum;

    invoke-virtual {v0, v1}, Ljava/io/PrintStream;->println(Ljava/lang/Object;)V

	return-void
.end method