package net.sourceforge.keepassj2me.tests;

import net.sourceforge.keepassj2me.keydb.KeydbDatabase;
import net.sourceforge.keepassj2me.keydb.KeydbEntry;
import net.sourceforge.keepassj2me.keydb.KeydbException;
import net.sourceforge.keepassj2me.keydb.KeydbGroup;
import net.sourceforge.keepassj2me.keydb.KeydbLockedException;
import net.sourceforge.keepassj2me.keydb.KeydbUtil;
import j2meunit.framework.Test;
import j2meunit.framework.TestCase;
import j2meunit.framework.TestMethod;
import j2meunit.framework.TestSuite;

public class KeydbTest extends TestCase {
	private String pass = "pass";
	private byte[] keyfile = new byte[] {1,2,3,4,5,6,7,8,9,10};
	private int rounds = 100;
	private KeydbDatabase db;
	
	public KeydbTest()
	{
	}

	public KeydbTest(String sTestName, TestMethod rTestMethod)
	{
		super(sTestName, rTestMethod);
	}
	
	public Test suite()
	{
		TestSuite aSuite = new TestSuite();
		
		aSuite.addTest(new KeydbTest("testDb", new TestMethod() { public void run(TestCase tc) {((KeydbTest) tc).testDb(); } }));
		aSuite.addTest(new KeydbTest("testDbKey32", new TestMethod() { public void run(TestCase tc) {((KeydbTest) tc).testUseKey32(); } }));
		aSuite.addTest(new KeydbTest("testDbKey64", new TestMethod() { public void run(TestCase tc) {((KeydbTest) tc).testUseKey64(); } }));
		aSuite.addTest(new KeydbTest("testDbKey64invalid", new TestMethod() { public void run(TestCase tc) {((KeydbTest) tc).testUseKey64invalid(); } }));
		aSuite.addTest(new KeydbTest("testGroups", new TestMethod() { public void run(TestCase tc) {((KeydbTest) tc).testGroups(); } }));
		aSuite.addTest(new KeydbTest("testGroupsDeleteLast", new TestMethod() { public void run(TestCase tc) {((KeydbTest) tc).testGroupsDeleteLast(); } }));
		aSuite.addTest(new KeydbTest("testGroupsDeleteMiddle", new TestMethod() { public void run(TestCase tc) {((KeydbTest) tc).testGroupsDeleteMiddle(); } }));
		aSuite.addTest(new KeydbTest("testGroupsDeleteFirst", new TestMethod() { public void run(TestCase tc) {((KeydbTest) tc).testGroupsDeleteFirst(); } }));
		aSuite.addTest(new KeydbTest("testEntries", new TestMethod() { public void run(TestCase tc) {((KeydbTest) tc).testEntries(); } }));

		return aSuite;
	}

	protected void out(String msg) {
		System.out.println();
		System.out.println("--------------------------------------------");
		System.out.println(msg);
		System.out.println("--------------------------------------------");
	}
	
	/* PRIMITIVES */
	
	private void createDb() {
		out("CREATE DB");
		db = new KeydbDatabase();
		assertNotNull("Database", db);
		try {
			db.create(pass, keyfile, rounds);
		} catch (KeydbException e) {
			assertTrue("create", false);
		}
	}
	
	private void lockDb() {
		out("LOCK");
		db.lock();
		assertTrue("lock", db.isLocked());
	}
	
	private void unlockDb() {
		out("UNLOCK");
		try {
			db.unlock(pass, keyfile);
		} catch (KeydbException e) {
			assertTrue("unlock", false);
		}
		assertTrue("unlock", !db.isLocked());
	}
	
	private String getGroupHash(KeydbGroup gr) {
		return KeydbUtil.hashToString(KeydbUtil.hash("group:"+gr.id+":"+gr.parentId+":"+gr.level+":"+gr.imageIndex));
	}
	private KeydbGroup createGroup(int gid, int level) {
		KeydbGroup gr = new KeydbGroup(db);
		try {
			gr.id = db.getUniqueGroupId();
		} catch (KeydbLockedException e) {
			assertTrue("getUniqueGroupId", false);
		}
		gr.parentId = gid;
		gr.level = level;
		gr.imageIndex = gr.id & 0x3F;
		gr.name = getGroupHash(gr);
		return gr;
	}
	private void checkGroup(KeydbGroup gr) {
		assertEquals(gr.name, getGroupHash(gr));
	}

	private String getEntryHash(KeydbEntry en) {
		return KeydbUtil.hashToString(KeydbUtil.hash(
				"entry:"+KeydbUtil.hashToString(KeydbUtil.hash(en.getUUID()))
				+":"+en.groupId
				+":"+en.imageIndex
				+":"+en.getNote()
				+":"+en.getPassword()
				+":"+en.getUrl()
				+":"+en.getUsername()
				+":"+KeydbUtil.hashToString(en.getBinaryData())
				+":"+en.getBinaryDesc()
			));
	}
	private void changeEntryData(KeydbEntry en) {
		byte[] uuid = en.createUUID();
		en.setUUID(uuid);
		en.imageIndex = uuid[0] & 0x3F;
		en.setNote(KeydbUtil.hashToString(KeydbUtil.hash("note:"+uuid[1]+":"+uuid[2])));
		en.setPassword(KeydbUtil.hashToString(KeydbUtil.hash("pass:"+uuid[3]+":"+uuid[4])));
		en.setUrl(KeydbUtil.hashToString(KeydbUtil.hash("url:"+uuid[5]+":"+uuid[6])));
		en.setUsername(KeydbUtil.hashToString(KeydbUtil.hash("user:"+uuid[7]+":"+uuid[8])));
		en.setBinaryData(KeydbUtil.hash("data:"+uuid[8]+":"+uuid[9]));
		en.setBinaryDesc(KeydbUtil.hashToString(KeydbUtil.hash("desc:"+uuid[10]+":"+uuid[11])));
		en.title = getEntryHash(en);
	}
	private KeydbEntry createEntry(int gid) {
		KeydbEntry en = new KeydbEntry(db);
		en.groupId = gid;
		changeEntryData(en);
		return en;
	}
	private void checkEntry(KeydbEntry en) {
		assertEquals(en.title, getEntryHash(en));
	}
	
	/* TESTS */

	/**
	 * Test db creation, locking and unlocking
	 */
	public void testDb() {
		this.createDb();
		KeydbGroup gr = new KeydbGroup(db);
		gr.save();
		this.lockDb();
		this.unlockDb();
		db.close();
	}

	/**
	 * Test db keyfile 32
	 */
	public void testUseKey32() {
		byte[] key = keyfile;
		
		//32 - key
		keyfile = new byte[] {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32};
		this.createDb();
		KeydbGroup gr = new KeydbGroup(db);
		gr.save();
		this.lockDb();
		this.unlockDb();
		db.close();
		
		keyfile = key;
	};
	
	/**
	 * Test db keyfile 64
	 */
	public void testUseKey64() {
		byte[] key = keyfile;
		
		//64 - key hex string
		keyfile = new String("0123456789abcdef0123456789abcdef0123456789ABCDEF0123456789ABCDEF").getBytes();
		this.createDb();
		KeydbGroup gr = new KeydbGroup(db);
		gr.save();
		this.lockDb();
		this.unlockDb();
		db.close();
		
		keyfile = key;
	}

	/**
	 * Test db keyfile 64 invalid
	 */
	public void testUseKey64invalid() {
		byte[] key = keyfile;
		
		//64 - key hex string
		keyfile = new String("qwerty6789abcdef0123456789abcdef0123456789abcdef0123456789abcdef").getBytes();
		keyfile[0] = (byte) 200;
		this.createDb();
		KeydbGroup gr = new KeydbGroup(db);
		gr.save();
		this.lockDb();
		this.unlockDb();
		db.close();
		
		keyfile = key;
	}
	
	/**
	 * Check massive group creation and deletion 
	 */
	public void testGroups() {
		this.createDb();
		
		int groups_count = 20;//100

		out("CREATE GROUPS");
		int[] ids = new int[groups_count];
		for(int i = 0; i < groups_count; ++i) {
			KeydbGroup gr = createGroup(0, 0);
			gr.save();
			ids[i] = gr.id;
		}
		assertTrue("change", db.isChanged());
		
		this.lockDb();
		this.unlockDb();
		
		assertEquals(groups_count, db.getHeader().getGroupsCount());
		
		out("CHECK GROUPS");
		for(int i = 0; i < groups_count; ++i) {
			KeydbGroup gr = null;
			try {
				gr = db.getGroup(ids[i]);
			} catch (KeydbException e) {
				assertTrue("getGroup", false);
			}
			assertNotNull("group", gr);
			checkGroup(gr);
		}
		
		out("REMOVE GROUPS");
		for(int i = 0; i < groups_count; i += 2) {
			KeydbGroup gr = null;
			try {
				gr = db.getGroup(ids[i]);
			} catch (KeydbException e) {
				assertTrue("getGroup", false);
			}
			try {
				db.deleteGroup(gr.index);
			} catch (KeydbLockedException e) {
				assertTrue("delete group", false);
			}
		}
		
		out("CHECK GROUPS");
		for(int j = 0; j < groups_count; j += 2) {
			int i = j + 1;
			KeydbGroup gr = null;
			try {
				gr = db.getGroup(ids[i]);
			} catch (KeydbException e) {
				assertTrue("getGroup", false);
			}
			assertNotNull("group", gr);
			checkGroup(gr);
		}
		
		db.close();
	}
	
	/**
	 * Check group deletion: last
	 */
	public void testGroupsDeleteLast() {
		this.createDb();
		
		out("CREATE GROUPS");
		
		KeydbGroup gr;
		
		gr = createGroup(0, 0);
		gr.save();
		int id1 = gr.id;

		gr = createGroup(0, 0);
		gr.save();
		int id2 = gr.id;

		gr = createGroup(0, 0);
		gr.save();
		int id3 = gr.id;
		
		out("REMOVE GROUPS");
		try {
			gr = db.getGroup(id3);
		} catch (KeydbException e) {
			assertTrue("getGroup", false);
		}
		try {
			db.deleteGroup(gr.index);
		} catch (KeydbLockedException e) {
			assertTrue("delete group", false);
		}
		
		out("CHECK GROUPS");
		try {
			gr = db.getGroup(id1);
		} catch (KeydbException e) {
			assertTrue("getGroup", false);
		}
		assertNotNull("group", gr);
		checkGroup(gr);
		
		try {
			gr = db.getGroup(id2);
		} catch (KeydbException e) {
			assertTrue("getGroup", false);
		}
		assertNotNull("group", gr);
		checkGroup(gr);
		
		db.close();
	}

	/**
	 * Check group deletion: middle
	 */
	public void testGroupsDeleteMiddle() {
		this.createDb();
		
		out("CREATE GROUPS");
		
		KeydbGroup gr;
		
		gr = createGroup(0, 0);
		gr.save();
		int id1 = gr.id;

		gr = createGroup(0, 0);
		gr.save();
		int id2 = gr.id;

		gr = createGroup(0, 0);
		gr.save();
		int id3 = gr.id;
		
		out("REMOVE GROUPS");
		try {
			gr = db.getGroup(id2);
		} catch (KeydbException e) {
			assertTrue("getGroup", false);
		}
		try {
			db.deleteGroup(gr.index);
		} catch (KeydbLockedException e) {
			assertTrue("delete group", false);
		}
		
		out("CHECK GROUPS");
		try {
			gr = db.getGroup(id1);
		} catch (KeydbException e) {
			assertTrue("getGroup", false);
		}
		assertNotNull("group", gr);
		checkGroup(gr);
		
		try {
			gr = db.getGroup(id3);
		} catch (KeydbException e) {
			assertTrue("getGroup", false);
		}
		assertNotNull("group", gr);
		checkGroup(gr);
		
		db.close();
	}
	
	/**
	 * Check group deletion: first
	 */
	public void testGroupsDeleteFirst() {
		this.createDb();
		
		out("CREATE GROUPS");
		
		KeydbGroup gr;
		
		gr = createGroup(0, 0);
		gr.save();
		int id1 = gr.id;

		gr = createGroup(0, 0);
		gr.save();
		int id2 = gr.id;

		gr = createGroup(0, 0);
		gr.save();
		int id3 = gr.id;
		
		out("REMOVE GROUPS");
		try {
			gr = db.getGroup(id1);
		} catch (KeydbException e) {
			assertTrue("getGroup", false);
		}
		try {
			db.deleteGroup(gr.index);
		} catch (KeydbLockedException e) {
			assertTrue("delete group", false);
		}
		
		out("CHECK GROUPS");
		try {
			gr = db.getGroup(id2);
		} catch (KeydbException e) {
			assertTrue("getGroup", false);
		}
		assertNotNull("group", gr);
		checkGroup(gr);
		
		try {
			gr = db.getGroup(id3);
		} catch (KeydbException e) {
			assertTrue("getGroup", false);
		}
		assertNotNull("group", gr);
		checkGroup(gr);
		
		db.close();
	}

	/**
	 * Check massive entries creation and deletion 
	 */
	public void testEntries() {
		this.createDb();
		
		out("CREATE GROUP");
		KeydbGroup gr;
		gr = createGroup(0, 0);
		gr.save();
		int id1 = gr.id;
		
		int entries_count = 20;//100

		out("CREATE ENTRIES");
		for(int i = 0; i < entries_count; ++i) {
			KeydbEntry en = createEntry(id1);
			en.save();
		}
		assertTrue("change", db.isChanged());
		
		this.lockDb();
		this.unlockDb();
		
		assertEquals(entries_count, db.getHeader().getEntriesCount());
		
		out("CHECK ENTRIES");
		for(int i = 0; i < entries_count; ++i) {
			KeydbEntry en = null;
			try {
				en = db.getEntryByIndex(id1, i);
			} catch (KeydbException e) {
				assertTrue("getEntry", false);
			}
			assertNotNull("entry", en);
			checkEntry(en);
		}
		
		out("REMOVE ENTRIES");
		for(int i = 0; i < entries_count / 2; ++i) {
			try {
				db.deleteEntry(i);
			} catch (KeydbLockedException e) {
				assertTrue("delete entry", false);
			}
		}
		
		assertEquals(entries_count / 2, db.getHeader().getEntriesCount());
		
		out("CHECK ENTRIES");
		for(int i = 0; i < entries_count / 2; ++i) {
			KeydbEntry en = null;
			try {
				en = db.getEntryByIndex(id1, 1);
			} catch (KeydbException e) {
				assertTrue("getEntry", false);
			}
			assertNotNull("entry", en);
			checkEntry(en);
		}
		
		out("DELETE GROUP");
		try {
			db.deleteGroup(0);
		} catch (KeydbLockedException e) {
			assertTrue("delete group", false);
		}
		
		assertEquals(0, db.getHeader().getEntriesCount());
		assertEquals(0, db.getHeader().getGroupsCount());
		
		db.close();
	}
}
