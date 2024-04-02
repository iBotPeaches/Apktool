-keep class brut.apktool.Main {
    public static void main(java.lang.String[]);
}
-keepclassmembers enum * {
    static **[] values();
    static ** valueOf(java.lang.String);
}
