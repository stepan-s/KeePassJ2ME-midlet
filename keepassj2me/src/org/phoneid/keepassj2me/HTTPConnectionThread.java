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
    String mURL = null, mUserCode = null, mPassCode = null;
    KeePassMIDlet mMIDlet;

    public HTTPConnectionThread(String url, String userCode, String passCode, KeePassMIDlet midlet) {
	mURL = url;
	mUserCode = userCode;
	mPassCode = passCode;
	mMIDlet = midlet;
    }
    
    public void run() {
	try {
	    connect(mURL, mUserCode, mPassCode);
	} catch (Exception e) {
	    System.out.println ("Error from connect()");
	    MessageBox msg = new MessageBox(Definition.TITLE, "Error from connect(): " + e.toString(),
					    AlertType.ERROR, mMIDlet, false,
					    null);
	    msg.waitForDone();
	    mMIDlet.exit();
	}
    }
    
    private void connect(String url, String userCode, String passCode)
	throws IOException, RecordStoreException, PhoneIDException
    {
	HttpConnection hc = null;
	InputStream in = null;
	String rawData = "usercode=" + userCode + "&passcode=" + passCode;
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
    
