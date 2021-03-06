/*
	Copyright 2008-2011 Stepan Strelets
	http://keepassj2me.sourceforge.net/

	This file is part of KeePass for J2ME.
	
	KeePass for J2ME is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, version 2.
	
	KeePass for J2ME is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.
	
	You should have received a copy of the GNU General Public License
	along with KeePass for J2ME.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sourceforge.keepassj2me;

import java.util.Date;

import javax.microedition.lcdui.*;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.ItemCommandListener;
import javax.microedition.lcdui.TextField;

import net.sourceforge.keepassj2me.L10nKeys.keys;
import net.sourceforge.keepassj2me.datasource.DataSourceAdapter;
import net.sourceforge.keepassj2me.datasource.DataSourceRegistry;
import net.sourceforge.keepassj2me.keydb.KeydbEntry;
import net.sourceforge.keepassj2me.keydb.KeydbUtil;
import net.sourceforge.keepassj2me.tools.DisplayStack;
import net.sourceforge.keepassj2me.tools.InputBox;

class ExitException extends Exception {}

/**
 * KDB Entry view
 * @author Stepan Strelets
 */
public class KeydbRecordView implements CommandListener, ItemCommandListener {
    protected Form form;
    protected StringItem note;
    protected ImageItem image;
    protected StringItem attachment;
    
    protected static final int EVENT_NONE = 0;
    protected static final int EVENT_CLOSE = 1;
    protected static final int EVENT_APPLY = 2;
    protected static final int EVENT_EDIT_NOTE = 3;
    protected static final int EVENT_CHANGE_IMAGE = 4;
    protected static final int EVENT_RESET_EXPIRE = 5;
    protected static final int EVENT_EXPORT_ATTACHMENT = 6;
    protected static final int EVENT_IMPORT_ATTACHMENT = 7;
    protected static final int EVENT_DELETE_ATTACHMENT = 8;
    protected static final int EVENT_EXIT = 9;
    protected int event = EVENT_NONE;
    
    protected Command cmdEditNote;
    protected Command cmdChangeImage;
    protected Command cmdReset;
    protected Command cmdExportAttachment;
    protected Command cmdImportAttachment;
    protected Command cmdDeleteAttachment;
    
    protected Command cmdOk;
    protected Command cmdCancel;
    protected Command cmdBack;
    protected Command cmdExit;
    
    /**
     * Construct and display form
     * 
     * @param entry <code>KeydbEntry</code>
     * @throws ExitException 
     */
    public KeydbRecordView(KeydbEntry entry) throws ExitException {
    	form = new Form(entry.title);
    	L10n lc = Config.getInstance().getLocale();
    	
    	cmdOk = new Command(lc.getString(keys.APPLY), Command.OK, 3);
    	cmdCancel = new Command(lc.getString(keys.CANCEL), Command.CANCEL, 2);
    	cmdBack = new Command(lc.getString(keys.BACK), Command.BACK, 2);
    	cmdExit = new Command(lc.getString(keys.EXIT), Command.EXIT, 4);
		form.addCommand(cmdOk);
		form.addCommand(cmdCancel);
		form.addCommand(cmdBack);
		form.addCommand(cmdExit);
    	form.setCommandListener(this);
    	
    	//Entry icon
    	int imageIndex = entry.imageIndex;
    	Image image = Icons.getInstance().getImageById(imageIndex, 0);
    	this.image = new ImageItem(lc.getString(keys.ICON), image, ImageItem.LAYOUT_DEFAULT, Integer.toString(imageIndex));
    	cmdChangeImage = new Command(lc.getString(keys.CHANGE), Command.ITEM, 1);
    	this.image.addCommand(cmdChangeImage);
    	this.image.setDefaultCommand(cmdChangeImage);
    	this.image.setItemCommandListener(this);
   		form.append(this.image);
	
    	TextField user = new TextField(lc.getString(keys.USERNAME), entry.getUsername(), 255, TextField.SENSITIVE);
    	form.append(user);
    	
    	TextField pass = new TextField(lc.getString(keys.PASSWORD), entry.getPassword(), 255, TextField.SENSITIVE);
    	form.append(pass);
    	
    	TextField url = new TextField(lc.getString(keys.URL), entry.getUrl(), 255, TextField.SENSITIVE);
    	form.append(url);
    	
    	TextField title = new TextField(lc.getString(keys.TITLE), entry.title, 255, TextField.ANY);
    	form.append(title);
    	
    	note = new StringItem(lc.getString(keys.NOTE), entry.getNote());
    	cmdEditNote = new Command(lc.getString(keys.EDIT), Command.ITEM, 1);
    	note.addCommand(cmdEditNote);
    	note.setDefaultCommand(cmdEditNote);
    	note.setItemCommandListener(this);
    	form.append(note);
    	
    	//attachment
		attachment = new StringItem(
			lc.getString(keys.ATTACHMENT),
			entry.binaryDataLength > 0
				? (entry.getBinaryDesc()+" ("+KeydbUtil.toPrettySize(entry.binaryDataLength)+")")
				: "-",
			Item.BUTTON
		);
		cmdExportAttachment = new Command(lc.getString(keys.EXPORT), Command.ITEM, 1);
		cmdImportAttachment = new Command(lc.getString(keys.IMPORT), Command.ITEM, 2);
		cmdDeleteAttachment = new Command(lc.getString(keys.DELETE), Command.ITEM, 2);
		attachment.addCommand(cmdExportAttachment);
		attachment.addCommand(cmdImportAttachment);
		attachment.addCommand(cmdDeleteAttachment);
		attachment.setDefaultCommand(cmdExportAttachment);
		attachment.setItemCommandListener(this);
		form.append(attachment);
    	
    	//Entry expire
    	DateField expire = new DateField(lc.getString(keys.EXPIRE), DateField.DATE_TIME);
    	cmdReset = new Command(lc.getString(keys.RESET), Command.ITEM, 1);
    	expire.addCommand(cmdReset);
    	expire.setItemCommandListener(this);
    	Date expireDate = entry.getExpire();
    	if (expireDate != null) {
    		expire.setDate(expireDate);
    	}
    	form.append(expire);

    	DisplayStack.getInstance().push(form);
    	
		try {
			while (true) {
				if (entry.getDB().isLocked()) break;
				this.event = EVENT_NONE;
				synchronized (this.form) {
					this.form.wait();
				}
				if (entry.getDB().isLocked()) break;
				entry.getDB().reassureWatchDog();
				
				if (this.event == EVENT_CLOSE) break;
				if (this.event == EVENT_APPLY) {
					entry.title = title.getString();
					entry.setUrl(url.getString());
					entry.setUsername(user.getString());
					entry.setPassword(pass.getString());
					entry.setNote(note.getText());
					entry.imageIndex = imageIndex;
					entry.setExpire(expire.getDate());
					entry.save();
					break;
				}
				if (this.event == EVENT_EXIT) {
					throw new ExitException();
				}
				
				switch(this.event) {
				case EVENT_EDIT_NOTE:
					InputBox val = new InputBox(lc.getString(keys.NOTE), note.getText(), 4096, TextField.PLAIN);
					if (val.getResult() != null) {
						note.setText(val.getResult());
					};
					break;
				case EVENT_CHANGE_IMAGE:
					ImageSelect sel = new ImageSelect();
					if (sel.select(imageIndex)) {
						imageIndex = sel.getSelectedImageIndex();
						this.image.setImage(Icons.getInstance().getImageById(imageIndex, 0));
					};
					break;
				case EVENT_RESET_EXPIRE:
					expire.setDate(null);
					break;
				case EVENT_EXPORT_ATTACHMENT:
					if (entry.binaryDataLength > 0) {
						DataSourceAdapter source;
						try {
							while(true) {
								source = DataSourceRegistry.selectSource(lc.getString(keys.ATTACHMENT), false, true);
								if (source.selectSave(lc.getString(keys.ATTACHMENT), entry.getBinaryDesc())) break;
								if (entry.getDB().isLocked()) throw new KeePassException(lc.getString(keys.DATABASE_LOCKED));
							}
						} catch (KeePassException e) {
							break;
						}
						source.save(entry.getBinaryData());
					};
					break;
				case EVENT_IMPORT_ATTACHMENT:
					DataSourceAdapter source;
					try {
						while(true) {
							source = DataSourceRegistry.selectSource(lc.getString(keys.ATTACHMENT), false, false);
							if (source.selectLoad(lc.getString(keys.ATTACHMENT))) break;
							if (entry.getDB().isLocked()) throw new KeePassException(lc.getString(keys.DATABASE_LOCKED));
						};
					} catch (KeePassException e) {
						break;
					}
					entry.setBinaryData(source.load());
					entry.setBinaryDesc(source.getName());
					attachment.setText(entry.getBinaryDesc()+" ("+KeydbUtil.toPrettySize(entry.binaryDataLength)+")");
					break;
				case EVENT_DELETE_ATTACHMENT:
					entry.setBinaryData(null);
					entry.setBinaryDesc(null);
					attachment.setText("-");
					break;
				}
			}
		} catch (ExitException e) {
			throw e;
		} catch (Exception e) {
		}
		DisplayStack.getInstance().pop();
    }
    protected void fireEvent(int event) {
    	this.event = event;
    	synchronized(this.form){
			this.form.notify();
		}
    }
    public void commandAction(Command cmd, Displayable dsp) {
    	if (cmd == cmdOk) {
    		fireEvent(EVENT_APPLY);
    	} else if (cmd == cmdCancel) {
    		fireEvent(EVENT_CLOSE);
    	} else if (cmd == cmdBack) {
    		fireEvent(EVENT_CLOSE);
    	} else if (cmd == cmdExit) {
    		fireEvent(EVENT_EXIT);
    	}
    }
	public void commandAction(Command cmd, Item item) {
		if (cmd == cmdEditNote) {
			fireEvent(EVENT_EDIT_NOTE);
		} else if (cmd == cmdChangeImage) {
			fireEvent(EVENT_CHANGE_IMAGE);
		} else if (cmd == cmdReset) {
			fireEvent(EVENT_RESET_EXPIRE);
		} else if (cmd == cmdExportAttachment) {
			fireEvent(EVENT_EXPORT_ATTACHMENT);
		} else if (cmd == cmdImportAttachment) {
			fireEvent(EVENT_IMPORT_ATTACHMENT);
		} else if (cmd == cmdDeleteAttachment) {
			fireEvent(EVENT_DELETE_ATTACHMENT);
		}
	}
}
