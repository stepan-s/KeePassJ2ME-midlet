package net.sourceforge.keepassj2me.datasource;

import java.io.IOException;
import java.io.InputStream;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Form;

import net.sourceforge.keepassj2me.Config;
import net.sourceforge.keepassj2me.KeePassMIDlet;
import net.sourceforge.keepassj2me.keydb.KeydbException;
import net.sourceforge.keepassj2me.tools.DisplayStack;

/**
 * @author Stepan Strelets
 */
public class DataSourceAdapterHttpCrypt extends DataSourceAdapter {
	private String url;
	private String usercode;
	private String passcode;
	private String enccode;
	
	public DataSourceAdapterHttpCrypt() {
		super(DataSourceRegistry.HTTPC, "KeepassServer", 1);
	}

	public boolean selectLoad(String caption) throws KeydbException {
		//FIXME:
		URLCodeBox box = new URLCodeBox("Download " + caption);
		box.setURL(Config.getInstance().getDownloadUrl());
		box.displayAndWait();
		if (box.getCommandType() != Command.CANCEL) {
			Config.getInstance().setDownloadUrl(box.getURL());
			
			this.url = box.getURL();
			this.usercode = box.getUserCode();
			this.passcode = box.getPassCode();
			this.enccode = box.getEncCode();
			return true;
		} else {
			return false;
		}
	}

	public InputStream getInputStream() throws KeydbException {
		throw new KeydbException("Not implemented");
	}
	
	public byte[] load() throws KeydbException {
		// now download kdb from web server
		Form waitForm = new Form(KeePassMIDlet.TITLE);
		waitForm.append("Downloading ...\n");
		DisplayStack.push(waitForm);
		
		HTTPConnectionThread t = new HTTPConnectionThread(
				this.url,
				this.usercode,
				this.passcode,
				this.enccode,
				waitForm);
		t.start();

		try {
			t.join();
		} catch (InterruptedException e) {
			// #ifdef DEBUG
				System.out.println(e.toString());
			// #endif
		}
		DisplayStack.pop();
		return t.getContent();
	}

	public void save(byte[] content) throws KeydbException {
		throw new KeydbException("HTTP source is readonly");
	}

	public boolean canSave() {
		return true;
	}
	
	public void serialize(SerializeStream out) throws IOException {
		out.write(uid);
		out.writeUTF(this.url);
	}
	
	public void unserialize(UnserializeStream in) throws IOException {
		this.url = in.readUTF();
	}
}
