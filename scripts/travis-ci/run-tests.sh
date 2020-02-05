#!/usr/bin/env sh

if [ "$TRAVIS_OS_NAME" = "windows" ]; then
    export GRADLE_OPTS=-Dorg.gradle.daemon=false
    export PATH=$PATH:"/c/Program Files/Java/jdk1.8.0_211/bin"
    export JAVA_HOME="/c/Program Files/Java/jdk1.8.0_211"
    ./gradlew.bat build shadowJar proguard
else
    ./gradlew build shadowJar proguard
fi

exit $?
