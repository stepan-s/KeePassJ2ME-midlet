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

import java.util.Enumeration;
import java.util.Stack;

import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.ImageItem;
import javax.microedition.lcdui.StringItem;
import javax.microedition.midlet.MIDlet;

import net.sourceforge.keepassj2me.Config;
import net.sourceforge.keepassj2me.Icons;
import net.sourceforge.keepassj2me.KeePassMIDlet;
import net.sourceforge.keepassj2me.L10nKeys.keys;

/**
 * @author Stepan Strelets
 */
public class DisplayStack {
	static protected DisplayStack instance = null;
	private Stack stack = null;
	private MIDlet midlet = null;
	Form splash = null;
	
	/**
	 * Constructor
	 * @param midlet
	 */
	public DisplayStack(MIDlet midlet) {
		if (instance == null) {
			instance = this;
			this.stack = new Stack();
			this.midlet = midlet;
			
			splash = new Form(KeePassMIDlet.TITLE);
			splash.append(new ImageItem("",
								Icons.getInstance().getImageById(Icons.ICON_LOGO),
								ImageItem.LAYOUT_CENTER | ImageItem.LAYOUT_NEWLINE_AFTER,
								"", ImageItem.PLAIN));
			StringItem label = new StringItem(Config.getLocaleString(keys.PLEASE_WAIT), "");
			label.setLayout(StringItem.LAYOUT_CENTER);
			splash.append(label);
		};
	}
	/**
	 * Get instance
	 * @return instance
	 */
	public static DisplayStack getInstance() {
		return instance;
	}
	/**
	 * Push displayable in stack and display
	 * @param d
	 */
	public void push(Displayable d) {
		stack.push(d);
		Display.getDisplay(midlet).setCurrent(d);
	}
	/**
	 * Push splash in stack and display
	 */
	public void pushSplash() {
		push(splash);
	}
	/**
	 * Remove current displayable from stack and display previous
	 */
	public void pop() {
		stack.pop();
		Display.getDisplay(midlet).setCurrent((Displayable)stack.lastElement());
	}
	
	/**
	 * Redisplay last in stack
	 */
	public void showLast() {
		Display.getDisplay(midlet).setCurrent((Displayable)stack.lastElement());
	}
	/**
	 * Replace last displyable in stack with new and display
	 * @param d
	 */
	public void replaceLast(Displayable d) {
		stack.pop();
		stack.push(d);
		Display.getDisplay(midlet).setCurrent(d);
	}
	/**
	 * Replace last displayable with splash and display
	 */
	public void replaceLastWithSplash() {
		replaceLast(splash);
	}
	/**
	 * Notify stack diplayables
	 */
	public static void notifyUI() {
		if (instance != null) {
			for (Enumeration e = instance.stack.elements(); e.hasMoreElements();) {
				Displayable d = (Displayable)(e.nextElement());
				synchronized (d) {
					d.notify();
				}
			};
		};
	}
}
