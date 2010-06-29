package net.sourceforge.keepassj2me.tools;

/**
 * Interface for `watchdog bark`
 * @author Stepan Strelets
 */
public interface IWatchDogTimerTarget {
	/**
	 * Watchdog timeout exeeded
	 */
	public void invokeByWatchDog();
}
