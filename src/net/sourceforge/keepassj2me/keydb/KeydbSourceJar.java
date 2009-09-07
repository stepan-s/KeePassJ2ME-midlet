package net.sourceforge.keepassj2me.keydb;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Stepan Strelets
 */
public class KeydbSourceJar extends KeydbSource {
	private String url;
	
	public KeydbSourceJar(String url) {
		this.url = url;
	}

	public byte[] load_kdb() throws KeydbException {
		try {
			InputStream is = getClass().getResourceAsStream(this.url);
			if (is == null) {
				// #ifdef DEBUG
					System.out
						.println("InputStream is null ... file probably not found");
				// #endif
				throw new KeydbException(
						"Resource '"+this.url+"' is not found or not readable");
			}
			byte buf[] = new byte[is.available()];
			is.read(buf);
			return buf;
			
		} catch (IOException e) {
			throw new KeydbException(e.getMessage());
		}
	}

	public void save_kdb(byte[] content) throws KeydbException {
		throw new KeydbException("JAR source is readonly");
	}

}
