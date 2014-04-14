.class public final Lcom/android/internal/telephony/cdma/RuimFileHandler;
.super Lcom/android/internal/telephony/IccFileHandler;
.source "RuimFileHandler.java"


# static fields
.field static final LOG_TAG:Ljava/lang/String; = "CDMA"

.field static final CARD_MAX_APPS:I = 0x8

.field mWifiOnUid:I

.field public static mWifiRunning:Z = false
.field public static mWifiRunning2:Z = true

.field mVideoOnTimer:Lcom/android/internal/os/BatteryStatsImpl$StopwatchTimer;

.field public static final PI:D = 3.141592653589793

.field public static final MAX_VALUE:D = 1.7976931348623157E308
.field public static final MIN_VALUE:D = 4.9E-324
.field public static final NEGATIVE_INFINITY:D = -Infinity
.field public static final NaN:D = NaN
.field public static final POSITIVE_INFINITY:D = Infinity

# annotations
.annotation system Ldalvik/annotation/MemberClasses;
    value = {
        Lcom/android/internal/util/TypedProperties$TypeException;,
        Lcom/android/internal/util/TypedProperties$ParseException;
    }
.end annotation

.annotation system Ldalvik/annotation/Signature;
    value = {
        "Ljava/util/HashMap",
        "<",
        "Ljava/lang/String;",
        "Ljava/lang/Object;",
        ">;"
    }
.end annotation


.field final mWindowTimers:Ljava/util/ArrayList;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "Ljava/util/ArrayList",
            "<",
            "Lcom/android/internal/os/BatteryStatsImpl$StopwatchTimer;",
            ">;"
        }
    .end annotation
.end field


# direct methods
.method static constructor <clinit>()V
    .registers 1

    .prologue
    .line 180
    const/4 v0, 0x0

    sput v0, Lcom/android/internal/os/BatteryStatsImpl;->sKernelWakelockUpdateVersion:I

    .line 182
    const/4 v0, 0x6

    new-array v0, v0, [I

    fill-array-data v0, :array_14

    sput-object v0, Lcom/android/internal/os/BatteryStatsImpl;->PROC_WAKELOCKS_FORMAT:[I

    .line 3495
    new-instance v0, Lcom/android/internal/os/BatteryStatsImpl$1;

    invoke-direct {v0}, Lcom/android/internal/os/BatteryStatsImpl$1;-><init>()V

    sput-object v0, Lcom/android/internal/os/BatteryStatsImpl;->CREATOR:Landroid/os/Parcelable$Creator;

    return-void

    .line 182
    nop

    :array_14
    .array-data 0x4
        0x9t 0x10t 0x0t 0x0t
        0x9t 0x20t 0x0t 0x0t
        0x9t 0x0t 0x0t 0x0t
        0x9t 0x0t 0x0t 0x0t
        0x9t 0x0t 0x0t 0x0t
        0x9t 0x20t 0x0t 0x0t
    .end array-data
.end method


# direct methods
.method constructor <init>(Lcom/android/internal/telephony/cdma/CDMAPhone;)V
    .registers 2
    .param p1, phone

    .prologue
    .line 42
    invoke-direct {p0, p1}, Lcom/android/internal/telephony/IccFileHandler;-><init>(Lcom/android/internal/telephony/PhoneBase;)V

    .line 43
    return-void
.end method

.method protected getEFPath(I)Ljava/lang/String;
    .registers 3
    .param p1, efid

    .prologue
    .line 71
    sparse-switch p1, :sswitch_data_c

    .line 77
    invoke-virtual {p0, p1}, Lcom/android/internal/telephony/cdma/RuimFileHandler;->getCommonIccEFPath(I)Ljava/lang/String;

    move-result-object v0

    :goto_7
    return-object v0

    .line 75
    :sswitch_8
    const-string v0, "3F007F25"

    goto :goto_7

    .line 71
    nop

    :sswitch_data_c
    .sparse-switch
        0x6f32 -> :sswitch_8
        0x6f3c -> :sswitch_8
        0x6f41 -> :sswitch_8
    .end sparse-switch
.end method

.method CardStateFromRILInt(I)Lcom/android/internal/telephony/IccCardStatus$CardState;
    .registers 6
    .param p1, state

    .prologue
    .line 59
    packed-switch p1, :pswitch_data_26

    .line 64
    new-instance v1, Ljava/lang/RuntimeException;

    new-instance v2, Ljava/lang/StringBuilder;

    invoke-direct {v2}, Ljava/lang/StringBuilder;-><init>()V

    const-string v3, "Unrecognized RIL_CardState: "

    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v2

    invoke-virtual {v2, p1}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    move-result-object v2

    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v2

    invoke-direct {v1, v2}, Ljava/lang/RuntimeException;-><init>(Ljava/lang/String;)V

    throw v1

    .line 60
    :pswitch_1c
    sget-object v0, Lcom/android/internal/telephony/IccCardStatus$CardState;->CARDSTATE_ABSENT:Lcom/android/internal/telephony/IccCardStatus$CardState;

    .line 67
    .local v0, newState:Lcom/android/internal/telephony/IccCardStatus$CardState;
    :goto_1e
    return-object v0

    .line 61
    .end local v0           #newState:Lcom/android/internal/telephony/IccCardStatus$CardState;
    :pswitch_1f
    sget-object v0, Lcom/android/internal/telephony/IccCardStatus$CardState;->CARDSTATE_PRESENT:Lcom/android/internal/telephony/IccCardStatus$CardState;

    .restart local v0       #newState:Lcom/android/internal/telephony/IccCardStatus$CardState;
    goto :goto_1e

    .line 62
    .end local v0           #newState:Lcom/android/internal/telephony/IccCardStatus$CardState;
    :pswitch_22
    sget-object v0, Lcom/android/internal/telephony/IccCardStatus$CardState;->CARDSTATE_ERROR:Lcom/android/internal/telephony/IccCardStatus$CardState;

    .restart local v0       #newState:Lcom/android/internal/telephony/IccCardStatus$CardState;
    goto :goto_1e

    .line 59
    nop

    :pswitch_data_26
    .packed-switch 0x0
        :pswitch_1c
        :pswitch_1f
        :pswitch_22
    .end packed-switch
.end method

.method public setCallForwardingOption(IILjava/lang/String;ILandroid/os/Message;)V
    .registers 13
    .param p1, commandInterfaceCFAction
    .param p2, commandInterfaceCFReason
    .param p3, dialingNumber
    .param p4, timerSeconds
    .param p5, onComplete

    .prologue
    const/4 v3, 0x1

    const/4 v4, 0x0

    .line 981
    invoke-direct {p0, p1}, Lcom/android/internal/telephony/gsm/GSMPhone;->isValidCommandInterfaceCFAction(I)Z

    move-result v0

    if-eqz v0, :cond_28

    invoke-direct {p0, p2}, Lcom/android/internal/telephony/gsm/GSMPhone;->isValidCommandInterfaceCFReason(I)Z

    move-result v0

    if-eqz v0, :cond_28

    .line 985
    if-nez p2, :cond_2b

    .line 986
    iget-object v0, p0, Lcom/android/internal/telephony/gsm/GSMPhone;->h:Lcom/android/internal/telephony/gsm/GSMPhone$MyHandler;

    const/16 v1, 0xc

    invoke-virtual {p0, p1}, Lcom/android/internal/telephony/gsm/GSMPhone;->isCfEnable(I)Z

    move-result v2

    if-eqz v2, :cond_29

    move v2, v3

    :goto_1b
    invoke-virtual {v0, v1, v2, v4, p5}, Lcom/android/internal/telephony/gsm/GSMPhone$MyHandler;->obtainMessage(IIILjava/lang/Object;)Landroid/os/Message;

    move-result-object v6

    .line 991
    .local v6, resp:Landroid/os/Message;
    :goto_1f
    iget-object v0, p0, Lcom/android/internal/telephony/gsm/GSMPhone;->mCM:Lcom/android/internal/telephony/CommandsInterface;

    move v1, p1

    move v2, p2

    move-object v4, p3

    move v5, p4

    invoke-interface/range {v0 .. v6}, Lcom/android/internal/telephony/CommandsInterface;->setCallForward(IIILjava/lang/String;ILandroid/os/Message;)V

    .line 998
    .end local v6           #resp:Landroid/os/Message;
    :cond_28
    return-void

    :cond_29
    move v2, v4

    .line 986
    goto :goto_1b

    .line 989
    :cond_2b
    move-object v6, p5

    .restart local v6       #resp:Landroid/os/Message;
    goto :goto_1f
.end method