@echo off
if "%PATH_BASE%" == "" set PATH_BASE=%PATH%
set PATH=%CD%;%PATH_BASE%;
chcp 65001 2>nul >nul
FOR /F "tokens=*" %%a in ('dir /B ^| findstr /R "\<apktool_?*.jar"') do SET JARFILE=%%a
java -jar -Duser.language=en -Dfile.encoding=UTF8 "%~dp0\%JARFILE%" %*