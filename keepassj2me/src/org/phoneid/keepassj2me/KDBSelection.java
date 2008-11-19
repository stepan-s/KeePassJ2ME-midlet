package org.phoneid.keepassj2me;

import java.io.IOException;
// PhoneID
import org.phoneid.*;
import javax.microedition.lcdui.*;

public class KDBSelection implements CommandListener {
	protected KeePassMIDlet midlet;
	private boolean isReady = false;
	private List list;
	private Displayable dspBACK;
	private int result = 0;

	public KDBSelection(KeePassMIDlet midlet) {
		list = new List("Load KDB from:", Choice.IMPLICIT);
		
		list.append("Internet", midlet.getImageById(1));
		list.append("Midlet", midlet.getImageById(36));
	
		// check whether the FileConnection API (part of JSR75) is available
		String fileConnVersion = System
				.getProperty("microedition.io.file.FileConnection.version");
		if (fileConnVersion != null) {
			System.out.println("Got FileConnection version " + fileConnVersion);
			list.append("File", midlet.getImageById(48));
			list.append("File in [other]", midlet.getImageById(48));
		};
		
		list.setCommandListener(this);
		list.addCommand(new Command("OK", Command.OK, 1));
		list.addCommand(new Command("Exit", Command.EXIT, 1));
		list.setSelectCommand(new Command("OK", Command.OK, 1));
		list.setCommandListener(this);
		this.midlet = midlet;

		// list.addCommand(midlet.CMD_EXIT);

		// Previous Display
		dspBACK = Display.getDisplay(midlet).getCurrent();

		// Display message
		System.out.println("Display KDBSelection list");
		Display.getDisplay(midlet).setCurrent(list);
		System.out.println("Displayed KDBSelection list");
	}

	public void commandAction(Command cmd, Displayable dsp) {
		System.out.println("KDBSelection commandAction()");

		if (cmd.getCommandType() == Command.OK) {
			result = list.getSelectedIndex();

			// if web sync is disabled, don't let web sync selected
			if (Definition.CONFIG_NO_WEB == true && result == 0)
				return;

			isReady = true;
			synchronized (this) {
				this.notify();
			}
			// Display.getDisplay(midlet).setCurrent(dspBACK);
		} else if (cmd.getCommandType() == Command.EXIT) {
			midlet.destroyApp(false);
			midlet.notifyDestroyed();
		}
	}

	public int getResult() {
		return result;
	}

	public void waitForDone() {
		try {
			while (!isReady) {
				synchronized (this) {
					this.wait();
				}
			}
		} catch (Exception e) {
			System.out.println(e.toString());
		}
	}
}
