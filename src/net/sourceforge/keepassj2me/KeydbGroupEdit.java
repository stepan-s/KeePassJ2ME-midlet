package net.sourceforge.keepassj2me;

import java.util.Date;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.ImageItem;
import javax.microedition.lcdui.StringItem;
import javax.microedition.lcdui.TextField;

import net.sourceforge.keepassj2me.keydb.KeydbGroup;
import net.sourceforge.keepassj2me.tools.DisplayStack;

public class KeydbGroupEdit implements CommandListener {
    protected Form form;
    
    protected static final int EVENT_NONE = 0;
    protected static final int EVENT_CLOSE = 1;
    protected static final int EVENT_APPLY = 2;
    protected int event = EVENT_NONE;
    
    /**
     * Construct and display message box
     * 
     * @param entry <code>KeydbEntry</code>
     */
    public KeydbGroupEdit(KeydbGroup group) {
    	form = new Form(group.name);
    	Image image = Icons.getInstance().getImageById(group.imageIndex, 0);
    	
    	if (image != null)
    		form.append(new ImageItem(null, image, ImageItem.LAYOUT_DEFAULT, null));
	
    	TextField title = new TextField("Title", group.name, 255, TextField.SENSITIVE);
    	form.append(title);
    	
    	Date expire = group.getExpire();
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
					group.name = title.getString();
					group.save();
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
}
