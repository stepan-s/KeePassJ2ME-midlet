/*
	Copyright 2007 Naomaru Itoi
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

package net.sourceforge.keepassj2me;

import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

import net.sourceforge.keepassj2me.tools.DisplayStack;

/**
 * Keepassj2me midlet
 * 
 * @author Naomaru Itoi
 * @author Stepan Strelets
 */
public class KeePassMIDlet extends MIDlet {
	/** Midlet title */
	public static final String TITLE = "KeePass for J2ME";
	private KeePassMIDletThread thread = null;
	
	/**
	 * Constructor
	 */
	public KeePassMIDlet() {
	}

	/**
	 * Exit
	 */
	public void exit() {
		notifyDestroyed();
	}
	
	/* Methods invoked by AMS */
	
	public void startApp() {
		if (thread == null) {
			new DisplayStack(this);
			DisplayStack.getInstance().pushSplash();
			thread = new KeePassMIDletThread(this);
			thread.start();
		}
	}

	public void pauseApp() {
	}

	public void destroyApp(boolean unconditional) throws MIDletStateChangeException {
		exit();
	}
}
