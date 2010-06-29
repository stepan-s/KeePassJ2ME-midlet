package net.sourceforge.keepassj2me.datasource;

import java.io.IOException;
import java.io.InputStream;

import net.sourceforge.keepassj2me.Icons;
import net.sourceforge.keepassj2me.KeePassException;
import net.sourceforge.keepassj2me.tools.FileBrowser;
import net.sourceforge.keepassj2me.tools.JarBrowser;

/**
 * JAR adapter
 * @author Stepan Strelets
 */
public class DataSourceAdapterJar extends DataSourceAdapter {
	private static final String jarKdbDir = "/kdb";
	private String url;
	
	/**
	 * You dont need create adapters directly, use registry
	 */
	public DataSourceAdapterJar() {
		super(DataSourceRegistry.JAR, "Midlet", 36);
	}

	public boolean selectLoad(String caption) throws KeePassException {
		try {
			JarBrowser jb = new JarBrowser("Select " + caption, Icons.getInstance().getImageById(Icons.ICON_FILE));
			jb.setDir(DataSourceAdapterJar.jarKdbDir);
			jb.display();
			String jarUrl = jb.getUrl();
			if (jarUrl != null) {
				this.url = jarUrl;
				return true;
			} else {
				return false;
			}
		} catch (KeePassException e) {
			throw new KeePassException(e.getMessage());
		}
	}

	public InputStream getInputStream() throws KeePassException {
		if (this.url == null)
			throw new KeePassException("URL not specified");
		
		return getClass().getResourceAsStream(this.url);
	}
	
	public byte[] load() throws KeePassException {
		try {
			if (this.url == null)
				throw new KeePassException("URL not specified");
			
			InputStream is = getClass().getResourceAsStream(this.url);
			if (is == null) {
				// #ifdef DEBUG
					System.out
						.println("InputStream is null ... file probably not found");
				// #endif
				throw new KeePassException(
						"Resource '"+this.url+"' is not found or not readable");
			}
			byte buf[] = new byte[is.available()];
			is.read(buf);
			return buf;
			
		} catch (IOException e) {
			throw new KeePassException(e.getMessage());
		}
	}

	public void save(byte[] content) throws KeePassException {
		throw new KeePassException("JAR source is readonly");
	}

	public boolean canLoad() {
		return JarBrowser.contentExists(DataSourceAdapterJar.jarKdbDir);
	}
	
	public void serialize(SerializeStream out) throws IOException {
		out.write(uid);
		out.writeUTF(this.url);
	}
	
	public void unserialize(UnserializeStream in) throws IOException {
		this.url = in.readUTF();
	}
	
	public String getCaption() {
		return "jar:"+getName();
	}
	public String getName() {
		return url.substring(url.lastIndexOf(FileBrowser.SEP_CHAR)+1);
	}
}
