package net.sourceforge.keepassj2me.keydb;

public interface IKeydbGroupContentRecever {
	public void addKeydbGroup(KeydbGroup group);
	public void totalGroups(int count);
	public void addKeydbEntry(KeydbEntry entry);
}
