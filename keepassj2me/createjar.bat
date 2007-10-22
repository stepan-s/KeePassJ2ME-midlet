cp KeePassJ2ME.jar Database.kdb jar && cd jar && jar xvf KeePassJ2ME.jar && rm KeePassJ2ME.jar && jar cvfm KeePassJ2ME-KDB.jar META-INF/MANIFEST.MF -C . org images Database.kdb

