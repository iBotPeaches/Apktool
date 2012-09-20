.class public LMain;
.super Ljava/lang/Object;

.method public static main([Ljava/lang/String;)V
    .registers 3

    :second_handler
    :first_try_start
        new-instance v0, Ljava/lang/RuntimeException;
        invoke-direct {v0}, Ljava/lang/RuntimeException;-><init>()V
        throw v0
    :first_try_end
    .catch Ljava/lang/Exception; {:first_try_start .. :first_try_end} :first_handler
    :first_handler
    :second_try_start
        new-instance v0, Ljava/lang/RuntimeException;
        invoke-direct {v0}, Ljava/lang/RuntimeException;-><init>()V
        throw v0
    :second_try_end
    .catch Ljava/lang/Exception; {:second_try_start .. :second_try_end} :second_handler
.end method