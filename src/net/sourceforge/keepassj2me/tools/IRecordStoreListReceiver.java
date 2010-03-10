package net.sourceforge.keepassj2me.tools;

/**
 * Interface for enumerate RecordStore database records 
 * @author Stepan Strelets
 */
public interface IRecordStoreListReceiver {
	public void listRecord(String name);
}
