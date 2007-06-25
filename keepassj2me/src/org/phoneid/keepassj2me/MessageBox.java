package org.phoneid.keepassj2me;

import java.io.*;
import javax.microedition.lcdui.*;
import javax.microedition.midlet.*;

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;

public class MessageBox extends Alert implements CommandListener
{
	protected MIDlet midlet;
	
	private boolean isReady = false;
	
	private Displayable dspBACK;

        private boolean result = false;
	
	public MessageBox(String title, String text, AlertType type, MIDlet midlet, boolean yesno)
	{
	    super(title, text, null, type);

	    /*
	    System.out.println ("MessageBox(1)");
		
		this.midlet = midlet;
		
		this.setCommandListener(this);
		this.setTimeout(Alert.FOREVER);

		System.out.println ("MessageBox(2)");
		
		if (yesno == true) {
		    addCommand(new Command("Yes", Command.OK, 1));
		    addCommand(new Command("No", Command.CANCEL, 2));
		} else {
		    addCommand(new Command("OK", Command.OK, 1));
		    // addCommand(new Command("Cancel", Command.CANCEL, 2));
		}

		System.out.println ("MessageBox(3)");
		// Previous Display
		dspBACK = Display.getDisplay(midlet).getCurrent();
		
		System.out.println ("MessageBox(4)");
		// Show message box
		Display.getDisplay(midlet).setCurrent(this);

		System.out.println ("MessageBox(5)");
		// Attendi la conferma di chiusura
		waitForDone();

		System.out.println ("MessageBox(6)");
		// Visualizza il precedente Display
		// Display.getDisplay(midlet).setCurrent(dspBACK);
		*/
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
	    //		if(cmd == Alert.DISMISS_COMMAND)
	    if(cmd.getCommandType() == Command.OK ||
	       cmd.getCommandType() == Command.CANCEL)
		{
		    if(cmd.getCommandType() == Command.OK)
			result = true;
		    else 
			result = false;
		    
			isReady = true;
			
			synchronized(this)
			{
			    System.out.println ("Notify");
				this.notify();
			}			
		}
	}

    public boolean getResult() {
	return result;
    }
	
}
