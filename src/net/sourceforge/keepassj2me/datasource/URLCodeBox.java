package net.sourceforge.keepassj2me.datasource;

import javax.microedition.lcdui.*;
import javax.microedition.midlet.MIDlet;

import net.sourceforge.keepassj2me.KeePassException;
import net.sourceforge.keepassj2me.KeePassMIDlet;
import net.sourceforge.keepassj2me.tools.MessageBox;

/**
 * Form site parameters  
 * @author Unknown
 * @author Stepan Strelets
 */
public class URLCodeBox implements CommandListener {
    protected MIDlet midlet;
    private boolean isReady = false;
    private Form form = null;
    private TextField urlField = null;
    private TextField userCodeField = null;
    private TextField passCodeField = null;
    private TextField encCodeField = null;
    private int mCommandType = 0;
    private String message = null;
    
	public static final int MAX_TEXT_LEN = 128;
    public static final int USER_CODE_LEN = 4;
    public static final int PASS_CODE_LEN = 4;
    public static final int ENC_CODE_LEN = 16;
    
    /**
     * Construct form 
     * @param title title of form
     * @param midlet parent MIDlet object
     */
    public URLCodeBox(String title, MIDlet midlet) {
    	this.midlet = midlet;
    	
    	form = new Form(title);

		urlField = new TextField("URL to download KDB from", null,
					 		URLCodeBox.MAX_TEXT_LEN, TextField.URL);
		form.append(urlField);
	
		userCodeField = new TextField("User Code", null, 
							URLCodeBox.USER_CODE_LEN,
							TextField.NUMERIC);
		form.append(userCodeField);
	
		passCodeField = new TextField("Pass Code", null, 
							URLCodeBox.PASS_CODE_LEN,
							TextField.NUMERIC);
		form.append(passCodeField);
	
		encCodeField = new TextField("Encryption Code", null, 
							URLCodeBox.ENC_CODE_LEN,
							TextField.NUMERIC);
		form.append(encCodeField);
	
		form.setCommandListener(this);
		form.addCommand(new Command("OK", Command.OK, 1));
		form.addCommand(new Command("Cancel", Command.CANCEL, 1));
    }
    
    private void checkFields() throws KeePassException {
		if (urlField.getString().length() == 0)
			throw new KeePassException("Enter URL");
		
		if (userCodeField.getString().length() != URLCodeBox.USER_CODE_LEN)
			throw new KeePassException("User code length must be " + URLCodeBox.USER_CODE_LEN);
			
		if (passCodeField.getString().length() != URLCodeBox.PASS_CODE_LEN)
			throw new KeePassException("Pass code length must be " + URLCodeBox.PASS_CODE_LEN);
		
		if (encCodeField.getString().length() != URLCodeBox.ENC_CODE_LEN)
			throw new KeePassException("Enc code length must be " + URLCodeBox.ENC_CODE_LEN);
    }
    
    /**
     * Display form and wait for user input
     * @param returnToPrevScreen return to previous screen when the task is done
     */
    public void display() {
		// #ifdef DEBUG
			System.out.println ("URLCodeBox display");
		// #endif
	
		// Previous Display
		Displayable dspBACK = Display.getDisplay(midlet).getCurrent();
    	
    	isReady = false;
		// Set Display
		Display.getDisplay(midlet).setCurrent(form);
		
		// wait for it to be done
		waitForDone();
			
		// Return the the previous display
		Display.getDisplay(midlet).setCurrent(dspBACK);
    }
    
    /**
     * Wait for user input
     */
    private void waitForDone() {
		try {
		    while(!isReady) {
		    	synchronized(this) {
		    		this.wait();
		    	}
		    	if (!isReady && this.message != null) {
					MessageBox msg = new MessageBox(KeePassMIDlet.TITLE, this.message, AlertType.ERROR, this.midlet, false, null);
					this.message = null;
					msg.waitForDone();
		    	}
		    }
		} catch(Exception e) {
		}
    }
	
    public void commandAction(Command cmd, Displayable dsp) {
		mCommandType = cmd.getCommandType();
		
		switch(mCommandType) {
		case Command.OK:
		case Command.ITEM:
			try {
				this.checkFields();
				isReady = true;
			} catch (KeePassException e) {
				this.message = e.getMessage();
			}
			break;
			
		case Command.CANCEL:
			isReady = true;
			break;
			
		default:
			return;
		};
		
	    synchronized(this) {
	    	this.notify();
	    };
    }
    
    /**
     * Get URL
     * @return String URL
     */
    public String getURL() {
    	return urlField.getString();
    }
    
    /**
     * Set URL
     * @param URL String url
     */
    public void setURL(String URL) {
    	urlField.setString(URL);
    }

    /**
     * Get user login
     * @return String user login
     */
    public String getUserCode() {
    	return userCodeField.getString();
    }

    /**
     * Get user password 
     * @return String user password
     */
    public String getPassCode() {
    	return passCodeField.getString();
    }

    /**
     * Get encryption code 
     * @return String encryption code
     */
    public String getEncCode() {
    	return encCodeField.getString();
    }

    /**
     * Get form result
     * @return
     */
    public int getCommandType() {
    	return mCommandType;
    }
}
