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

// Java
import javax.microedition.lcdui.*;

import net.sourceforge.keepassj2me.Config;
import net.sourceforge.keepassj2me.L10nKeys.keys;

/**
 * UI for input text string
 * 
 * @author Unknown
 * @author Stepan Strelets
 */

public class InputBox implements CommandListener
{
    private String result = null;
    private TextBox tb = null;

    /**
     * Construct, display and wait
     * 
     * @param title Title of text box
     * @param defaultValue Default value of text enter field
     * @param maxLen Max length of text enter field
     * @param type <code>TextField.NUMERIC</code>, <code>TextField.PASSWORD</code>, etc.
     */
    public InputBox(String title, String defaultValue, int maxLen, int type) {
    	// #ifdef DEBUG
			System.out.println("InputBox");
		// #endif
		
		tb = new TextBox(title, defaultValue, maxLen, type);
		tb.setCommandListener(this);
		tb.addCommand(new Command(Config.getLocaleString(keys.OK), Command.OK, 1));
		tb.addCommand(new Command(Config.getLocaleString(keys.CANCEL), Command.CANCEL, 1));
		
		this.DisplayAndWait();
    }

    private void DisplayAndWait() {
    	DisplayStack.getInstance().push(tb);
		try {
			synchronized(this.tb) {
			    this.tb.wait();
			}
		} catch(Exception e) {
		}
		DisplayStack.getInstance().pop();
    }

    public void commandAction(Command cmd, Displayable dsp) {
    	switch (cmd.getCommandType()) {
    	case Command.OK:
    		result = tb.getString();
    		break;
    		
    	case Command.CANCEL:
    		result = null;
    		break;
    	}
    	
	    synchronized(this.tb) {
	    	this.tb.notify();
	    }			
    }

    /**
     * Get password
     * @return String with password or <code>null</code> if cancel 
     */
    public String getResult() {
    	return result;
    }
}
