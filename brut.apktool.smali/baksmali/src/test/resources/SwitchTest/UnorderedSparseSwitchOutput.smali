.class public LUnorderedSparseSwitch;
.super Ljava/lang/Object;
.method public static test_sparse-switch()V
.registers 1
const v0, 0xd
sparse-switch v0, :sswitch_data_c
:sswitch_6
return-void
:sswitch_7
return-void
:sswitch_8
return-void
:sswitch_9
return-void
:sswitch_a
return-void
nop

# Note: ordered keys
:sswitch_data_c
.sparse-switch
0xa -> :sswitch_6
0xd -> :sswitch_9
0xf -> :sswitch_8
0x14 -> :sswitch_7
0x63 -> :sswitch_a
.end sparse-switch
.end method