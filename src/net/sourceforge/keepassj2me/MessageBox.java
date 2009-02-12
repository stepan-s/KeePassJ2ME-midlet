package net.sourceforge.keepassj2me;

import javax.microedition.lcdui.*;
import javax.microedition.midlet.*;

import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;

/**
 * Message box
 * 
 * @author Unknown
 * @author Stepan Strelets
 */
public class MessageBox implements CommandListener
{
    protected MIDlet midlet;
    private Form form;
    private Displayable dspBACK;
    private int result = -1;
    private boolean isReady = false;
    
    /**
     * Construct and display message box
     * 
     * @param title Title of message box
     * @param message Message
     * @param type Type of alert
     * @param midlet Parent midlet
     * @param yesno Commands <code>true</code> - "Yes|No", <code>false</code> - "OK" 
     * @param image Image
     */
    public MessageBox(String title, String message, AlertType type, MIDlet midlet, boolean yesno, Image image) {
    	//TODO: Need refactor
    	this.midlet = midlet;
    	
    	form = new Form(title);

    	if (image != null)
    		form.append(new ImageItem(null, image,
    				ImageItem.LAYOUT_CENTER | ImageItem.LAYOUT_NEWLINE_AFTER,
    				null));
	
    	form.append(message);

		if (yesno == true) {
		    form.addCommand(new Command("Yes", Command.OK, 1));
		    form.addCommand(new Command("No", Command.CANCEL, 2));
		} else {
		    form.addCommand(new Command("OK", Command.OK, 1));
		    // addCommand(new Command("Cancel", Command.CANCEL, 2));
		}
		//form.addCommand(new Command("Exit", Command.STOP, 1));
    	form.setCommandListener(this);
	
    	// Previous Display
    	dspBACK = Display.getDisplay(midlet).getCurrent();
	
    	//Display message
    	// #ifdef DEBUG
    		System.out.println ("Display message");
    	// #endif
    	Display.getDisplay(midlet).setCurrent(form);
    }
    
    public void commandAction(Command cmd, Displayable dsp) {
    	switch (cmd.getCommandType()) {
    	case Command.OK:
    	case Command.CANCEL:
    	case Command.STOP:
    		result = cmd.getCommandType();
    		isReady = true;
    		synchronized(this){
    			this.notify();
    		}
	        Display.getDisplay(midlet).setCurrent(dspBACK);
	        break;
	    default:
	    	return;
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
    public void waitForDone() {
    	try {
    		while(!isReady) {
    			synchronized(this) {
    				this.wait();
			    }
		    }
	    } catch(Exception e) {
	    	// #ifdef DEBUG
	    		System.out.println (e.toString());
	    	// #endif
	    }
    }
}
