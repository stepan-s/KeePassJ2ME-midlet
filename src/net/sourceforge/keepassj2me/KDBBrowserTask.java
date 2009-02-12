package net.sourceforge.keepassj2me;

import java.util.*;

/**
 * Watch dog timer
 * 
 * @author Unknown
 * @author Stepan Strelets
 *
 */
class KDBBrowserTask extends TimerTask {
	private KDBBrowser mParent = null;
    
	/**
	 * Construct watch dog timer
	 * 
	 * @param parent <code>KDBBrowser</code> to watch
	 */
    public KDBBrowserTask(KDBBrowser parent) {
    	mParent = parent;
    }
    public void run(){
    	// #ifdef DEBUG
    		System.out.println ("KDBBrowser timer invoked");
    	// #endif
    	mParent.stop();
    }
}

