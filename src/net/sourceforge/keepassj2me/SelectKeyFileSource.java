package net.sourceforge.keepassj2me;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;
import javax.microedition.midlet.MIDlet;

public class SelectKeyFileSource extends List implements CommandListener {
	public static final int WAIT = -1;
	public static final int NONE = 0;
	public static final int FROM_FILE = 1;
	public static final int FROM_JAR = 2;
	private boolean from_file;
	private boolean from_jar;
	private int result = NONE;
	
	public SelectKeyFileSource(MIDlet midlet) {
		super("Use key file?", List.IMPLICIT);
		
		from_file = FileBrowser.isSupported();
		from_jar = JarBrowser.contentExists(KeePassMIDlet.jarKdbDir);
		
		if (from_file || from_jar) {
			this.append("no", Icons.getInstance().getImageById(Icons.ICON_EXIT));
			if (from_file) this.append("From file", Icons.getInstance().getImageById(Icons.ICON_OPEN_FROM_FILE));
			if (from_jar) this.append("From jar", Icons.getInstance().getImageById(Icons.ICON_OPEN_FROM_JAR));
			Command cmd = new Command("OK", Command.OK, 1);
			this.addCommand(cmd);
			this.setSelectCommand(cmd);
			this.setCommandListener(this);
			
			Displayable dspBACK = Display.getDisplay(midlet).getCurrent();
			Display.getDisplay(midlet).setCurrent(this);
			this.result = WAIT;
			try {
				while (this.result == WAIT) {
					synchronized (this) {
						this.wait();
					}
				}
			} catch (Exception e) {}
			Display.getDisplay(midlet).setCurrent(dspBACK);
		};
	}
	public void commandAction(Command cmd, Displayable dsp) {
		if (cmd.getCommandType() == Command.OK) {
			int index = this.getSelectedIndex();
			
			if (index == 0) {
				this.result = NONE;
			} else if (index == 1) {
				if (from_file) this.result = FROM_FILE;
				else this.result = FROM_JAR;
			} else if (index == 2) {
				this.result = FROM_JAR;
			};
			synchronized (this) {
				this.notify();
			};
		}
	}
	public int getResult() {
		return this.result;
	}
}
