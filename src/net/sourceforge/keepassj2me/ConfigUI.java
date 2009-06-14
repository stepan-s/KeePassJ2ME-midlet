package net.sourceforge.keepassj2me;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.TextField;
import javax.microedition.midlet.MIDlet;

public class ConfigUI extends Form implements CommandListener {
	private TextField lastDirField = null;
	private TextField downloadUrlField = null;
	private TextField watchDogTimeoutField = null;
	private Config config = null;
	private MIDlet midlet;
	
	public ConfigUI(MIDlet midlet) {
		super("Setup");
		config = Config.getInstance();
		this.midlet = midlet;
		
		downloadUrlField = new TextField("URL to download KDB from", config.getDownloadUrl(), Definition.MAX_TEXT_LEN, TextField.URL);
		this.append(downloadUrlField);
		watchDogTimeoutField = new TextField("Watchdog timeout, minutes", String.valueOf(config.getWathDogTimeOut()), 2, TextField.NUMERIC);
		this.append(watchDogTimeoutField);
		if (FileBrowser.isSupported()) {		
			lastDirField = new TextField("Last dir", config.getLastDir(), Definition.MAX_TEXT_LEN, TextField.URL);
			this.append(lastDirField);
		};
	
		this.setCommandListener(this);
		this.addCommand(new Command("OK", Command.OK, 1));
		this.addCommand(new Command("Cancel", Command.CANCEL, 1));
	}
	public void show() {
		Displayable dspBACK = Display.getDisplay(midlet).getCurrent();
		Display.getDisplay(midlet).setCurrent(this);
	
		try {
	    	synchronized(this) {
	    		this.wait();
	    	};
		} catch (InterruptedException e) {
		};
		
		Display.getDisplay(midlet).setCurrent(dspBACK);
	}

	public void commandAction(Command cmd, Displayable form) {
		switch(cmd.getCommandType()) {
		case Command.OK:
			config.setDownloadUrl(downloadUrlField.getString());
			try {
				config.setWathDogTimeout(Byte.parseByte(watchDogTimeoutField.getString()));
			} catch (NumberFormatException e) {
			};
			if (lastDirField != null) config.setLastDir(lastDirField.getString());
			
		case Command.CANCEL:
			synchronized(this) {
				this.notify();
			}
			break;
		};
	}
}
