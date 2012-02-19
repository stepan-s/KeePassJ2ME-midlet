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

import net.sourceforge.keepassj2me.L10nKeys.keys;
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
     * @param group 
     */
    public KeydbGroupEdit(KeydbGroup group) {
    	form = new Form(group.name);
    	L10n lc = Config.getInstance().getLocale();
    	
    	int imageIndex = group.imageIndex;
    	Image image = Icons.getInstance().getImageById(imageIndex, 0);
    	this.image = new ImageItem(null, image, ImageItem.LAYOUT_DEFAULT, null);
    	cmdChangeImage = new Command(lc.getString(keys.CHANGE), Command.ITEM, 1);
    	this.image.addCommand(cmdChangeImage);
    	this.image.setDefaultCommand(cmdChangeImage);
    	this.image.setItemCommandListener(this);
   		form.append(this.image);
	
    	TextField title = new TextField(lc.getString(keys.TITLE), group.name, 255, TextField.ANY);
    	form.append(title);
    	
    	Date expire = group.getExpire();
    	if (expire != null) {
    		form.append(new StringItem(lc.getString(keys.EXPIRE), expire.toString()));
    	}

    	cmdOk = new Command(lc.getString(keys.APPLY), Command.OK, 3);
    	cmdCancel = new Command(lc.getString(keys.CANCEL), Command.CANCEL, 2);
		form.addCommand(cmdOk);
		form.addCommand(cmdCancel);
    	form.setCommandListener(this);
    	
    	DisplayStack.getInstance().push(form);
    	
		try {
			while (true) {
				if (group.getDB().isLocked()) break;
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
    	}
    }
	public void commandAction(Command cmd, Item item) {
		if (cmd == cmdChangeImage) {
			fireEvent(EVENT_CHANGE_IMAGE);
		}
	}
}
