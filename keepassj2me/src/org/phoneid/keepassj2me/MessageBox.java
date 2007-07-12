package org.phoneid.keepassj2me;

import java.io.*;
import javax.microedition.lcdui.*;
import javax.microedition.midlet.*;

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;

public class MessageBox implements CommandListener
{
    protected KeePassMIDlet midlet;
    private Form form;
    private Displayable dspBACK;
    private boolean result = false;
    private boolean isReady = false;
    
    public MessageBox(String title, String message, AlertType type, KeePassMIDlet midlet, boolean yesno)
    {
	form = new Form(title);
	
	form.append(message);
	
	this.midlet = midlet;
	
	form.setCommandListener(this);

	if (yesno == true) {
	    form.addCommand(new Command("Yes", Command.OK, 1));
	    form.addCommand(new Command("No", Command.CANCEL, 2));
	} else {
	    form.addCommand(new Command("OK", Command.OK, 1));
	    // addCommand(new Command("Cancel", Command.CANCEL, 2));
	}
	
	form.addCommand(midlet.CMD_EXIT);
	
	// Previous Display
	dspBACK = Display.getDisplay(midlet).getCurrent();
	
	// Display message
	System.out.println ("Display message");
	Display.getDisplay(midlet).setCurrent(form);
    }
    
    public void commandAction(Command cmd, Displayable dsp)
    {
	//		if(cmd == Alert.DISMISS_COMMAND)
	if(cmd.getCommandType() == Command.OK ||
	   cmd.getCommandType() == Command.CANCEL) {
	    if(cmd.getCommandType() == Command.OK)
		result = true;
	    else 
		result = false;

	    isReady = true;
	    synchronized(this){
		this.notify();
	    }
	    
	    Display.getDisplay(midlet).setCurrent(dspBACK);
	    
	} else if (cmd == midlet.CMD_EXIT) {
	    midlet.destroyApp(false);
	    midlet.notifyDestroyed();
	}
    }
    
    public boolean getResult() {
	return result;
    }

    public void waitForDone()
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
	catch(Exception e) {
	    System.out.println (e.toString());
	}
    }

}
