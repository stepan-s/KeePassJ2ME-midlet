package net.sourceforge.keepassj2me.tools;

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
	public static void push(Displayable d) {
		instance.stack.push(d);
		Display.getDisplay(instance.midlet).setCurrent(d);
	}
	public static void pushSplash() {
		instance.stack.push(instance.splash);
		Display.getDisplay(instance.midlet).setCurrent(instance.splash);
	}
	public static void pop() {
		instance.stack.pop();
		Display.getDisplay(instance.midlet).setCurrent((Displayable)instance.stack.lastElement());
	}
	
	public static void showLast() {
		Display.getDisplay(instance.midlet).setCurrent((Displayable)instance.stack.lastElement());
	}
	public static void replaceLast(Displayable d) {
		instance.stack.pop();
		instance.stack.push(d);
		Display.getDisplay(instance.midlet).setCurrent(d);
	}
	public static void replaceLastWithSplash() {
		replaceLast(instance.splash);
	}
}
