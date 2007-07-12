package org.phoneid.keepassj2me;

import java.io.*;
import javax.microedition.lcdui.*;
import javax.microedition.midlet.*;

public class PasswordBox implements CommandListener
{
    protected KeePassMIDlet midlet;
    private boolean isReady = false;
    private Displayable dspBACK;
    private String result = null;
    private Form form = null;
    private TextField txtField = null;
    
    public PasswordBox(String title, String boxTitle, int maxLen, KeePassMIDlet midlet, boolean returnToPrevScreen, int type)
    {
	form = new Form(title);
		
	this.midlet = midlet;

	txtField = new TextField(boxTitle, "", maxLen, type);
	form.append(txtField);
		
	form.setCommandListener(this);
	form.addCommand(new Command("OK", Command.OK, 1));
	form.addCommand(new Command("Cancel", Command.CANCEL, 2));
	
	// Previous Display
	dspBACK = Display.getDisplay(midlet).getCurrent();
	// Set Display
	Display.getDisplay(midlet).setCurrent(form);
		
	// wait for it to be done
	waitForDone();
		
	// Return the the previous display
	if (returnToPrevScreen == true) 
	    Display.getDisplay(midlet).setCurrent(dspBACK);
    }

    private void waitForDone()
    {
	try
	    {
		while(!isReady)
		    {
			synchronized(this)
			    {
				this.wait();
					
			    }
		    }
	    }
	catch(Exception error)
	    {
			
	    }
    }

    public void commandAction(Command cmd, Displayable dsp)
    {
	if(cmd.getCommandType() == Command.OK ||
	   cmd.getCommandType() == Command.CANCEL)
	    {
		if(cmd.getCommandType() == Command.OK)
		    result = txtField.getString();
		else 
		    result = null;
		    
		isReady = true;
			
		synchronized(this)
		    {
			this.notify();
		    }			
	    }
    }

    public String getResult() {
	return result;
    }	
}
