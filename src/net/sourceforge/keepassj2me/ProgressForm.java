package net.sourceforge.keepassj2me;

import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Gauge;
import javax.microedition.lcdui.Item;

/**
 * Form for display current progress a process 
 * @author Stepan Strelets
 */
public class ProgressForm extends Form implements IProgressListener {
	private Gauge bar = null;
	
	/**
	 * Construct form
	 * @param title Form title
	 */
	public ProgressForm(String title) {
		super(title);
		bar = new Gauge("", false, 100, 0);
		bar.setLayout(Item.LAYOUT_CENTER | Item.LAYOUT_VCENTER | Item.LAYOUT_EXPAND);
		this.append(bar);
	}
	
	public void setProgress(int procent, String message) {
		bar.setValue(procent);
		if (message != null) bar.setLabel(message);
	}
}
