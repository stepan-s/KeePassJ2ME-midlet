@echo off

set INSTALL_DIR=KeePassUploader-%1

echo %INSTALL_DIR%

set WTK_HOME=D:\WTK22

REM Create package

rm -rf %INSTALL_DIR%
mkdir %INSTALL_DIR%
cp uploader/release/KeePassUploader.exe uploader/KeePassUploader/TermsOfService.txt %INSTALL_DIR%

zip -r %INSTALL_DIR%.zip %INSTALL_DIR%

REM Create source tar ball
cd uploader
zip -r KeePassUploader-src-%1.zip *.sln */*.cpp */*.h */*.vcproj */*.rc */*.txt */*/*.ico
mv KeePassUploader-src-%1.zip ..
cd ..
rm -rf %INSTALL_DIR%
