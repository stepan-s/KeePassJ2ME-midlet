package net.sourceforge.keepassj2me.keydb;

import java.io.IOException;
import java.io.InputStream;

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import javax.microedition.midlet.MIDlet;

import net.sourceforge.keepassj2me.Config;
import net.sourceforge.keepassj2me.FileBrowser;
import net.sourceforge.keepassj2me.Icons;

/**
 * @author Stepan Strelets
 */
public class DataSourceFile extends DataSource {
	private String url;
	
	public DataSourceFile() {
	}
	
	public void select(MIDlet midlet, String caption) throws KeydbException {
		FileBrowser fileBrowser = new FileBrowser(midlet, "Select " + caption, Icons.getInstance().getImageById(Icons.ICON_DIR), Icons.getInstance().getImageById(Icons.ICON_FILE), Icons.getInstance().getImageById(Icons.ICON_BACK));
		fileBrowser.setDir(Config.getInstance().getLastDir());
		fileBrowser.display();
		String url = fileBrowser.getUrl();
		if (url != null) {
			Config.getInstance().setLastDir(url);
			this.url = url;
		};
	}

	public InputStream getInputStream() throws KeydbException {
		FileConnection conn = null;
		try {
			if (this.url == null)
				throw new KeydbException("URL not specified");
			
			conn = (FileConnection) Connector.open(this.url, Connector.READ);
			if (!conn.exists()) {
				throw new KeydbException("File does not exist: " + this.url);
			};
			return conn.openInputStream();
			
		} catch (IOException e) {
			throw new KeydbException("File access error");
		}
	}

	public byte[] load() throws KeydbException {
		try {
			if (this.url == null)
				throw new KeydbException("URL not specified");
			
			// open the file
			FileConnection conn = (FileConnection) Connector.open(this.url, Connector.READ);
			if (!conn.exists()) {
				// #ifdef DEBUG
				System.out.println("File doesn't exist");
				// #endif
				throw new KeydbException("File does not exist: "
						+ conn.getPath() + conn.getName());
			}
			InputStream is = conn.openInputStream();
			
			// TODO what if the file is too big for a single array?
			byte buf[] = new byte[(int) conn.fileSize()];
			// TODO this read is blocking and may not read the whole file
			// #ifdef DEBUG
			int read =
			// #endif
				is.read(buf);
			conn.close();
			// #ifdef DEBUG
			System.out.println("Storing " + read + " bytes into buf.");
			// #endif
			return buf;
			
		} catch (IOException e) {
			throw new KeydbException(e.getMessage());
		}
	}

	public void save(byte[] content) {
		// TODO Auto-generated method stub

	}

	public static boolean canLoad() {
		return FileBrowser.isSupported();
	}
	public static boolean canSave() {
		return FileBrowser.isSupported();
	}
}
