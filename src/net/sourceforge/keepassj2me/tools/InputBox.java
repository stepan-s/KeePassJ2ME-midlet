package net.sourceforge.keepassj2me.tools;

// Java
import javax.microedition.lcdui.*;

/**
 * UI for input text string
 * 
 * @author Unknown
 * @author Stepan Strelets
 */

public class InputBox implements CommandListener
{
    private boolean isReady = false;
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
    public InputBox(String title, String defaultValue, int maxLen, int type) {
    	// #ifdef DEBUG
			System.out.println("InputBox");
		// #endif
		
		tb = new TextBox(title, defaultValue, maxLen, type);
		tb.setCommandListener(this);
		tb.addCommand(new Command("OK", Command.OK, 1));
		tb.addCommand(new Command("Cancel", Command.CANCEL, 1));
		
		this.DisplayAndWait();
    }

    private void DisplayAndWait() {
    	DisplayStack.push(tb);
		try {
		    while(!isReady) {
				synchronized(this) {
				    this.wait();
				}
		    }
		} catch(Exception e) {
		}
		DisplayStack.pop();
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