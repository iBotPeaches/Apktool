FROM openjdk:21-slim

# Latest version as of 2023.10.01
# Ref: https://developer.android.com/studio/index.html#command-line-tools-only
ARG COMMAND_LINE_TOOLS_VERSION=10406996

ARG ANDROID_SDK_ROOT=/opt/android-sdk

RUN \
    # Update
    apt-get update &&\
    \
    # Apktool
    apt-get install --no-install-recommends -y curl zipalign &&\
    curl -o /usr/local/bin/apktool https://raw.githubusercontent.com/iBotPeaches/Apktool/master/scripts/linux/apktool &&\
    curl -L -o /usr/local/bin/apktool.jar $(curl -s https://api.github.com/repos/iBotPeaches/Apktool/releases/latest |grep browser_download_url |awk '{print $2}' |sed 's/"//g') &&\
    chmod +x /usr/local/bin/apktool /usr/local/bin/apktool.jar &&\
    \
    # Android SDK
    apt-get install -y --no-install-recommends \
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

ENV PATH ${PATH}:/opt/bin

CMD ["/usr/local/bin/apktool"]
