FROM sapmachine:21-jdk-headless-ubuntu-jammy AS builder

COPY . /build
WORKDIR /build
RUN \
    # Apktool
    ./gradlew build shadowJar proguard

FROM sapmachine:21-jre-headless-ubuntu-jammy

# Latest version as of 2023.10.01
# Ref: https://developer.android.com/studio/index.html#command-line-tools-only
ARG COMMAND_LINE_TOOLS_VERSION=10406996

ARG ANDROID_SDK_ROOT=/opt/android-sdk

RUN \
    # Update
    apt-get update &&\
    \
    # Android SDK
    apt-get install -y --no-install-recommends \
        ca-certificates \
        curl \
        zipalign \
        git \
        openssl \
        wget \
        unzip \
        sdkmanager &&\
    curl -o /tmp/tools.zip https://dl.google.com/android/repository/commandlinetools-linux-${COMMAND_LINE_TOOLS_VERSION}_latest.zip &&\
    mkdir -p ${ANDROID_SDK_ROOT}/cmdline-tools &&\
    unzip -q /tmp/tools.zip -d ${ANDROID_SDK_ROOT}/cmdline-tools &&\
    mv ${ANDROID_SDK_ROOT}/cmdline-tools/cmdline-tools ${ANDROID_SDK_ROOT}/cmdline-tools/latest &&\
    rm -v /tmp/tools.zip &&\
    mkdir -p /root/.android/ && touch /root/.android/repositories.cfg &&\
    yes | sdkmanager --licenses &&\
    export BUILD_TOOLS_VERSION=$(sdkmanager --list |grep build-tools |grep -v rc |awk '{print $1}' |sed 's/build-tools;//g' |sort |tail -n1) &&\
    sdkmanager --install "build-tools;${BUILD_TOOLS_VERSION}" &&\
    ln -s ${ANDROID_SDK_ROOT}/build-tools/${BUILD_TOOLS_VERSION} /opt/bin &&\
    \
    # Cleanup
    rm -rf /var/lib/apt/lists/*

COPY --from=builder /build/brut.apktool/apktool-cli/build/libs/apktool-v*.jar /usr/local/bin/apktool.jar
COPY --from=builder /scripts/linux/apktool /usr/local/bin/apktool
RUN \
    # Apktool final setup
    chmod +x /usr/local/bin/apktool /usr/local/bin/apktool.jar

ENV PATH ${PATH}:/opt/bin

CMD ["/usr/local/bin/apktool"]