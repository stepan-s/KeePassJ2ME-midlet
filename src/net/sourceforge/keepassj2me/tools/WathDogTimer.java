package net.sourceforge.keepassj2me.tools;

import java.util.*;

/**
 * Watch dog timer
 * 
 * @author Unknown
 * @author Stepan Strelets
 *
 */
public class WathDogTimer {
	TimerTask task = null;
	Timer timer = null;
	private IWathDogTimerTarget target = null;
    
	/**
	 * Construct watch dog timer
	 * 
	 * @param target <code>IWathDogTimerTarget</code> to watch
	 */
    public WathDogTimer(IWathDogTimerTarget target) {
    	this.target = target;
    }
    
    public void setTimer(long delay) {
		this.cancelTimer();
		timer = new Timer();
		task = new TimerTask() {
			public void run() {
		    	target.invokeByWathDog();
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

