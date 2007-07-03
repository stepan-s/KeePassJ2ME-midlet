@echo off

REM look for java.exe
set PROGNAME=java
@for %%e in (%PATHEXT%) do @for %%i in (%PROGNAME%%%e) do @if NOT "%%~$PATH:i"=="" GOTO FOUND_JAVA
echo Cannot find java.exe in the PATH.  Please install JDK and try again. 
EXIT 1

:FOUND_JAVA

REM look for jar.exe
set PROGNAME=jar
@for %%e in (%PATHEXT%) do @for %%i in (%PROGNAME%%%e) do @if NOT "%%~$PATH:i"=="" GOTO FOUND_JAR
echo Cannot find jar.exe in the PATH.  Please install JDK and try again. 
EXIT 1

:FOUND_JAR

java -cp KeePassInstaller.jar org.phoneid.keepassinstaller.KeePassInstaller
jar cvfm KeePassJ2ME-KDB.jar META-INF/MANIFEST.MF -C . Database.kdb org images/*.png

