package net.sourceforge.keepassj2me;

import java.io.UnsupportedEncodingException;

import javax.microedition.lcdui.*;
import javax.microedition.midlet.*;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;

import net.sourceforge.keepassj2me.importerv3.PwEntry;

/**
 * Message box
 * 
 * @author Stepan Strelets
 */
public class KDBRecordView implements CommandListener
{
    protected MIDlet midlet;
    private Form form;
    private Displayable dspBACK;
    
    /**
     * Construct and display message box
     * 
     * @param midlet Parent <code>KeePassMIDlet</code>
     * @param entry <code>PwEntry</code>
     */
    public KDBRecordView(MIDlet midlet, PwEntry entry) {
    	this.midlet = midlet;
    	
    	form = new Form(entry.title);
    	Image image = Icons.getInstance().getImageById(entry.imageId, 0);
    	
    	if (image != null)
    		form.append(new ImageItem(null, image, ImageItem.LAYOUT_DEFAULT, null));
	
    	form.append(new TextField("URL", entry.url, 255, TextField.ANY));
    	form.append(new TextField("User", entry.username, 255, TextField.ANY));
		try {//FIXME: Why ImporterV3 does not convert to UTF-8? 
	    	form.append(new TextField("Pass", new String(entry.getPassword(), "UTF-8"), 255, TextField.ANY));
		} catch (UnsupportedEncodingException e) {
	    	form.append(new StringItem("Pass", "password encoding error"));
		}
    	form.append(new StringItem("Note", entry.additional));
    	if (entry.tExpire != null) {
    		form.append(new StringItem("Expire", entry.tExpire.toString()));
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
