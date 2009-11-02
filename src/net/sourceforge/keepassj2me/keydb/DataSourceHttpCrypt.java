package net.sourceforge.keepassj2me.keydb;

import java.io.InputStream;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.midlet.MIDlet;

import net.sourceforge.keepassj2me.Config;
import net.sourceforge.keepassj2me.HTTPConnectionThread;
import net.sourceforge.keepassj2me.KeePassMIDlet;
import net.sourceforge.keepassj2me.URLCodeBox;

/**
 * @author Stepan Strelets
 */
public class DataSourceHttpCrypt extends DataSource {
	private String url;
	private String usercode;
	private String passcode;
	private String enccode;
	private MIDlet midlet;
	
/*	public DataSourceHttpCrypt(String url, String usercode, String passcode, String enccode, MIDlet midlet) {
		this.url = url;
		this.usercode = usercode;
		this.passcode = passcode;
		this.enccode = enccode;
		this.midlet = midlet;
	}*/

	public void select(MIDlet midlet, String caption) throws KeydbException {
		//FIXME:
		URLCodeBox box = new URLCodeBox("Download " + caption, this.midlet);
		box.setURL(Config.getInstance().getDownloadUrl());
		box.display();
		if (box.getCommandType() != Command.CANCEL) {
			Config.getInstance().setDownloadUrl(box.getURL());
			
			this.url = box.getURL();
			this.usercode = box.getUserCode();
			this.passcode = box.getPassCode();
			this.enccode = box.getEncCode();
			this.midlet = midlet;
		};
	}

	public InputStream getInputStream() throws KeydbException {
		throw new KeydbException("Not implemented");
	}
	
	public byte[] load() throws KeydbException {
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

	public void save(byte[] content) throws KeydbException {
		throw new KeydbException("HTTP source is readonly");
	}

	public static boolean canSave() {
		return true;
	}
}
