# Building aapt binaries.

The steps taken for building our modified aapt binaries for apktool.

### Getting the modified `frameworks/base` repo.
First step is using the [platform_frameworks_base](https://github.com/iBotPeaches/platform_frameworks_base) repo.

While previously unorganized, the repo now follows the branch naming convention depending on the current Android version.
So `apktool_7.1` corresponds to the 7.1 Android release. This branch should work for all `android-7.1.x` tags for AOSP.

We didn't follow this naming convention until Android 7.1. So don't go looking for older versions.

This repo has a variety of changes applied. These changes range from disabling optimizations to lessening the rules
that aapt regularly has. We do this because apktool's job is to not fix apks, but rather keep them as close to the
original as they were.

### First we need the AOSP source

As cheesy as it is, just follow this [downloading](https://source.android.com/source/downloading.html) link in order
to get the source downloaded. This is no small download, expect to use 40-60GB.

After that, you need to build AOSP via this [documentation](https://source.android.com/source/building.html) guide. Now
we aren't building the entire AOSP package, the initial build is to just see if you are capable of building it.

We check out a certain tag. Currently we use `android-7.1.1_r4`.

### Including our modified `frameworks/base` package.

There is probably a more automated way to do this, but for now just remove all the files in `frameworks/base`. Now
you can clone the modified repo from first step into this directory.

### Building the aapt binary.

The steps below are different per flavor and operating system. For cross compiling the Windows binary on Unix,
we lose the ability to quickly build just the aapt binary. So the Windows procedure builds the entire Sdk.

#### Unix 32
1. `source build/envsetup.sh`
2. `lunch sdk-eng`
3. `make OUT_DIR=out-x32 LOCAL_MULTILIB=32 USE_NINJA=false aapt`
4. `strip out-x32/host/linux-x86/bin/aapt`

#### Unix 64
1. `source build/envsetup.sh`
2. `lunch sdk-eng`
3. `make OUT_DIR=out-x64 LOCAL_MULTILIB=64 USE_NINJA=false aapt`
4. `strip out-x64/host/linux-x86/bin/aapt`

#### Windows
1. `source build/envsetup.sh`
2. `lunch sdk-eng`
3. `make PRODUCT-sdk-win_sdk USE_NINJA=false`
4. `strip out/host/windows-x86/bin/aapt.exe`

#### Mac 32
1. `source build/envsetup.sh`
2. `lunch sdk-eng`
3. `make OUT_DIR=out-x32 LOCAL_MULTILIB=32 USE_NINJA=false aapt`

#### Mac 64
1. `source build/envsetup.sh`
2. `lunch sdk-eng`
3. `make OUT_DIR=out-x64 LOCAL_MULTILIB=64 USE_NINJA=false aapt`
