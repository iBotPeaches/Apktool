# Apktool #
http://code.google.com/p/android-apktool/

Links
- Wiki http://code.google.com/p/android-apktool/w/list
- Bug Reports http://code.google.com/p/android-apktool/issues/list
- Changelog/Information http://code.google.com/p/android-apktool/wiki/Changelog
- XDA Post http://forum.xda-developers.com/showthread.php?p=28366939
- Source (Github) https://github.com/iBotPeaches/Apktool
- Source (GoogleCode) http://code.google.com/p/android-apktool/source/list

add build explanation

build environment: Java (1.7)

Build Steps

git clone git://github.com/iBotPeaches/Apktool.git

cd Apktool

# for Wondows user
set JAVA_TOOL_OPTIONS=-Dfile.encoding=utf-8 -Duser.language=en -Duser.country=US

# for Linux user
export JAVA_TOOL_OPTIONS=-Dfile.encoding=utf-8 -Duser.language=en -Duser.country=US


[./gradlew][gradlew.bat] fatJar

or [./gradlew][gradlew.bat] fatJar proguard

./brut.apktool/apktool-cli/build/libs/apktool-xxxxx.jar
