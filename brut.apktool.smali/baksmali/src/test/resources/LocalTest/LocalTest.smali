.class public LLocalTest;
.super Ljava/lang/Object;


# direct methods
.method public static method1()V
    .registers 10

    .local v0, "blah! This local name has some spaces, a colon, even a \nnewline!":I, "some sig info:\nblah."
    .local v1, "blah! This local name has some spaces, a colon, even a \nnewline!":V, "some sig info:\nblah."
    .local v2, "blah! This local name has some spaces, a colon, even a \nnewline!":I
    .local v3, "blah! This local name has some spaces, a colon, even a \nnewline!":V
    .local v4, null:I, "some sig info:\nblah."
    .local v5, null:V, "some sig info:\nblah."
    .local v6, null:I
    .local v7
    .local v8
    .local v9
    return-void
.end method

.method public static method2(IJLjava/lang/String;)V
    .registers 10
    .param p0, "blah! This local name has some spaces, a colon, even a \nnewline!"    # I
    .param p1    # J
        .annotation runtime LAnnotationWithValues;
        .end annotation
    .end param

    return-void
.end method
