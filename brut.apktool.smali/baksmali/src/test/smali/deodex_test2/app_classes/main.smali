.class public Lmain;

.super Ljava/lang/Object;

.method public static main([Ljava/lang/String;)V
    .registers 6

    const v2, 0


    const v3, 1
    const v4, 0
    new-array v1, v3, [Lsubclass1;
    new-instance v0, Lsubclass1;
    invoke-direct {v0}, Lsubclass1;-><init>()V
    aput-object v0, v1, v4

    goto :here2

    :here
    const v2, 1

    :here2

    #this is tricky, because it has to merge two array types, [Lsubclass1; and [Lsubclass2
    #which should result in [Lsuperclass;. However, this dex file won't have a reference
    #to [Lsuperclass;
    aget-object v5, v1, v4

    invoke-virtual {v5}, Lsupersuperclass;->somemethod()V


    new-array v1, v3, [Lsubclass2;
    new-instance v0, Lsubclass2;
    invoke-direct {v0}, Lsubclass2;-><init>()V
    aput-object v0, v1, v4

    if-eqz v2, :here

    return-void
.end method