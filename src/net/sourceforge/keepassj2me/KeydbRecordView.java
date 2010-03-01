package net.sourceforge.keepassj2me;

import java.util.Date;

import javax.microedition.lcdui.*;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.ItemCommandListener;
import javax.microedition.lcdui.TextField;

import net.sourceforge.keepassj2me.keydb.KeydbEntry;
import net.sourceforge.keepassj2me.tools.DisplayStack;
import net.sourceforge.keepassj2me.tools.InputBox;

/**
 * KDB Entry view
 * @author Stepan Strelets
 */
public class KeydbRecordView implements CommandListener, ItemCommandListener {
    protected Form form;
    protected StringItem note;
    protected ImageItem image;
    
    protected static final int EVENT_NONE = 0;
    protected static final int EVENT_CLOSE = 1;
    protected static final int EVENT_APPLY = 2;
    protected static final int EVENT_EDIT_NOTE = 3;
    protected static final int EVENT_CHANGE_IMAGE = 4;
    protected int event = EVENT_NONE;
    
    protected Command cmdEditNote;
    protected Command cmdChangeImage;
    
    /**
     * Construct and display message box
     * 
     * @param entry <code>KeydbEntry</code>
     */
    public KeydbRecordView(KeydbEntry entry) {
    	form = new Form(entry.title);
    	
    	int imageIndex = entry.imageIndex;
    	Image image = Icons.getInstance().getImageById(imageIndex, 0);
    	this.image = new ImageItem(null, image, ImageItem.LAYOUT_DEFAULT, null);
    	cmdChangeImage = new Command("Change", Command.ITEM, 1);
    	this.image.addCommand(cmdChangeImage);
    	this.image.setDefaultCommand(cmdChangeImage);
    	this.image.setItemCommandListener(this);
   		form.append(this.image);
	
    	TextField title = new TextField("Title", entry.title, 255, TextField.SENSITIVE);
    	form.append(title);
    	TextField url = new TextField("URL", entry.getUrl(), 255, TextField.SENSITIVE);
    	form.append(url);
    	TextField user = new TextField("User", entry.getUsername(), 255, TextField.SENSITIVE);
    	form.append(user);
    	TextField pass = new TextField("Pass", entry.getPassword(), 255, TextField.SENSITIVE);
    	form.append(pass);
    	
    	note = new StringItem("Note", entry.getNote());
    	cmdEditNote = new Command("Edit", Command.ITEM, 1);
    	note.addCommand(cmdEditNote);
    	note.setDefaultCommand(cmdEditNote);
    	note.setItemCommandListener(this);
    	form.append(note);
    	
    	if (entry.binaryDataLength > 0) {
    		StringItem attachment = new StringItem("Attachment",
    			entry.getBinaryDesc()+" ("+(entry.binaryDataLength >= 1024 ? (entry.binaryDataLength/1024)+"kB)" : entry.binaryDataLength+"B)"),
    			Item.BUTTON);
    		/*if () {
    			cmdExportAttachement = new Command("Export", Command.ITEM, 1);
    			attachment.addCommand(cmdExportAttachement);
    			attachment.setDefaultCommand(cmdExportAttachement);
    			attachment.setItemCommandListener(this);
    		};*/
    		form.append(attachment);
    	}
    	Date expire = entry.getExpire();
    	if (expire != null) {
    		form.append(new StringItem("Expire", expire.toString()));
    	}

    	Command cmdOk = new Command("Apply", Command.OK, 1);
    	Command cmdCancel = new Command("Cancel", Command.CANCEL, 1);
		form.addCommand(cmdOk);
		form.addCommand(cmdCancel);
    	form.setCommandListener(this);
    	
    	DisplayStack.push(form);
    	
		try {
			while (true) {
				synchronized (this) {
					this.wait();
				}
				if (this.event == EVENT_CLOSE) break;
				if (this.event == EVENT_APPLY) {
					entry.title = title.getString();
					entry.setUrl(url.getString());
					entry.setUsername(user.getString());
					entry.setPassword(pass.getString());
					entry.setNote(note.getText());
					entry.imageIndex = imageIndex;
					entry.save();
					break;
				}
				
				switch(this.event) {
				case EVENT_EDIT_NOTE:
					InputBox val = new InputBox("Note", note.getText(), 4096, TextField.PLAIN);
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
				}
			}
		} catch (Exception e) {
		}
		DisplayStack.pop();
    }
    protected void fireEvent(int event) {
    	this.event = event;
    	synchronized(this){
			this.notify();
		}
    }
    public void commandAction(Command cmd, Displayable dsp) {
    	switch (cmd.getCommandType()) {
    	case Command.CANCEL:
    		fireEvent(EVENT_CLOSE);
	        break;
    	case Command.OK:
    		fireEvent(EVENT_APPLY);
	        break;
	    default:
	    	return;
    	}
    }
	public void commandAction(Command cmd, Item item) {
		if (cmd == cmdEditNote) {
			fireEvent(EVENT_EDIT_NOTE);
		} else if (cmd == cmdChangeImage) {
			fireEvent(EVENT_CHANGE_IMAGE);
		} /* else if (cmd == cmdExportAttachement) {
			fireEvent(EVENT_EXPORT);
		}*/
	}
}
