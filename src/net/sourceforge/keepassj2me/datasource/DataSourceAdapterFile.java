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
import java.io.OutputStream;

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;

import net.sourceforge.keepassj2me.Config;
import net.sourceforge.keepassj2me.KeePassException;
import net.sourceforge.keepassj2me.L10nKeys.keys;
import net.sourceforge.keepassj2me.tools.FileBrowser;

/**
 * File system adapter
 * @author Stepan Strelets
 */
public class DataSourceAdapterFile extends DataSourceAdapter {
	private String url;
	
	/**
	 * You dont need create adapters directly, use registry
	 */
	public DataSourceAdapterFile() {
		super(DataSourceRegistry.FILE, Config.getLocaleString(keys.DS_FILE), 48);
	}
	
	public boolean selectLoad(String caption) throws KeePassException {
		String url = FileBrowser.open(Config.getLocaleString(keys.DS_SELECT, new String[] {caption}), null);
		if (url != null) {
			this.url = url;
			return true;
		} else {
			return false;
		}
	}
	public boolean selectSave(String caption, String defaultName) throws KeePassException {
		String url = FileBrowser.save(Config.getLocaleString(keys.DS_SELECT, new String[] {caption}), null, defaultName);
		if (url != null) {
			this.url = url;
			return true;
		} else {
			return false;
		}
	}

	public InputStream getInputStream() throws KeePassException {
		FileConnection conn = null;
		try {
			if (this.url == null)
				throw new KeePassException(Config.getLocaleString(keys.DS_URL_NOT_SPECIFIED));
			
			conn = (FileConnection) Connector.open(this.url, Connector.READ);
			if (!conn.exists()) {
				throw new KeePassException(Config.getLocaleString(keys.DS_FILE_NOT_EXIST, new String[] {this.url}));
			};
			return new FileInputStream(conn);
			
		} catch (IOException e) {
			throw new KeePassException(Config.getLocaleString(keys.DS_FILE_ACCESS_ERROR));
		}
	}

	public byte[] load() throws KeePassException {
		try {
			if (this.url == null)
				throw new KeePassException(Config.getLocaleString(keys.DS_URL_NOT_SPECIFIED));
			
			// open the file
			FileConnection conn = (FileConnection) Connector.open(this.url, Connector.READ);
			if (!conn.exists()) {
				// #ifdef DEBUG
				System.out.println("File doesn't exist");
				// #endif
				throw new KeePassException(Config.getLocaleString(keys.DS_FILE_NOT_EXIST, new String[] {conn.getPath() + conn.getName()}));
			}
			InputStream is = conn.openInputStream();
			
			// TODO what if the file is too big for a single array?
			byte buf[] = new byte[(int) conn.fileSize()];
			// TODO this read is blocking and may not read the whole file
			// #ifdef DEBUG
			int read =
			// #endif
				is.read(buf);
			is.close();
			conn.close();
			// #ifdef DEBUG
			System.out.println("Storing " + read + " bytes into buf.");
			// #endif
			return buf;
			
		} catch (IOException e) {
			throw new KeePassException(e.getMessage());
		}
	}

	public void save(byte[] content) throws KeePassException {
		try {
			if (this.url == null)
				throw new KeePassException(Config.getLocaleString(keys.DS_URL_NOT_SPECIFIED));
			
			FileConnection conn = (FileConnection) Connector.open(this.url, Connector.READ_WRITE);
			if (!conn.exists()) conn.create();
			OutputStream os = conn.openOutputStream();
			os.write(content);
			os.close();
			conn.truncate(content.length);
			conn.close();
			
		} catch (IOException e) {
			throw new KeePassException(e.getMessage());
		}
	}

	public boolean canLoad() {
		return FileBrowser.isSupported();
	}
	
	public boolean canSave() {
		return FileBrowser.isSupported();
	}

	public void serialize(SerializeStream out) throws IOException {
		out.write(uid);
		out.writeUTF(this.url);
	}
	
	public void unserialize(UnserializeStream in) throws IOException {
		this.url = in.readUTF();
	}
	
	public String getCaption() {
		return "fs:"+getName();
	}
	public String getName() {
		return url.substring(url.lastIndexOf(FileBrowser.SEP_CHAR)+1);
	}
}
