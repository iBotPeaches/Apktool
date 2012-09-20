.class public Lrandomclass;

.super Ljava/lang/Object;

.method public constructor <init>()V
    .registers 1
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

.method public getSuperclass()Lsuperclass;
   .registers 2

    new-instance v0, Lsuperclass;
    invoke-direct {v0}, Lsuperclass;-><init>()V

    return-object v0
.end method