#!/usr/bin/env sh

if [[ "$TRAVIS_OS_NAME" == "windows" ]]; then
    choco install jdk8
    export PATH=$PATH:"/c/Program Files/Java/jdk1.8.0_191/bin"
    ./gradlew.bat clean
else
    ./gradlew clean
fi

exit $?