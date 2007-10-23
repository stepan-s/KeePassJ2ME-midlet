package org.phoneid.keepassj2me;

// PhoneID
import org.phoneid.*;

import java.io.*;
import javax.microedition.lcdui.*;
import javax.microedition.midlet.*;

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;

public class KDBSelection implements CommandListener
{
    protected KeePassMIDlet midlet;
    private boolean isReady = false;
    private List list;
    private Displayable dspBACK;
    private int result = 0;
	
    public KDBSelection(KeePassMIDlet midlet)
    {
	String[] stringArray = {
	    "Download from web", 
	    "Use local copy in jar"
	};
	list = new List("Choose KDB Location", Choice.IMPLICIT,
			stringArray, null);
	// check whether the FileConnection API (part of JSR75) is available
	String fileConnVersion = System.getProperty("microedition.io.file.FileConnection.version");
	if (fileConnVersion != null) {
		System.out.println("Got FileConnection version "+fileConnVersion);
		list.append("Load from filesystem", null);
	}
	list.setCommandListener(this);
	list.addCommand(new Command("OK", Command.OK, 1));
	list.addCommand(new Command("Exit", Command.EXIT, 1));
	list.setSelectCommand(new Command("OK", Command.OK, 1));
	list.setCommandListener(this);
	this.midlet = midlet;
	
	// list.addCommand(midlet.CMD_EXIT);
	
	// Previous Display
	dspBACK = Display.getDisplay(midlet).getCurrent();
	
	// Display message
	System.out.println ("Display KDBSelection list");
	Display.getDisplay(midlet).setCurrent(list);
	System.out.println ("Displayed KDBSelection list");
    }
    
    public void commandAction(Command cmd, Displayable dsp)
    {
	System.out.println ("KDBSelection commandAction()");
	
	if(cmd.getCommandType() == Command.OK) {
	    result = list.getSelectedIndex();

	    // if web sync is disabled, don't let web sync selected
	    if (Definition.CONFIG_NO_WEB == true && result == 0)
		return;
	    
	    isReady = true;
	    synchronized(this){
		this.notify();
	    }			
	    // Display.getDisplay(midlet).setCurrent(dspBACK);
	} else if (cmd.getCommandType() == Command.EXIT) {
	    midlet.destroyApp(false);
	    midlet.notifyDestroyed();
	}
    }
    
    public int getResult() {
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
