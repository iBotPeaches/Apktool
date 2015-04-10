.class public LDuplicateDirectVirtualMethods;
.super Ljava/lang/Object;


# direct methods
.method private blah()V
    .registers 1

    return-void
.end method

# duplicate method ignored
# .method private blah()V
#     .registers 1

#     return-void
# .end method


# virtual methods
.method public alah()V
    .registers 1

    return-void
.end method

# There is both a direct and virtual method with this signature.
# You will need to rename one of these methods, including all references.
.method public blah()V
    .registers 1

    return-void
.end method

# duplicate method ignored
# .method public blah()V
#     .registers 1

#     return-void
# .end method

.method public clah()V
    .registers 1

    return-void
.end method
