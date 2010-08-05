package net.sourceforge.keepassj2me;

import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.TextField;

import net.sourceforge.keepassj2me.L10nConstants.keys;
import net.sourceforge.keepassj2me.keydb.KeydbDatabase;
import net.sourceforge.keepassj2me.tools.DisplayStack;
import net.sourceforge.keepassj2me.tools.FileBrowser;

/**
 * Config UI 
 * @author Stepan Strelets
 */
public class ConfigUI extends Form implements CommandListener {
	private TextField lastDirField = null;
	private TextField downloadUrlField = null;
	private TextField watchDogTimeoutField = null;
	private TextField pageSizeField = null;
	private ChoiceGroup iconsDisabledField = null;
	private ChoiceGroup searchByField = null;
	private TextField encryptionRounds = null;
	private Config config = null;
	
	/**
	 * Create form
	 */
	public ConfigUI() {
		super(Config.getLocaleString(keys.SETUP));
		config = Config.getInstance();
		L10nResources lc = config.getLocale();
		
		downloadUrlField = new TextField(lc.getString(keys.DOWLOAD_URL), config.getDownloadUrl(), 250, TextField.URL);
		this.append(downloadUrlField);
		
		watchDogTimeoutField = new TextField(lc.getString(keys.WATCHDOG_TIMEOUT), String.valueOf(config.getWatchDogTimeOut()), 2, TextField.NUMERIC);
		this.append(watchDogTimeoutField);
		
		if (FileBrowser.isSupported()) {		
			lastDirField = new TextField(lc.getString(keys.LAST_DIR), config.getLastDir(), 250, TextField.URL);
			this.append(lastDirField);
		};
		
		pageSizeField = new TextField(lc.getString(keys.PAGE_SIZE), String.valueOf(config.getPageSize()), 3, TextField.NUMERIC);
		this.append(pageSizeField);
		
		iconsDisabledField = new ChoiceGroup(null, ChoiceGroup.MULTIPLE);
		iconsDisabledField.append(lc.getString(keys.DISABLE_ICONS), null);
		iconsDisabledField.setSelectedIndex(0, config.isIconsDisabled());
		this.append(iconsDisabledField);
	
		searchByField = new ChoiceGroup(lc.getString(keys.SEARCH_BY), ChoiceGroup.MULTIPLE);
		searchByField.append(lc.getString(keys.TITLE), null);
		searchByField.append(lc.getString(keys.URL), null);
		searchByField.append(lc.getString(keys.USERNAME), null);
		searchByField.append(lc.getString(keys.NOTE), null);
		byte searchBy = config.getSearchBy();
		searchByField.setSelectedIndex(0, (searchBy & KeydbDatabase.SEARCHBYTITLE) != 0);
		searchByField.setSelectedIndex(1, (searchBy & KeydbDatabase.SEARCHBYURL) != 0);
		searchByField.setSelectedIndex(2, (searchBy & KeydbDatabase.SEARCHBYUSERNAME) != 0);
		searchByField.setSelectedIndex(3, (searchBy & KeydbDatabase.SEARCHBYNOTE) != 0);
		this.append(searchByField);
	
		encryptionRounds = new TextField(lc.getString(keys.ENCRYPTION_ROUNDS), String.valueOf(config.getEncryptionRounds()), 10, TextField.NUMERIC);
		this.append(encryptionRounds);
		
		this.setCommandListener(this);
		this.addCommand(new Command(lc.getString(keys.OK), Command.OK, 1));
		this.addCommand(new Command(lc.getString(keys.CANCEL), Command.CANCEL, 1));
	}
	/**
	 * Show form and wait for user
	 */
	public void show() {
		DisplayStack.getInstance().push(this);
		try {
	    	synchronized(this) {
	    		this.wait();
	    	};
		} catch (InterruptedException e) {
		};
		DisplayStack.getInstance().pop();
	}

	public void commandAction(Command cmd, Displayable form) {
		switch(cmd.getCommandType()) {
		case Command.OK:
			config.setAutoSave(false);
			
			config.setDownloadUrl(downloadUrlField.getString());
			
			try {
				config.setWatchDogTimeout(Byte.parseByte(watchDogTimeoutField.getString()));
			} catch (NumberFormatException e) {
			};
			
			try {
				config.setPageSize(Byte.parseByte(pageSizeField.getString()));
			} catch (NumberFormatException e) {
			};
			
			if (lastDirField != null) config.setLastDir(lastDirField.getString());
			
			boolean[] states = new boolean[iconsDisabledField.size()];
			iconsDisabledField.getSelectedFlags(states);
			config.setIconsDisabled(states[0]);
			
			states = new boolean[searchByField.size()];
			searchByField.getSelectedFlags(states);
			config.setSearchBy((byte)(
				(states[0] ? KeydbDatabase.SEARCHBYTITLE : 0)
				| (states[1] ? KeydbDatabase.SEARCHBYURL : 0)
				| (states[2] ? KeydbDatabase.SEARCHBYUSERNAME : 0)
				| (states[3] ? KeydbDatabase.SEARCHBYNOTE : 0)
			));
			
			try {
				config.setEncryptionRounds(Integer.parseInt(encryptionRounds.getString()));
			} catch (NumberFormatException e) {
			};
			
			config.setAutoSave(true);
			config.save();
			
		case Command.CANCEL:
			synchronized(this) {
				this.notify();
			}
			break;
		};
	}
}
