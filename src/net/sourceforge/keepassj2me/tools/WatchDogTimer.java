package net.sourceforge.keepassj2me.tools;

import java.util.*;

/**
 * Watch dog timer
 * 
 * @author Unknown
 * @author Stepan Strelets
 *
 */
public class WatchDogTimer {
	TimerTask task = null;
	Timer timer = null;
	private IWatchDogTimerTarget target = null;
    
	/**
	 * Construct watch dog timer
	 * 
	 * @param target <code>IWatchDogTimerTarget</code> to watch
	 */
    public WatchDogTimer(IWatchDogTimerTarget target) {
    	this.target = target;
    }
    
    public void setTimer(long delay) {
		this.cancelTimer();
		timer = new Timer();
		task = new TimerTask() {
			public void run() {
		    	target.invokeByWatchDog();
			}
		};
		timer.schedule(task, delay);
    }
    public void cancelTimer() {
    	if (timer != null) {
    		timer.cancel();
    		timer = null;
    	};
    	if (task != null) {
    		task = null;
    	}
    }
}

