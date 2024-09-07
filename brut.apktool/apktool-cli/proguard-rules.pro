-keep class brut.apktool.Main {
    public static void main(java.lang.String[]);
}
-keepclassmembers enum * {
    static **[] values();
    static ** valueOf(java.lang.String);
}

# https://github.com/iBotPeaches/Apktool/issues/3602#issuecomment-2117317880
-dontwarn org.xmlpull.mxp1**

# https://github.com/iBotPeaches/Apktool/pull/3670#issuecomment-2296326878
-dontwarn com.google.j2objc.annotations.Weak
-dontwarn com.google.j2objc.annotations.RetainedWith
