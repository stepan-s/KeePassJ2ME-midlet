package net.sourceforge.keepassj2me;

import java.util.Date;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.ImageItem;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.ItemCommandListener;
import javax.microedition.lcdui.StringItem;
import javax.microedition.lcdui.TextField;

import net.sourceforge.keepassj2me.keydb.KeydbGroup;
import net.sourceforge.keepassj2me.tools.DisplayStack;

/**
 * Form for group edit
 * @author Stepan Strelets
 * */
public class KeydbGroupEdit implements CommandListener, ItemCommandListener {
    protected Form form;
    protected ImageItem image;
    
    protected static final int EVENT_NONE = 0;
    protected static final int EVENT_CLOSE = 1;
    protected static final int EVENT_APPLY = 2;
    protected static final int EVENT_CHANGE_IMAGE = 3;
    protected int event = EVENT_NONE;

    protected Command cmdChangeImage;
    
    protected Command cmdOk;
    protected Command cmdCancel;

    /**
     * Construct and display form
     * 
     * @param entry <code>KeydbEntry</code>
     */
    public KeydbGroupEdit(KeydbGroup group) {
    	form = new Form(group.name);
    	
    	int imageIndex = group.imageIndex;
    	Image image = Icons.getInstance().getImageById(imageIndex, 0);
    	this.image = new ImageItem(null, image, ImageItem.LAYOUT_DEFAULT, null);
    	cmdChangeImage = new Command("Change", Command.ITEM, 1);
    	this.image.addCommand(cmdChangeImage);
    	this.image.setDefaultCommand(cmdChangeImage);
    	this.image.setItemCommandListener(this);
   		form.append(this.image);
	
    	TextField title = new TextField("Title", group.name, 255, TextField.ANY);
    	form.append(title);
    	
    	Date expire = group.getExpire();
    	if (expire != null) {
    		form.append(new StringItem("Expire", expire.toString()));
    	}

    	cmdOk = new Command("Apply", Command.SCREEN, 3);
    	cmdCancel = new Command("Cancel", Command.SCREEN, 2);
		form.addCommand(cmdOk);
		form.addCommand(cmdCancel);
    	form.setCommandListener(this);
    	
    	DisplayStack.push(form);
    	
		try {
			while (true) {
				this.event = EVENT_NONE;
				synchronized (this.form) {
					this.form.wait();
				}
				if (group.getDB().isLocked()) break;
				group.getDB().reassureWatchDog();
				
				if (this.event == EVENT_CLOSE) break;
				if (this.event == EVENT_APPLY) {
					group.name = title.getString();
					group.imageIndex = imageIndex;
					group.save();
					break;
				}
				
				switch(this.event) {
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
    	synchronized(this.form){
			this.form.notify();
		}
    }
    public void commandAction(Command cmd, Displayable dsp) {
    	if (cmd == cmdOk) {
    		fireEvent(EVENT_APPLY);
    	} else if (cmd == cmdCancel) {
    		fireEvent(EVENT_CLOSE);
    	}
    }
	public void commandAction(Command cmd, Item item) {
		if (cmd == cmdChangeImage) {
			fireEvent(EVENT_CHANGE_IMAGE);
		}
	}
}
