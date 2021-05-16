@echo off
setlocal
set BASENAME=apktool_
chcp 65001 2>nul >nul

set java_exe=java.exe

if defined JAVA_HOME (
set java_exe="%JAVA_HOME%\bin\java.exe"
) else ( rem in case not defined JAVA_HOME
rem to work with jre 1.8 installed by IntelliJ IDEA in default folders uncomment the next line (with specific one )
rem set java_exe= "%UserProfile%\.jdks\corretto-1.8.0_292\jre\bin\java.exe"
rem to work with any jre installed by IntelliJ IDEA in default folders uncomment the next line
rem for /d %%D in ("%UserProfile%\.jdks\*") do set java_exe= "%%~fD\jre\bin\java.exe"

rem to work with jdk 16 uncomment the next line (with specific one )
rem set java_exe= "C:\Program Files\Java\jdk-16.0.1\bin\java.exe"
rem to work with any jdk present uncomment the next line
rem set java_exe= "C:\Program Files\Java\j*\bin\java.exe" -dont work
for /d %%D in ("C:\Program Files\Java\jdk*") do set java_exe= "%%~fD\bin\java.exe"
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
%java_exe% -jar -Duser.language=en -Dfile.encoding=UTF8 "%~dp0%BASENAME%%max%.jar" %fastCommand% %*

rem Pause when ran non interactively
for /f "tokens=2" %%# in ("%cmdcmdline%") do if /i "%%#" equ "/c" pause
