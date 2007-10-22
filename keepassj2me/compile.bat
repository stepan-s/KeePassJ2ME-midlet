@echo off

set INSTALL_DIR=KeePassJ2ME-%1

echo %INSTALL_DIR%

set WTK_HOME=D:\WTK22

REM Create package

rm -rf %INSTALL_DIR%
mkdir %INSTALL_DIR%
javac -d class src/org/phoneid/keepassinstaller/*.java
jar cvf %INSTALL_DIR%/KeePassInstaller.jar -C class org
cp -r doc/README.txt KeePassInstaller.bat res/images %WTK_HOME%/apps/KeePassJ2ME/bin/KeePassJ2ME.jar %WTK_HOME%/apps/KeePassJ2ME/bin/KeePassJ2ME.jad %INSTALL_DIR%
cd %INSTALL_DIR%
jar xvf KeePassJ2ME.jar
echo please get rid of carriage return in %INSTALL_DIR%/META-INF/MANIFEST.MF
pause
rm KeePassJ2ME.jar
cd ..
zip -r %INSTALL_DIR%.zip %INSTALL_DIR%

REM Create source tar ball
tar cvfz KeePassJ2ME-src-%1.tar.gz src copyCVSToWTK.sh
rm -rf %INSTALL_DIR%
