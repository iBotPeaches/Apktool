.class public LMultiSwitch;
.super Ljava/lang/Object;
.source "Format31t.smali"

.method public multi-packed-switch()V
    .registers 1
    const p0, 0xc
    packed-switch p0, :pswitch_data_12
    goto :goto_b
    :pswitch_7
    return-void
    :pswitch_8
    return-void
    :pswitch_9
    return-void
    :pswitch_a
    return-void
    :goto_b
    packed-switch p0, :pswitch_data_12
        nop
    return-void
    :pswitch_f
    return-void
    :pswitch_10
    return-void
    :pswitch_11
    return-void
    :pswitch_12
    :pswitch_data_12
    .packed-switch 0xa
    :pswitch_7
    :pswitch_8
    :pswitch_9
    :pswitch_a
    .end packed-switch

.end method

.method public multi-sparse-switch()V
    .registers 1
    const p0, 0xd
    sparse-switch p0, :sswitch_data_12
    goto :goto_b
    :sswitch_7
    return-void
    :sswitch_8
    return-void
    :sswitch_9
    return-void
    :sswitch_a
    return-void
    :goto_b
    sparse-switch p0, :sswitch_data_12
    nop
    return-void
    :sswitch_f
    return-void
    :sswitch_10
    return-void
    :sswitch_11
    return-void

    :sswitch_12

    :sswitch_data_12
    .sparse-switch
        0xa -> :sswitch_7
        0xf -> :sswitch_9
        0x14 -> :sswitch_8
        0x63 -> :sswitch_a
    .end sparse-switch
.end method