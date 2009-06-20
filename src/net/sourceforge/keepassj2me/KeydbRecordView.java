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
     * @param midlet Parent <code>KeePassMIDlet</code>
     * @param entry <code>KeydbEntry</code>
     */
    public KeydbRecordView(KeePassMIDlet midlet, KeydbDatabase db, KeydbEntry entry) {
    	this.midlet = midlet;
    	
    	form = new Form(entry.title);
    	Image image = midlet.getImageById(entry.imageIndex, 0);
    	
    	if (image != null)
    		form.append(new ImageItem(null, image, ImageItem.LAYOUT_DEFAULT, null));
	
		form.append(new TextField("URL", entry.getUrl(db), 255, TextField.ANY));
    	form.append(new TextField("User", entry.getUsername(db), 255, TextField.ANY));
    	form.append(new TextField("Pass", entry.getPassword(db), 255, TextField.ANY));
    	form.append(new StringItem("Note", entry.getNote(db)));
    	if (entry.binaryDataLength > 0) {
    		form.append(new StringItem("Attachement",
    				entry.getBinaryDesc(db)+" ("+(entry.binaryDataLength >= 1024 ? (entry.binaryDataLength/1024)+"kB)" : entry.binaryDataLength+"B)"),
    				Item.BUTTON));
    	}
    	Date expire = entry.getExpire(db);
    	if (expire != null) {
    		form.append(new StringItem("Expire", expire.toString()));
    	}

		form.addCommand(new Command("Back", Command.BACK, 1));
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
	    default:
	    	return;
    	}
    }
}
