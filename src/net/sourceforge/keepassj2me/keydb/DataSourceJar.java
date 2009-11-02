package net.sourceforge.keepassj2me.keydb;

import java.io.IOException;
import java.io.InputStream;

import javax.microedition.midlet.MIDlet;

import net.sourceforge.keepassj2me.Icons;
import net.sourceforge.keepassj2me.JarBrowser;
import net.sourceforge.keepassj2me.KeePassException;
import net.sourceforge.keepassj2me.KeePassMIDlet;

/**
 * @author Stepan Strelets
 */
public class DataSourceJar extends DataSource {
	private String url;
	
	public DataSourceJar() {
	}

	public void select(MIDlet midlet, String caption) throws KeydbException {
		try {
			JarBrowser jb = new JarBrowser(midlet, "Select " + caption, Icons.getInstance().getImageById(Icons.ICON_FILE));
			jb.setDir(KeePassMIDlet.jarKdbDir);
			jb.display();
			String jarUrl = jb.getUrl();
			if (jarUrl != null) {
				this.url = jarUrl;
			};
		} catch (KeePassException e) {
			throw new KeydbException(e.getMessage());
		}
	}

	public InputStream getInputStream() throws KeydbException {
		if (this.url == null)
			throw new KeydbException("URL not specified");
		
		return getClass().getResourceAsStream(this.url);
	}
	
	public byte[] load() throws KeydbException {
		try {
			if (this.url == null)
				throw new KeydbException("URL not specified");
			
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

	public void save(byte[] content) throws KeydbException {
		throw new KeydbException("JAR source is readonly");
	}

	public static boolean canLoad() {
		return JarBrowser.contentExists(KeePassMIDlet.jarKdbDir);
	}
}
