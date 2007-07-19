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
import javax.microedition.rms.*;

public class HTTPConnectionThread
    extends Thread
{
    String mSecretCode = null;
    String mURL = null;
    KeePassMIDlet mMIDlet;

    public HTTPConnectionThread(String secretCode, String url, KeePassMIDlet midlet) {
	mSecretCode = secretCode;
	mURL = url;
	mMIDlet = midlet;
    }
    
    public void run() {
	try {
	    connect(mSecretCode, mURL);
	} catch (Exception e) {
	    System.out.println ("Error from connect()");
	    MessageBox msg = new MessageBox(Definition.TITLE, "Error from connect(): " + e.toString(),
					    AlertType.ERROR, mMIDlet, false,
					    null);
	    msg.waitForDone();
	    mMIDlet.exit();
	}
    }

    private void connect(String secretCode, String url)
	throws IOException, RecordStoreException, PhoneIDException
    {
	HttpConnection hc = null;
	InputStream in = null;
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
	if (contentLength == 0) {
	    throw (new PhoneIDException ("Download failed"));
	}
	
	byte[] content = new byte[contentLength];
	int length = in.read(content);
	
	in.close();
	hc.close();
	
	// Show the response to the user.
	System.out.println ("Downloaded " + contentLength + " bytes");

	mMIDlet.storeKDBInRecordStore(content);
    }
    
}
    
