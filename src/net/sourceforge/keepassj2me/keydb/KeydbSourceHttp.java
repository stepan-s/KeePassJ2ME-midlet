package net.sourceforge.keepassj2me.keydb;

import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.midlet.MIDlet;

import net.sourceforge.keepassj2me.HTTPConnectionThread;
import net.sourceforge.keepassj2me.KeePassMIDlet;

/**
 * @author Stepan Strelets
 */
public class KeydbSourceHttp extends KeydbSource {
	private String url;
	private String usercode;
	private String passcode;
	private String enccode;
	private MIDlet midlet;
	
	public KeydbSourceHttp(String url, String usercode, String passcode, String enccode, MIDlet midlet) {
		this.url = url;
		this.usercode = usercode;
		this.passcode = passcode;
		this.enccode = enccode;
		this.midlet = midlet;
	}

	public byte[] load_kdb() throws KeydbException {
		// now download kdb from web server
		Form waitForm = new Form(KeePassMIDlet.TITLE);
		waitForm.append("Downloading ...\n");
		Displayable back = Display.getDisplay(midlet).getCurrent(); 
		Display.getDisplay(midlet).setCurrent(waitForm);
		
		HTTPConnectionThread t = new HTTPConnectionThread(
				this.url,
				this.usercode,
				this.passcode,
				this.enccode,
				this.midlet,
				waitForm);
		t.start();

		try {
			t.join();
		} catch (InterruptedException e) {
			// #ifdef DEBUG
				System.out.println(e.toString());
			// #endif
		}
		Display.getDisplay(midlet).setCurrent(back);
		return t.getContent();
	}

	public void save_kdb(byte[] content) throws KeydbException {
		throw new KeydbException("HTTP source is readonly");
	}

}
