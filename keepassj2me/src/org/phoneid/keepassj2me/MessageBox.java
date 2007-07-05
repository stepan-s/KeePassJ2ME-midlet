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
	
	private boolean isReady = false;

    private Form form;
	
	private Displayable dspBACK;

        private boolean result = false;
	
    /*public MessageBox(String title, String text, AlertType type, KeePassMIDlet midlet, boolean yesno)
    {
	String[] texts = new String[1];
	texts[0] = text;
	
	this(title, texts, type, midlet, yesno);
	}*/
    
    public MessageBox(String title, String[] messages, AlertType type, KeePassMIDlet midlet, boolean yesno)
	{
	    // super(title, text, null, type);
	    form = new Form(title);

	    for (int i=0; i<messages.length; i++)
		form.append(messages[i]);
		
		this.midlet = midlet;
		
		form.setCommandListener(this);
		// form.setTimeout(Alert.FOREVER);

		if (yesno == true) {
		    form.addCommand(new Command("Yes", Command.OK, 1));
		    form.addCommand(new Command("No", Command.CANCEL, 2));
		} else {
		    form.addCommand(new Command("OK", Command.OK, 1));
		    // addCommand(new Command("Cancel", Command.CANCEL, 2));
		}
		
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

		System.out.println ("isReady is set to true");
		
		
		Display.getDisplay(midlet).setCurrent(dspBACK);
		
	    }
	}

    public boolean getResult() {
	return result;
    }
	
}
