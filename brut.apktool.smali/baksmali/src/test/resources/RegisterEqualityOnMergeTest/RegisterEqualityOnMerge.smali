.class public LRegisterEqualityOnMerge;
.super Ljava/lang/Object;


# direct methods
.method public constructor <init>()V
    .registers 4

    #v0=(Uninit);v1=(Uninit);v2=(Uninit);p0=(UninitThis,LRegisterEqualityOnMerge;);
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    #v0=(Uninit);v1=(Uninit);v2=(Uninit);p0=(Reference,LRegisterEqualityOnMerge;);

    #v0=(Uninit);v1=(Uninit);v2=(Uninit);p0=(Reference,LRegisterEqualityOnMerge;);
    invoke-virtual {p0}, Ljava/lang/Object;->toString()Ljava/lang/String;
    #v0=(Uninit);v1=(Uninit);v2=(Uninit);p0=(Reference,LRegisterEqualityOnMerge;);

    #v0=(Uninit);v1=(Uninit);v2=(Uninit);p0=(Reference,LRegisterEqualityOnMerge;);
    move-result-object v0
    #v0=(Reference,Ljava/lang/String;);v1=(Uninit);v2=(Uninit);p0=(Reference,LRegisterEqualityOnMerge;);

    #v0=(Reference,Ljava/lang/String;);v1=(Uninit);v2=(Uninit);p0=(Reference,LRegisterEqualityOnMerge;);
    if-eqz v0, :cond_d
    #v0=(Reference,Ljava/lang/String;);v1=(Uninit);v2=(Uninit);p0=(Reference,LRegisterEqualityOnMerge;);

    #v0=(Reference,Ljava/lang/String;);v1=(Uninit);v2=(Uninit);p0=(Reference,LRegisterEqualityOnMerge;);
    invoke-virtual {p0}, Ljava/lang/Object;->toString()Ljava/lang/String;
    #v0=(Reference,Ljava/lang/String;);v1=(Uninit);v2=(Uninit);p0=(Reference,LRegisterEqualityOnMerge;);

    #v0=(Reference,Ljava/lang/String;);v1=(Uninit);v2=(Uninit);p0=(Reference,LRegisterEqualityOnMerge;);
    move-result-object v0
    #v0=(Reference,Ljava/lang/String;);v1=(Uninit);v2=(Uninit);p0=(Reference,LRegisterEqualityOnMerge;);

    :cond_d
    #v0=(Reference,Ljava/lang/String;);v1=(Uninit);v2=(Uninit);p0=(Reference,LRegisterEqualityOnMerge;);
    return-void
    #v0=(Reference,Ljava/lang/String;);v1=(Uninit);v2=(Uninit);p0=(Reference,LRegisterEqualityOnMerge;);
.end method
