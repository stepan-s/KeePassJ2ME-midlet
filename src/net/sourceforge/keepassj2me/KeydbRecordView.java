package net.sourceforge.keepassj2me;

import java.util.Date;

import javax.microedition.lcdui.*;
import javax.microedition.midlet.*;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;

import net.sourceforge.keepassj2me.keydb.KeydbDatabase;
import net.sourceforge.keepassj2me.keydb.KeydbEntry;

/**
 * KDB Entry view
 * @author Stepan Strelets
 */
public class KeydbRecordView implements CommandListener {
    protected MIDlet midlet;
    private Form form;
    private Displayable dspBACK;
    
    /**
     * Construct and display message box
     * 
     * @param midlet Parent <code>MIDlet</code>
     * @param entry <code>KeydbEntry</code>
     */
    public KeydbRecordView(MIDlet midlet, KeydbDatabase db, KeydbEntry entry) {
    	this.midlet = midlet;
    	
    	form = new Form(entry.title);
    	Image image = Icons.getInstance().getImageById(entry.imageIndex, 0);
    	
    	if (image != null)
    		form.append(new ImageItem(null, image, ImageItem.LAYOUT_DEFAULT, null));
	
		form.append(new TextField("URL", entry.getUrl(db), 255, TextField.SENSITIVE));
    	form.append(new TextField("User", entry.getUsername(db), 255, TextField.SENSITIVE));
    	form.append(new TextField("Pass", entry.getPassword(db), 255, TextField.SENSITIVE));
    	form.append(new StringItem("Note", entry.getNote(db)));
    	if (entry.binaryDataLength > 0) {
    		StringItem attachment = new StringItem("Attachment",
    			entry.getBinaryDesc(db)+" ("+(entry.binaryDataLength >= 1024 ? (entry.binaryDataLength/1024)+"kB)" : entry.binaryDataLength+"B)"),
    			Item.BUTTON); 
    		/*if () {
    			Command export = new Command("Export", Command.ITEM, 1);
    			attachment.addCommand(export);
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
	
    	dspBACK = Display.getDisplay(midlet).getCurrent();
    	Display.getDisplay(midlet).setCurrent(form);
    }
    public void commandAction(Command cmd, Displayable dsp) {
    	switch (cmd.getCommandType()) {
    	case Command.BACK:
    		synchronized(this){
    			this.notify();
    		}
	        Display.getDisplay(midlet).setCurrent(dspBACK);
	        break;
    	/*case Command.ITEM:
    		break;*/
	    default:
	    	return;
    	}
    }
}
