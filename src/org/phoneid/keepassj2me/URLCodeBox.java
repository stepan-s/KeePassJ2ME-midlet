package org.phoneid.keepassj2me;

// PhoneID utils
import org.phoneid.*;

// Java
import java.io.*;
import javax.microedition.lcdui.*;
import javax.microedition.midlet.*;

public class URLCodeBox implements CommandListener
{
    protected KeePassMIDlet midlet;
    private boolean isReady = false;
    private Displayable dspBACK;
    private String mURL = null, mUserCode = null, mPassCode = null, mEncCode = null;
    private Form form = null;
    private TextField urlField = null, userCodeField = null,
	passCodeField = null, encCodeField = null;
    private int mCommandType = 0;

    /**
     * title : title of entire window
     * midlet : parent MIDlet object
     * returnToPrevScreen : return to previous screen when the task is done
     */
    
    public URLCodeBox(String title, KeePassMIDlet midlet, boolean returnToPrevScreen)
    {
	form = new Form(title);
	this.midlet = midlet;

	System.out.println ("URLCodeBox - 1");
	
	urlField = new TextField("URL to download KDB from",
				 Definition.DEFAULT_KDB_URL,
				 Definition.MAX_TEXT_LEN, TextField.URL);
	form.append(urlField);

	System.out.println ("URLCodeBox - 2");

	userCodeField = new TextField("User Code", null, 
				      Definition.USER_CODE_LEN,
				      TextField.NUMERIC);
	form.append(userCodeField);

	passCodeField = new TextField("Pass Code", null, 
				      Definition.PASS_CODE_LEN,
				      TextField.NUMERIC);
	form.append(passCodeField);

	encCodeField = new TextField("Encryption Code", null, 
				      Definition.ENC_CODE_LEN,
				      TextField.NUMERIC);
	form.append(encCodeField);

	System.out.println ("URLCodeBox - 3");
	
	form.setCommandListener(this);
	form.addCommand(new Command("OK", Command.OK, 1));
	form.addCommand(new Command("Cancel", Command.CANCEL, 1));
	
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
	try {
	    while(!isReady) {
		synchronized(this) {
		    this.wait();
		}
	    }
	} catch(Exception e) {
	}
    }

    public void commandAction(Command cmd, Displayable dsp)
    {
	mCommandType = cmd.getCommandType();
	
	if(cmd.getCommandType() == Command.OK ||
	   cmd.getCommandType() == Command.CANCEL ||
	   cmd.getCommandType() == Command.ITEM) {
	    if(cmd.getCommandType() == Command.OK) {
		mURL = urlField.getString();
		mUserCode = userCodeField.getString();
		mPassCode = passCodeField.getString();
		mEncCode = encCodeField.getString();
	    } else {
		mURL = null;
		mUserCode = null;
		mPassCode = null;
		mEncCode = null;
	    }
	    
	    isReady = true;
	    
	    synchronized(this) {
		this.notify();
	    }			
	}
    }
    
    public String getURL() {
	return mURL;
    }

    public String getUserCode() {
	return mUserCode;
    }

    public String getPassCode() {
	return mPassCode;
    }

    public String getEncCode() {
	return mEncCode;
    }

    public int getCommandType() {
	return mCommandType;
    }
}
