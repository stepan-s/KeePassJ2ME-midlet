/*
	Copyright 2008-2011 Stepan Strelets
	http://keepassj2me.sourceforge.net/

	This file is part of KeePass for J2ME.
	
	KeePass for J2ME is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, version 2.
	
	KeePass for J2ME is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.
	
	You should have received a copy of the GNU General Public License
	along with KeePass for J2ME.  If not, see <http://www.gnu.org/licenses/>.
 */

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
    
    /**
     * Reset watchdog timeout
     * @param delay milliseconds
     */
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
    /**
     * Cancel timer
     */
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

