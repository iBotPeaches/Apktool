FROM openjdk:22-slim

RUN \
    # Update
    apt-get update &&\
    \
    # APKTool
    apt-get install --no-install-recommends -y curl zipalign &&\
    curl -o /usr/local/bin/apktool https://raw.githubusercontent.com/iBotPeaches/Apktool/master/scripts/linux/apktool &&\
    curl -L -o /usr/local/bin/apktool.jar $(curl -s https://api.github.com/repos/iBotPeaches/Apktool/releases/latest |grep browser_download_url |awk '{print $2}' |sed 's/"//g') &&\
    chmod +x /usr/local/bin/apktool /usr/local/bin/apktool.jar &&\
    \
    # Android SDK
    apt-get install -y --no-install-recommends \
        git \
        git-lfs \
        openssl \
        wget \
        unzip \
        sdkmanager &&\
    curl -o /tmp/tools.zip https://dl.google.com/android/repository/commandlinetools-linux-8512546_latest.zip && \
    mkdir -p ${ANDROID_SDK_ROOT}/cmdline-tools && \
    unzip -q /tmp/tools.zip -d ${ANDROID_SDK_ROOT}/cmdline-tools && \
    mv ${ANDROID_SDK_ROOT}/cmdline-tools/cmdline-tools ${ANDROID_SDK_ROOT}/cmdline-tools/latest && \
    rm -v /tmp/tools.zip && \
    mkdir -p /root/.android/ && touch /root/.android/repositories.cfg &&\
    yes | sdkmanager --licenses &&\
    sdkmanager --install "build-tools;30.0.3" &&\
    \
    # Cleanup
    rm -rf /var/lib/apt/lists/*

ENV PATH ${PATH}:/opt/android-sdk-linux/build-tools/30.0.3

CMD ["/usr/local/bin/apktool"]