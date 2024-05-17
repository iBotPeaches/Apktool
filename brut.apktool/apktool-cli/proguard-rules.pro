-keep class brut.apktool.Main {
    public static void main(java.lang.String[]);
}
-keepclassmembers enum * {
    static **[] values();
    static ** valueOf(java.lang.String);
}

# https://github.com/iBotPeaches/Apktool/issues/3602#issuecomment-2117317880
-dontwarn org.xmlpull.mxp1**
