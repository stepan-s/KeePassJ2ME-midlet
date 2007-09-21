KeePass for J2ME
nao@phoneid.org

* This package requires JDK-1.6.0 or above to be installed.  java.exe and jar.exe must be in the path.

- unzip KeePassJ2ME-*.zip
- Run KeePassInstaller.bat
- Choose your KeePass Key Database File, Database.kdb (or in a different name)
- This will create KeePassJ2ME-KDB.jar.  Download this file to your J2ME phone through USB, Bluetooth, etc.
  - Note that you do not have to download Database.kdb, or any other key database file, in addition to KeePassJ2ME-KDB.jar.  The jar file contains the kdb file you have chosen. 
- On your J2ME phone, launch KeePass for J2ME.

* Phone specific information

- Motorola RAZR V3

Use MIDway to download KeePassJ2ME-KDB.jar.  Use the jad file in the package.
If the jad file does not work, use JADMAker to create a new one.

If it still does not work, check this (http://sourceforge.net/forum/forum.php?thread_id=1826315&forum_id=709880).

"So I used JADMAker to create a JAD file, then loaded it in the emulator, and I got 'Invalid application' error messages on load. I noticed that the MIDlet name was truncated. Looked at the Manifest file, and there was a <CR> in the middle of the name. Changed the manifest file, no difference. Te\hen I noticed that the JAD file had the same thing. A hard carriage return at org.phoneid.keepassj2me.KeePas So I editted the JAD file to remove that carriage return, and the app worked. I was able to download it to the phone using MIDway, and the app worked there as well. Just to be sure I went back to the original manifest file, and that didn't make a difference. In this case, I guess it really wants a JAD file for MIDway, and it really wants the full entrypoint name on one line. I can live with that. "

