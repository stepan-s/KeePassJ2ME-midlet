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

import javax.microedition.lcdui.*;

import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;

import net.sourceforge.keepassj2me.Config;
import net.sourceforge.keepassj2me.Icons;
import net.sourceforge.keepassj2me.KeePassMIDlet;
import net.sourceforge.keepassj2me.L10nKeys.keys;

/**
 * Message box
 * 
 * @author Unknown
 * @author Stepan Strelets
 */
public class MessageBox implements CommandListener
{
    private Form form;
    private int result = -1;
    
    /**
     * Construct and display message box
     * 
     * @param title Title of message box
     * @param message Message
     * @param type Type of alert
     * @param yesno Commands <code>true</code> - "Yes|No", <code>false</code> - "OK" 
     * @param image Image
     */
    public MessageBox(String title, String message, AlertType type, boolean yesno, Image image) {
    	//TODO: Need refactor
    	
    	form = new Form(title);

    	if (image != null)
    		form.append(new ImageItem(null, image,
    				ImageItem.LAYOUT_CENTER | ImageItem.LAYOUT_NEWLINE_AFTER,
    				null));
	
    	form.append(message);

		if (yesno == true) {
		    form.addCommand(new Command(Config.getLocaleString(keys.YES), Command.OK, 1));
		    form.addCommand(new Command(Config.getLocaleString(keys.NO), Command.CANCEL, 2));
		} else {
		    form.addCommand(new Command(Config.getLocaleString(keys.OK), Command.OK, 1));
		    // addCommand(new Command("Cancel", Command.CANCEL, 2));
		}
		//form.addCommand(new Command("Exit", Command.STOP, 1));
    	form.setCommandListener(this);
    }
    
    public void commandAction(Command cmd, Displayable dsp) {
    	switch (cmd.getCommandType()) {
    	case Command.OK:
    	case Command.CANCEL:
    	case Command.STOP:
    		result = cmd.getCommandType();
	        break;
    	}
		synchronized(this.form){
			this.form.notify();
		}
    }
    
    /**
     * Get result 
     * @return result - command type
     */
    public int getResult() {
    	return result;
    }

    /**
     * Wait for user
     */
    public void displayAndWait() {
    	DisplayStack.getInstance().push(form);
    	try {
			synchronized(this.form) {
				this.form.wait();
		    }
	    } catch(Exception e) {
	    }
	    DisplayStack.getInstance().pop();
    }

	/**
	 * Show alert
	 * @param msg message text
	 */
	public static void showAlert(String msg) {
		MessageBox mb = new MessageBox(KeePassMIDlet.TITLE, msg, AlertType.ERROR, false, Icons.getInstance().getImageById(Icons.ICON_ALERT));
		mb.displayAndWait();
	}

	/**
	 * Show alert
	 * @param msg message text
	 * @return true on ok, false on cancel
	 */
	public static boolean showConfirm(String msg) {
		MessageBox mb = new MessageBox(KeePassMIDlet.TITLE, msg, AlertType.WARNING, true, Icons.getInstance().getImageById(Icons.ICON_ALERT));
		mb.displayAndWait();
		return (mb.getResult() == Command.OK);
	}
}
