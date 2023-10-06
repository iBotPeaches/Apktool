@echo off
setlocal
set BASENAME=apktool_
chcp 65001 2>nul >nul

set java_exe=java.exe

if defined JAVA_HOME (
set "java_exe=%JAVA_HOME%\bin\java.exe"
)

rem Find the highest version .jar available in the same directory as the script
setlocal EnableDelayedExpansion
pushd "%~dp0"
if exist apktool.jar (
    set BASENAME=apktool
    goto skipversioned
)
set max=0
for /f "tokens=1* delims=-_.0" %%A in ('dir /b /a-d %BASENAME%*.jar') do if %%~B gtr !max! set max=%%~nB
:skipversioned
popd
setlocal DisableDelayedExpansion

rem Find out if the commandline is a parameterless .jar or directory, for fast unpack/repack
if "%~1"=="" goto load
if not "%~2"=="" goto load
set ATTR=%~a1
if "%ATTR:~0,1%"=="d" (
    rem Directory, rebuild
    set fastCommand=b
)
if "%ATTR:~0,1%"=="-" if "%~x1"==".apk" (
    rem APK file, unpack
    set fastCommand=d
)

:load
"%java_exe%" -jar -Xmx1024M -Duser.language=en -Dfile.encoding=UTF8 -Djdk.util.zip.disableZip64ExtraFieldValidation=true -Djdk.nio.zipfs.allowDotZipEntry=true "%~dp0%BASENAME%%max%.jar" %fastCommand% %*

rem Pause when ran non interactively
for %%i in (%cmdcmdline%) do if /i "%%~i"=="/c" pause & exit /b
