.class public LUninitRefIdentityTest;
.super Ljava/lang/Object;


# direct methods
.method public constructor <init>()V
    .registers 4

    #v0=(Uninit);v1=(Uninit);v2=(Uninit);p0=(UninitThis,LUninitRefIdentityTest;);
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    #v0=(Uninit);v1=(Uninit);v2=(Uninit);p0=(Reference,LUninitRefIdentityTest;);

    #v0=(Uninit);v1=(Uninit);v2=(Uninit);p0=(Reference,LUninitRefIdentityTest;);
    new-instance v0, Ljava/lang/String;
    #v0=(UninitRef,Ljava/lang/String;);v1=(Uninit);v2=(Uninit);p0=(Reference,LUninitRefIdentityTest;);

    #v0=(UninitRef,Ljava/lang/String;);v1=(Uninit);v2=(Uninit);p0=(Reference,LUninitRefIdentityTest;);
    if-eqz v0, :cond_9
    #v0=(UninitRef,Ljava/lang/String;);v1=(Uninit);v2=(Uninit);p0=(Reference,LUninitRefIdentityTest;);

    #v0=(UninitRef,Ljava/lang/String;);v1=(Uninit);v2=(Uninit);p0=(Reference,LUninitRefIdentityTest;);
    new-instance v0, Ljava/lang/String;
    #v0=(UninitRef,Ljava/lang/String;);v1=(Uninit);v2=(Uninit);p0=(Reference,LUninitRefIdentityTest;);

    :cond_9
    #v0=(Conflicted):merge{0x5:(UninitRef,Ljava/lang/String;),0x7:(UninitRef,Ljava/lang/String;)}
    #v1=(Uninit);v2=(Uninit);p0=(Reference,LUninitRefIdentityTest;);
    invoke-direct {v0}, Ljava/lang/String;-><init>()V
    #v0=(Unknown);v1=(Uninit);v2=(Uninit);p0=(Reference,LUninitRefIdentityTest;);

    #v0=(Unknown);v1=(Uninit);v2=(Uninit);p0=(Reference,LUninitRefIdentityTest;);
    return-void
    #v0=(Unknown);v1=(Uninit);v2=(Uninit);p0=(Reference,LUninitRefIdentityTest;);
.end method

.method public constructor <init>(Ljava/lang/String;)V
    .registers 2

    #p0=(UninitThis,LUninitRefIdentityTest;);p1=(Reference,Ljava/lang/String;);
    move-object p1, p0
    #p0=(UninitThis,LUninitRefIdentityTest;);p1=(UninitThis,LUninitRefIdentityTest;);

    #p0=(UninitThis,LUninitRefIdentityTest;);p1=(UninitThis,LUninitRefIdentityTest;);
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    #p0=(Reference,LUninitRefIdentityTest;);p1=(Reference,LUninitRefIdentityTest;);

    #p0=(Reference,LUninitRefIdentityTest;);p1=(Reference,LUninitRefIdentityTest;);
    return-void
    #p0=(Reference,LUninitRefIdentityTest;);p1=(Reference,LUninitRefIdentityTest;);
.end method
