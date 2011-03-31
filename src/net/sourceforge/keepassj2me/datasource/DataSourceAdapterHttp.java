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
import java.util.Vector;

import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;

import net.sourceforge.keepassj2me.Config;
import net.sourceforge.keepassj2me.KeePassException;
import net.sourceforge.keepassj2me.L10nKeys.keys;
import net.sourceforge.keepassj2me.keydb.KeydbUtil;
import net.sourceforge.keepassj2me.tools.FileBrowser;

/**
 * Http adapter
 * @author Stepan Strelets
 */
public class DataSourceAdapterHttp extends DataSourceAdapter {
	private String url;
	private String name;
	private String user;
	private String pass;
	private Vector list;

	/**
	 * You dont need create adapters directly, use registry
	 */
	public DataSourceAdapterHttp() {
		super(DataSourceRegistry.HTTP, Config.getLocaleString(keys.DS_HTTP), 1);
	}
	
	public boolean selectLoad(String caption) throws KeePassException {
		this.getList();
		return false;
	}

	public boolean selectSave(String caption, String defaultName) throws KeePassException {
		return false;
	}
	
	public InputStream getInputStream() throws KeePassException {
		// TODO Auto-generated method stub
		return null;
	}

	public byte[] load() throws KeePassException {
		// TODO Auto-generated method stub
		return null;
	}

	public void save(byte[] content) throws KeePassException {
		// TODO Auto-generated method stub

	}

	public boolean canSave() {
		return true;
	}
	
	public void serialize(SerializeStream out) throws IOException {
		out.write(uid);
		out.writeUTF(this.url);
		out.writeUTF(this.name);
		out.writeUTF(this.user);
		out.writeUTF(this.pass);
	}

	public void unserialize(UnserializeStream in) throws IOException {
		this.url = in.readUTF();
		this.name = in.readUTF();
		this.user = in.readUTF();
		this.pass = in.readUTF();
	}

	public String getCaption() {
		return "http:"+getName();
	}
	
	public String getName() {
		return url.substring(url.lastIndexOf(FileBrowser.SEP_CHAR)+1);
	}
	
	private Vector getList() throws KeePassException {
		if (list == null) {
			try {
				String raw = new String(this.httpDo("list"), "UTF-8");
				// #ifdef DEBUG
				System.out.println(raw);
				// #endif
				Vector result = new Vector();
				
				String name;
				do {
					int i = raw.indexOf(0x0A);
					if (i > 0) {
						name = raw.substring(0, i).trim();
						raw = raw.substring(i + 1);
					} else {
						name = raw.trim();
						raw = "";
					};
					if (name.length() > 0) {
						result.addElement(name);
					};
				} while (raw.length() > 0);
				list = result;
				
			} catch(Exception e) {
				throw new KeePassException(e.getMessage());
			}
		};
		return list;
	}
	private byte[] httpDo(String resource) throws IOException {
		byte[] result = null;
		
		HttpConnection con = (HttpConnection)Connector.open(url+"/"+this.user+"/"+resource);
		con.setRequestMethod(HttpConnection.GET);
		con.setRequestProperty("x-auth-custom", KeydbUtil.hashToString(KeydbUtil.hash(this.pass)));
		if (con.getResponseCode() == 200) {
			
		} else {
			throw new IOException(con.getResponseMessage());
		}
		
		long size = con.getLength();
		result = new byte[(int)size];
		InputStream in = con.openInputStream();
		in.read(result);
		
		return result;
	}
}
