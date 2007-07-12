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
    
    public PasswordBox(String title, String boxTitle, String defaultValue, int maxLen, KeePassMIDlet midlet, boolean returnToPrevScreen, int type)
    {
	//System.out.println ("PasswordBox 1");
	
	form = new Form(title);
		
	this.midlet = midlet;

	//System.out.println ("PasswordBox 2: " + defaultValue);
	txtField = new TextField(boxTitle, defaultValue, maxLen, type);
	//System.out.println ("PasswordBox 3");
	form.append(txtField);

	//System.out.println ("PasswordBox 4");
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
