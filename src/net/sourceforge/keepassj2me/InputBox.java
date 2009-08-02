package net.sourceforge.keepassj2me;

// Java
import javax.microedition.lcdui.*;
import javax.microedition.midlet.MIDlet;

/**
 * UI for input text string
 * 
 * @author Unknown
 * @author Stepan Strelets
 */

public class InputBox implements CommandListener
{
    protected MIDlet midlet;
    private boolean isReady = false;
    private Displayable dspBACK;
    private String result = null;
    private TextBox tb = null;

    /**
     * Construct, display and wait
     * 
     * @param midlet Parent MIDlet object
     * @param title Title of text box
     * @param defaultValue Default value of text enter field
     * @param maxLen Max length of text enter field
     * @param type <code>TextField.NUMERIC</code>, <code>TextField.PASSWORD</code>, etc.
     */
    public InputBox(MIDlet midlet, String title, String defaultValue, int maxLen, int type) {
    	// #ifdef DEBUG
			System.out.println("InputBox");
		// #endif
		
		this.midlet = midlet;
	
		// Previous Display
		dspBACK = Display.getDisplay(midlet).getCurrent();
		
		tb = new TextBox(title, defaultValue, maxLen, type);
		tb.setCommandListener(this);
		tb.addCommand(new Command("OK", Command.OK, 1));
		tb.addCommand(new Command("Cancel", Command.CANCEL, 1));
		// Set Display
		Display.getDisplay(midlet).setCurrent(tb);
			
		// wait for it to be done
		waitForDone();
			
    }

    private void waitForDone() {
		try {
		    while(!isReady) {
				synchronized(this) {
				    this.wait();
				}
		    }
		} catch(Exception e) {
		}
    }

    public void commandAction(Command cmd, Displayable dsp) {
    	switch (cmd.getCommandType()) {
    	case Command.OK:
    		result = tb.getString();
    		break;
    		
    	case Command.CANCEL:
    		result = null;
    		break;
    		
    	default:
    		return;
    	}
    	
		// Return the the previous display
	    Display.getDisplay(midlet).setCurrent(dspBACK);
	
	    isReady = true;
	    synchronized(this) {
	    	this.notify();
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
