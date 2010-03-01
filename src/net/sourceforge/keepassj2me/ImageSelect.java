package net.sourceforge.keepassj2me;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;

import net.sourceforge.keepassj2me.tools.DisplayStack;

public class ImageSelect implements CommandListener {
	private List list;
	private int result = -1;
	
	public ImageSelect() {
		Icons icons = Icons.getInstance();
		list = new List("", List.IMPLICIT);
		for(int i = 0; i < Icons.NUM_ICONS; ++i) {
			list.append(Integer.toString(i), icons.getImageById(i));
		};

		list.addCommand(new Command("Cancel", Command.CANCEL, 1));
		Command cmd_ok = new Command("OK", Command.OK, 1);
		list.addCommand(cmd_ok);
		list.setSelectCommand(cmd_ok);
		list.setCommandListener(this);
	}
	public void commandAction(Command cmd, Displayable dsp) {
		switch(cmd.getCommandType()) {
		case Command.OK:
			result = 1;
			break;
			
		case Command.CANCEL:
			result = 0;
			break;
			
		default:
			return;
		}

		synchronized (this) {
			this.notify();
		}
	}
	public boolean select(int index) {
		list.setSelectedIndex(index, true);
		DisplayStack.push(list);
		try {
			while (result == -1) {
				synchronized (this) {
					this.wait();
				}
			}
		} catch (Exception e) {}
		DisplayStack.pop();
		return (result == 1);
	}
	public int getSelectedImageIndex() {
		return list.getSelectedIndex();
	}
}
