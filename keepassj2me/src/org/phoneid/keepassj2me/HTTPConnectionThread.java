package org.phoneid.keepassj2me;

// PhoneID
import org.phoneid.*;

// Java
import javax.microedition.io.*;
import javax.microedition.lcdui.*;
import javax.microedition.midlet.*;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.*;
import java.io.UnsupportedEncodingException;

/// record store
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;
import javax.microedition.rms.RecordEnumeration;

public class HTTPConnectionThread
    extends Thread
{
    String mSecretCode = null;
    KeePassMIDlet mMidlet;

    public HTTPConnectionThread(String secretCode, KeePassMIDlet midlet) {
	mSecretCode = secretCode;
	mMidlet = midlet;
    }
    
    public void run() {
	try {
	    connect(mSecretCode);
	} catch (Exception e) {
	    mMidlet.doAlert(e.toString());
	}
    }

    private void connect(String secretCode)
	throws IOException, RecordStoreException, PhoneIDException
    {
	HttpConnection hc = null;
	InputStream in = null;
	String url = "http://www.keepassserver.info/download.php";
	String rawData = "code=" + secretCode;
	String type = "application/x-www-form-urlencoded";
	
	hc = (HttpConnection)Connector.open(url);
	
	hc.setRequestMethod(HttpConnection.POST);
	hc.setRequestProperty("Content-Type", type);
	hc.setRequestProperty("Content-Length", "13");
	OutputStream os = hc.openOutputStream();
	os.write(rawData.getBytes());
	
	int rc = hc.getResponseCode();
	System.out.println ("rc = " + rc);
	
	if (rc != HttpConnection.HTTP_OK) {
	    throw new IOException("HTTP response code: " + rc);
	}
	
	in = hc.openInputStream();
	
	int contentLength = (int)hc.getLength();
	byte[] content = new byte[contentLength];
	int length = in.read(content);
	
	in.close();
	hc.close();
	
	// Show the response to the user.
	System.out.println ("Downloaded " + contentLength + " bytes");
	//String s = new String(raw, 0, length);
	//System.out.println (s);

	if (contentLength == 0) {
	    throw (new PhoneIDException ("Download failed"));
	}

	// make sure there's no record - if there are some, delete them
	
	// open / create record store
	RecordStore rs = RecordStore.openRecordStore( Definition.KDBRecordStoreName, true );
	rs.addRecord(content, 0, contentLength);
	rs.closeRecordStore();
    }
    
}
    
