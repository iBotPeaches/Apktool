.class public LInterfaceOrder;
.super Ljava/lang/Object;

# Note how these two interfaces are not in alphabetical order
.implements Ljava/io/Serializable;
.implements Ljava/util/EventListener;
.implements Ljava/lang/Runnable;
.implements Ljava/io/Flushable;
.implements Ljava/lang/Clonable;
.implements Ljava/util/Observer;
.implements Ljava/io/Closeable;

# direct methods
.method public constructor <init>()V
    .registers 1
    return-void
.end method

.method public close()V
    .registers 1
    return-void
.end method

.method public flush()V
    .registers 1
    return-void
.end method

.method public run()V
    .registers 1
    return-void
.end method

.method public update(Ljava/util/Observable;Ljava/lang/Object;)V
    .registers 3
    return-void
.end method
