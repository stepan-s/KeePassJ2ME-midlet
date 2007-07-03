package org.phoneid.keepassinstaller;

import javax.swing.JFrame;
import javax.swing.JLabel;
import java.awt.EventQueue;
import javax.swing.JFileChooser;
import java.io.*;
import java.util.jar.*;
import java.nio.channels.FileChannel;

public final class KeePassInstaller implements Runnable {

    public static void main(String[] args) {
        EventQueue.invokeLater (new KeePassInstaller());
    } 

    public void run() {
	try {
	    //Create a file chooser
	    JFileChooser fc = new JFileChooser();
	    
	    //Add a custom file filter for *.kdb file
	    fc.addChoosableFileFilter(new KDBFilter());
	    // fc.setAcceptAllFileFilterUsed(true);
	    
	    //In response to a button click:
	    int returnVal = fc.showOpenDialog(null);
	    
	    if (returnVal == JFileChooser.APPROVE_OPTION) {
		File dir = fc.getCurrentDirectory();
		File file = fc.getSelectedFile();
		//This is where a real application would open the file.
		System.out.println (file.getAbsolutePath());
		
		// createJar(file.getAbsolutePath(), file.getName());
		copyFile(file.getAbsolutePath(), "Database.kdb");
		
	    } else {
		System.err.println ("Cannot open file");
		System.exit(1);
	    }
	} catch (IOException e) {
	    e.printStackTrace();
	    System.exit(1);
	}
    }

    public void copyFile(String from, String to)
	throws IOException
    {
	// Create channel on the source
        FileChannel srcChannel = new FileInputStream(from).getChannel();
	// Create channel on the destination
        FileChannel dstChannel = new FileOutputStream(to).getChannel();
    
        // Copy file contents from source to destination
        dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
    
        // Close the channels
        srcChannel.close();
        dstChannel.close();
    }

    public void copyStream(InputStream in, OutputStream out)
	throws IOException
    {
	byte buffer[] = new byte[1024];
	
	while (true) {
	    int nRead = in.read(buffer, 0, buffer.length);
	    if (nRead <= 0)
		break;
	    out.write(buffer, 0, nRead);
	}
	//in.close();
    }
    
    public void createJar(String pathKDB, String nameKDB)
	throws IOException
    {
	// JarEntry jarEntry = new JarEntry("KeePassJ2ME-kdb.jar");
	JarEntry jarAdd;

	// copy KDB file to current directory
	copyFile(pathKDB, nameKDB);
	
	// read manifest
	FileInputStream inStreamManifest = new FileInputStream("META-INF/MANIFEST.MF");
	FileOutputStream outStream = new FileOutputStream("KeePassJ2ME-kdb.jar");
	JarOutputStream out = new JarOutputStream(outStream, new Manifest(inStreamManifest));

	// copyStream(inStreamManifest, out);
	
	jarAdd = new JarEntry(nameKDB);
	out.putNextEntry(jarAdd);
	//jarAdd = new JarEntry("org/");
	//out.putNextEntry(jarAdd);
	
	// Write file to archive
	FileInputStream in = new FileInputStream(nameKDB);
	copyStream(in, out);
	in.close();
	
	out.close();
	/*
	  byte buffer[] = new byte[BUFFER_SIZE];
	  // Open archive file

	    for (int i = 0; i < tobeJared.length; i++) {
        if (tobeJared[i] == null || !tobeJared[i].exists()
            || tobeJared[i].isDirectory())
          continue; // Just in case...
        System.out.println("Adding " + tobeJared[i].getName());

        // Add archive entry
        JarEntry jarAdd = new JarEntry(tobeJared[i].getName());
        jarAdd.setTime(tobeJared[i].lastModified());
        out.putNextEntry(jarAdd);

        // Write file to archive
        FileInputStream in = new FileInputStream(tobeJared[i]);
        while (true) {
          int nRead = in.read(buffer, 0, buffer.length);
          if (nRead <= 0)
            break;
          out.write(buffer, 0, nRead);
        }
        in.close();
      }

      out.close();
      stream.close();
      System.out.println("Adding completed OK");
	    */
    }
}
