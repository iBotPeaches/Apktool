.class public LMultiSwitch;
.super Ljava/lang/Object;
.source "Format31t.smali"


# virtual methods
.method public multi-packed-switch()V
    .registers 1

    const p0, 0xc

    packed-switch p0, :pswitch_data_14

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
    packed-switch p0, :pswitch_data_20

    nop

    :pswitch_f
    return-void

    :pswitch_10
    return-void

    :pswitch_11
    return-void

    :pswitch_12
    return-void

    nop

    :pswitch_data_14
    .packed-switch 0xa
        :pswitch_7
        :pswitch_8
        :pswitch_9
        :pswitch_a
    .end packed-switch

    :pswitch_data_20
    .packed-switch 0xa
        :pswitch_f
        :pswitch_10
        :pswitch_11
        :pswitch_12
    .end packed-switch
.end method

.method public multi-sparse-switch()V
    .registers 1

    const p0, 0xd

    sparse-switch p0, :sswitch_data_14

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
    sparse-switch p0, :sswitch_data_26

    nop

    :sswitch_f
    return-void

    :sswitch_10
    return-void

    :sswitch_11
    return-void

    :sswitch_12
    return-void

    nop

    :sswitch_data_14
    .sparse-switch
        0xa -> :sswitch_7
        0xf -> :sswitch_9
        0x14 -> :sswitch_8
        0x63 -> :sswitch_a
    .end sparse-switch

    :sswitch_data_26
    .sparse-switch
        0xa -> :sswitch_f
        0xf -> :sswitch_11
        0x14 -> :sswitch_10
        0x63 -> :sswitch_12
    .end sparse-switch
.end method
