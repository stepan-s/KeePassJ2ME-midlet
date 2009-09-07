package net.sourceforge.keepassj2me.keydb;

import java.io.IOException;
import java.io.InputStream;

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;

/**
 * @author Stepan Strelets
 */
public class KeydbSourceFile extends KeydbSource {
	private String url;
	
	public KeydbSourceFile(String url) {
		this.url = url;
		this.readonly = false;
	}
	public byte[] load_kdb() throws KeydbException {
		try {
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

	public void save_kdb(byte[] content) {
		// TODO Auto-generated method stub

	}

}
