#!/usr/bin/env sh

if [ "$TRAVIS_OS_NAME" = "windows" ]; then
    export GRADLE_OPTS=-Dorg.gradle.daemon=false
    choco install jdk8
    export PATH=$PATH:"/c/Program Files/Java/jdk1.8.0_211/bin"
    export JAVA_HOME="/c/Program Files/Java/jdk1.8.0_211"
    ./gradlew.bat clean
else
    ./gradlew clean
fi

exit $?
