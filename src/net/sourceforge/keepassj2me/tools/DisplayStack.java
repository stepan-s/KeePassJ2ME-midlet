package net.sourceforge.keepassj2me.tools;

import java.util.Enumeration;
import java.util.Stack;

import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.ImageItem;
import javax.microedition.lcdui.StringItem;
import javax.microedition.midlet.MIDlet;

import net.sourceforge.keepassj2me.Icons;
import net.sourceforge.keepassj2me.KeePassMIDlet;

/**
 * @author Stepan Strelets
 */
public class DisplayStack {
	static protected DisplayStack instance = null;
	private Stack stack = null;
	private MIDlet midlet = null;
	Form splash = null;
	
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
			StringItem label = new StringItem("Please wait", "");
			label.setLayout(StringItem.LAYOUT_CENTER);
			splash.append(label);
		};
	}
	public static DisplayStack getInstance() {
		return instance;
	}
	public void push(Displayable d) {
		stack.push(d);
		Display.getDisplay(midlet).setCurrent(d);
	}
	public void pushSplash() {
		push(splash);
	}
	public void pop() {
		stack.pop();
		Display.getDisplay(midlet).setCurrent((Displayable)stack.lastElement());
	}
	
	public void showLast() {
		Display.getDisplay(midlet).setCurrent((Displayable)stack.lastElement());
	}
	public void replaceLast(Displayable d) {
		stack.pop();
		stack.push(d);
		Display.getDisplay(midlet).setCurrent(d);
	}
	public void replaceLastWithSplash() {
		replaceLast(splash);
	}
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
