.class public LUnorderedSparseSwitch;
.super Ljava/lang/Object;

.method public static test_sparse-switch()V
    .registers 1

    const v0, 13

    sparse-switch v0, :SparseSwitch

:Label10
    return-void

:Label20
    return-void

:Label15
    return-void

:Label13
    return-void

:Label99
    return-void

# Note: unordered keys
:SparseSwitch
    .sparse-switch
        10 -> :Label10
        20 -> :Label20
        15 -> :Label15
        99 -> :Label99
        13 -> :Label13
    .end sparse-switch
.end method