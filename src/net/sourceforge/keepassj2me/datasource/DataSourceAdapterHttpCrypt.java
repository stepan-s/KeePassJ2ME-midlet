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

package net.sourceforge.keepassj2me.datasource;

import java.io.IOException;
import java.io.InputStream;

import javax.microedition.lcdui.Command;

import net.sourceforge.keepassj2me.Config;
import net.sourceforge.keepassj2me.KeePassException;
import net.sourceforge.keepassj2me.tools.DisplayStack;
import net.sourceforge.keepassj2me.tools.FileBrowser;
import net.sourceforge.keepassj2me.tools.ProgressForm;

/**
 * Http adapter (old)
 * @author Stepan Strelets
 */
public class DataSourceAdapterHttpCrypt extends DataSourceAdapter {
	private String url;
	private String usercode;
	private String passcode;
	private String enccode;
	
	/**
	 * You dont need create adapters directly, use registry
	 */
	public DataSourceAdapterHttpCrypt() {
		super(DataSourceRegistry.HTTPC, "KeepassServer", 1);
	}

	public boolean selectLoad(String caption) throws KeePassException {
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

	public InputStream getInputStream() throws KeePassException {
		throw new KeePassException("Not implemented");
	}
	
	public byte[] load() throws KeePassException {
		// now download kdb from web server
		ProgressForm waitForm = new ProgressForm(false);
		try {
			waitForm.setProgress(0, "Downloading");
		} catch (KeePassException e1) {
		}
		DisplayStack.getInstance().push(waitForm);
		
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
		DisplayStack.getInstance().pop();
		return t.getContent();
	}

	public void save(byte[] content) throws KeePassException {
		throw new KeePassException("HTTP source is readonly");
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
	
	public String getCaption() {
		return "http.c:"+getName();
	}
	public String getName() {
		return url.substring(url.lastIndexOf(FileBrowser.SEP_CHAR)+1);
	}
}
