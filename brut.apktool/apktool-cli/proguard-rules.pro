-keep class brut.apktool.Main {
    public static void main(java.lang.String[]);
}
-keepclassmembers enum * {
    static **[] values();
    static ** valueOf(java.lang.String);
}

# https://github.com/iBotPeaches/Apktool/pull/3670#issuecomment-2296326878
-dontwarn com.google.j2objc.annotations.Weak
-dontwarn com.google.j2objc.annotations.RetainedWith
