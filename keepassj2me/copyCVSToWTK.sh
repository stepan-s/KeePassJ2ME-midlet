WTK_MIDLET_DIR="D:/WTK22/apps/KeePassJ2ME"

# make directories
mkdir -p $WTK_MIDLET_DIR/bin
mkdir -p $WTK_MIDLET_DIR/classes
mkdir -p $WTK_MIDLET_DIR/src/org/phoneid/keepassj2me

# cp -u MIDlet/project.properties copyCVSToWTK.sh copyWTKToCVS.sh $WTK_MIDLET_DIR
#cp -u MANIFEST.MF $WTK_MIDLET_DIR/bin/
# cp -u MIDlet/bin/PhoneIDMIDlet.jad $WTK_MIDLET_DIR/bin/
cp -ru src/org/phoneid/*.java $WTK_MIDLET_DIR/src/org/phoneid
cp -ru src/org/phoneid/keepassj2me/*.java $WTK_MIDLET_DIR/src/org/phoneid/keepassj2me
# rm $WTK_MIDLET_DIR/src/org/phoneid/HostUtil.java
cp -ru src/org/bouncycastle1 $WTK_MIDLET_DIR/src/org/
cp -ru res/images/*.png $WTK_MIDLET_DIR/res/images
cp -ru res/*.png $WTK_MIDLET_DIR/res


