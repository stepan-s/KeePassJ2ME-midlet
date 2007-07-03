set INSTALL_DIR=KeePassJ2ME
set WTK_HOME=D:\WTK22

rm -rf %INSTALL_DIR%
mkdir %INSTALL_DIR%
javac -d class src/org/phoneid/keepassinstaller/*.java
jar cvf %INSTALL_DIR%/KeePassInstaller.jar -C class org
cp -r doc/README.txt KeePassInstaller.bat res/images %WTK_HOME%/apps/KeePassJ2ME/bin/KeePassJ2ME.jar %INSTALL_DIR%
cd %INSTALL_DIR%
jar xvf KeePassJ2ME.jar
rm KeePassJ2ME.jar
cd ..
