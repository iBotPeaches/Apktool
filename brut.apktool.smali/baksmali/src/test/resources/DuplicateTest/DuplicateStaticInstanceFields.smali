.class public LDuplicateStaticInstanceFields;
.super Ljava/lang/Object;


# static fields
.field public static blah:I

# duplicate field ignored
# .field public static blah:I


# instance fields
.field public alah:I

# There is both a static and instance field with this signature.
# You will need to rename one of these fields, including all references.
.field public blah:I

# duplicate field ignored
# .field public blah:I

.field public clah:I
