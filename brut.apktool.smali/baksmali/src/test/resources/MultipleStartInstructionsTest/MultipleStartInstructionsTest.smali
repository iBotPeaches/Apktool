.class public LMultipleStartInstructionsTest;
.super Ljava/lang/Object;


# direct methods
.method public constructor <init>(Ljava/lang/String;)V
    .registers 4

    :try_start_0
    #v0=(Uninit);v1=(Uninit);p0=(UninitThis,LMultipleStartInstructionsTest;);p1=(Reference,Ljava/lang/String;);
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    #v0=(Uninit);v1=(Uninit);p0=(Reference,LMultipleStartInstructionsTest;);p1=(Reference,Ljava/lang/String;);

    #v0=(Uninit);v1=(Uninit);p0=(Reference,LMultipleStartInstructionsTest;);p1=(Reference,Ljava/lang/String;);
    const-string v0, "blah"
    #v0=(Reference,Ljava/lang/String;);v1=(Uninit);p0=(Reference,LMultipleStartInstructionsTest;);p1=(Reference,Ljava/lang/String;);

    #v0=(Reference,Ljava/lang/String;);v1=(Uninit);p0=(Reference,LMultipleStartInstructionsTest;);p1=(Reference,Ljava/lang/String;);
    return-void
    #v0=(Reference,Ljava/lang/String;);v1=(Uninit);p0=(Reference,LMultipleStartInstructionsTest;);p1=(Reference,Ljava/lang/String;);
    :try_end_6
    .catchall {:try_start_0 .. :try_end_6} :catchall_6

    :catchall_6
    :try_start_6
    #v0=(Uninit);v1=(Uninit);
    #p0=(Conflicted):merge{Start:(UninitThis,LMultipleStartInstructionsTest;),0x0:(Reference,LMultipleStartInstructionsTest;)}
    #p1=(Reference,Ljava/lang/String;);
    invoke-static {}, LMultipleStartInstructionsTest;->blah()V
    #v0=(Uninit);v1=(Uninit);p0=(Conflicted);p1=(Reference,Ljava/lang/String;);
    :try_end_9
    .catchall {:try_start_6 .. :try_end_9} :catchall_9

    :catchall_9
    #v0=(Uninit);v1=(Uninit);
    #p0=(Conflicted):merge{Start:(UninitThis,LMultipleStartInstructionsTest;),0x0:(Reference,LMultipleStartInstructionsTest;),0x6:(Conflicted)}
    #p1=(Reference,Ljava/lang/String;);
    return-void
    #v0=(Uninit);v1=(Uninit);p0=(Conflicted);p1=(Reference,Ljava/lang/String;);
.end method

.method public static blah()V
    .registers 0

    return-void
.end method
