.class public final enum LEnum;
.super Ljava/lang/Enum;

.field private static final synthetic $VALUES:[LEnum;

.field public static final enum 12:LEnum;

.method static constructor <clinit>()V
    .registers 4

    const/4 v3, 1
    const/4 v2, 0
    new-instance v0, LEnum;
    const-string v1, "12"
    invoke-direct {v0, v1, v2}, LEnum;-><init>(Ljava/lang/String;I)V
    sput-object v0, LEnum;->12:LEnum;

    const/4 v0, 1
    new-array v0, v0, [LEnum;
    sget-object v1, LEnum;->12:LEnum;
    aput-object v1, v0, v2
    
    sput-object v0, LEnum;->$VALUES:[LEnum;
    return-void
.end method

.method private constructor <init>(Ljava/lang/String;I)V
    .registers 3

    invoke-direct {p0, p1, p2}, Ljava/lang/Enum;-><init>(Ljava/lang/String;I)V
    return-void
.end method

.method public static valueOf(Ljava/lang/String;)LEnum;
    .registers 2

    const-class v0, LEnum;
    invoke-static {v0, p0}, Ljava/lang/Enum;->valueOf(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Enum;
    move-result-object v1
    check-cast v1, LEnum;
    return-object v1
.end method

.method public static values()[LEnum;
    .registers 1

    sget-object v0, LEnum;->$VALUES:[LEnum;
    invoke-virtual {v0}, [LEnum;->clone()Ljava/lang/Object;
    move-result-object v0
    check-cast v0, [LEnum;
    return-object v0
.end method