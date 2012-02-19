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

package net.sourceforge.keepassj2me.tools;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Gauge;
import javax.microedition.lcdui.Item;

import net.sourceforge.keepassj2me.Config;
import net.sourceforge.keepassj2me.KeePassException;
import net.sourceforge.keepassj2me.KeePassMIDlet;
import net.sourceforge.keepassj2me.L10nKeys.keys;

/**
 * Form for display current progress a process 
 * @author Stepan Strelets
 */
public class ProgressForm extends Form implements IProgressListener, CommandListener {
	private Gauge bar = null;
	private Command cmdCancel = null;
	private boolean cancel = false;
	
	/**
	 * Construct form
	 * @param cancelable
	 */
	public ProgressForm(boolean cancelable) {
		super(KeePassMIDlet.TITLE);
		bar = new Gauge("", false, 100, 0);
		bar.setLayout(Item.LAYOUT_CENTER | Item.LAYOUT_VCENTER | Item.LAYOUT_EXPAND);
		if (cancelable) {
			this.cmdCancel = new Command(Config.getLocaleString(keys.CANCEL), Command.CANCEL, 1);
			this.addCommand(this.cmdCancel);
			this.setCommandListener(this);
		}
		this.append(bar);
	}
	
	public void setProgress(int procent, String message) throws KeePassException {
		if (this.cancel) {
			throw new KeePassException(Config.getLocaleString(keys.CANCEL_BY_USER));			
		} else {
			bar.setValue(procent);
			if (message != null) bar.setLabel(message);
		};
	}

	public void commandAction(Command cmd, Displayable dsp) {
		if (cmd == this.cmdCancel) {
			this.cancel = true;
			this.bar.setLabel(Config.getLocaleString(keys.CANCELING));
		}
	}
}
