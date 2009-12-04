package net.sourceforge.keepassj2me.datasource;

import java.io.IOException;
import java.io.InputStream;

import net.sourceforge.keepassj2me.Icons;
import net.sourceforge.keepassj2me.KeePassException;
import net.sourceforge.keepassj2me.KeePassMIDlet;
import net.sourceforge.keepassj2me.keydb.KeydbException;
import net.sourceforge.keepassj2me.tools.JarBrowser;

/**
 * @author Stepan Strelets
 */
public class DataSourceAdapterJar extends DataSourceAdapter {
	private String url;
	
	public DataSourceAdapterJar() {
		super(DataSourceRegistry.JAR, "Midlet", 36);
	}

	public void select(String caption) throws KeydbException {
		try {
			JarBrowser jb = new JarBrowser("Select " + caption, Icons.getInstance().getImageById(Icons.ICON_FILE));
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

	public boolean canLoad() {
		return JarBrowser.contentExists(KeePassMIDlet.jarKdbDir);
	}
	
	public void serialize(SerializeStream out) throws IOException {
		out.write(uid);
		out.writeUTF(this.url);
	}
	
	public void unserialize(UnserializeStream in) throws IOException {
		this.url = in.readUTF();
	}
}
