package net.sourceforge.keepassj2me;

import java.util.Date;

import javax.microedition.lcdui.*;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.ItemCommandListener;
import javax.microedition.lcdui.TextField;

import net.sourceforge.keepassj2me.keydb.KeydbDatabase;
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
    
    protected static final int EVENT_NONE = 0;
    protected static final int EVENT_CLOSE = 1;
    protected static final int EVENT_EDIT_NOTE = 2;
    protected int event = EVENT_NONE;
    
    /**
     * Construct and display message box
     * 
     * @param midlet Parent <code>MIDlet</code>
     * @param entry <code>KeydbEntry</code>
     */
    public KeydbRecordView(KeydbDatabase db, KeydbEntry entry) {
    	form = new Form(entry.title);
    	Image image = Icons.getInstance().getImageById(entry.imageIndex, 0);
    	
    	if (image != null)
    		form.append(new ImageItem(null, image, ImageItem.LAYOUT_DEFAULT, null));
	
		form.append(new TextField("URL", entry.getUrl(db), 255, TextField.SENSITIVE));
    	form.append(new TextField("User", entry.getUsername(db), 255, TextField.SENSITIVE));
    	form.append(new TextField("Pass", entry.getPassword(db), 255, TextField.SENSITIVE));
    	note = new StringItem("Note", entry.getNote(db));
    	note.addCommand(new Command("Edit", Command.ITEM, 1));
    	note.setItemCommandListener(this);
    	form.append(note);
    	if (entry.binaryDataLength > 0) {
    		StringItem attachment = new StringItem("Attachment",
    			entry.getBinaryDesc(db)+" ("+(entry.binaryDataLength >= 1024 ? (entry.binaryDataLength/1024)+"kB)" : entry.binaryDataLength+"B)"),
    			Item.BUTTON); 
    		/*if () {
    			Command export = new Command("Export", Command.ITEM, 1);
    			attachment.addCommand(export);
    			attachment.setItemCommandListener(this);
    		};*/
    		form.append(attachment);
    	}
    	Date expire = entry.getExpire(db);
    	if (expire != null) {
    		form.append(new StringItem("Expire", expire.toString()));
    	}

    	Command back = new Command("Back", Command.BACK, 1);
		form.addCommand(back);
    	form.setCommandListener(this);
    	
    	DisplayStack.push(form);
    	
		try {
			while (true) {
				synchronized (this) {
					this.wait();
				}
				if (this.event == EVENT_CLOSE) break;
				
				switch(this.event) {
				case EVENT_EDIT_NOTE:
					InputBox val = new InputBox("Note", note.getText(), 4096, TextField.PLAIN);
					if (val.getResult() != null) {
						note.setText(val.getResult());
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
    	case Command.BACK:
    		fireEvent(EVENT_CLOSE);
	        break;
	    default:
	    	return;
    	}
    }
	public void commandAction(Command cmd, Item item) {
		if (cmd.getLabel().equals("Edit")) {
			if (item.equals(note)) fireEvent(EVENT_EDIT_NOTE);
		} /* else if (cmd.getLabel().equals("Export")) {
			if (item.equals(attachment)) fireEvent(EVENT_EXPORT);
		}*/
	}
}
