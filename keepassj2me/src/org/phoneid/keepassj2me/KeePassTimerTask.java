package org.phoneid.keepassj2me;

import java.util.*;

class KeePassTimerTask extends TimerTask {

    KeePassMIDlet mParent = null;
    
    public void run(){
	System.out.println ("Timer invoked");
	mParent.openDatabaseAndDisplay();
    }

    public KeePassTimerTask(KeePassMIDlet parent)
    {
	mParent = parent;
    }
}

