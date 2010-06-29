package net.sourceforge.keepassj2me.datasource;

import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;

import net.sourceforge.keepassj2me.Icons;
import net.sourceforge.keepassj2me.KeePassException;
import net.sourceforge.keepassj2me.tools.DisplayStack;
import net.sourceforge.keepassj2me.tools.ListTag;

/**
 * @author Stepan Strelets
 */
public class DataSourceSelect implements CommandListener {
	/** Result type - invalid */
	public static final int RESULT_INVALID = -1;
	/** Result type - none selected */
	public static final int RESULT_NONE = -2;
	/** Result type - cancel */
	public static final int RESULT_CANCEL = -3;
	
	private ListTag list;
	private int result = RESULT_CANCEL;

	/**
	 * Construct and display
	 * @param caption form title
	 * @param select source type
	 * @param allow_no allow `none` selection
	 * @param save true - save, false - load
	 * @param midlet
	 */
	public DataSourceSelect(String caption, int select, boolean allow_no, boolean save) {
		Icons icons = Icons.getInstance();
		
		list = new ListTag(caption, Choice.IMPLICIT);

		// No
		if (allow_no)
			list.append("No", icons.getImageById(Icons.ICON_EXIT), RESULT_NONE);
		
		DataSourceAdapter source;
		for(int i = 0; i < DataSourceRegistry.reg.length; ++i) {
			try {
				source = DataSourceRegistry.createDataSource(DataSourceRegistry.reg[i]);
				if (save ? source.canSave() : source.canLoad())
					list.append(source.getFamilyName(), icons.getImageById(source.getIcon()), source.getUid());
			} catch (KeePassException e) {
			}
		};
		list.setSelectedTag(select, true);
		
		list.addCommand(new Command("Cancel", Command.CANCEL, 1));
		Command cmd_ok = new Command("OK", Command.OK, 1);
		list.addCommand(cmd_ok);
		list.setSelectCommand(cmd_ok);
		list.setCommandListener(this);
	}

	public void commandAction(Command cmd, Displayable dsp) {
		switch(cmd.getCommandType()) {
		case Command.OK:
			try {
				result = list.getSelectedTagInt();
			} catch (ArrayIndexOutOfBoundsException e) {
				result = RESULT_INVALID;
			};
			break;
			
		case Command.CANCEL:
			result = RESULT_CANCEL;
			break;
		}

		synchronized (this.list) {
			this.list.notify();
		}
	}

	/**
	 * Get user selection
	 * @return result on from RESULT_*
	 */
	public int getResult() {
		return result;
	}

	/**
	 * wait for user
	 */
	public void displayAndWait() {
		DisplayStack.getInstance().push(list);
		try {
			synchronized (this.list) {
				this.list.wait();
			}
		} catch (Exception e) {}
		DisplayStack.getInstance().pop();
	}
}
