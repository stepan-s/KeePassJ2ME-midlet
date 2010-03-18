package net.sourceforge.keepassj2me.tests;

import net.sourceforge.keepassj2me.keydb.KeydbDatabase;
import net.sourceforge.keepassj2me.keydb.KeydbException;
import net.sourceforge.keepassj2me.keydb.KeydbGroup;
import net.sourceforge.keepassj2me.keydb.KeydbLockedException;
import j2meunit.framework.Test;
import j2meunit.framework.TestCase;
import j2meunit.framework.TestMethod;
import j2meunit.framework.TestSuite;

public class KeydbTest extends TestCase {
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

		return aSuite;
	}

	protected void out(String msg) {
		System.out.println();
		System.out.println("--------------------------------------------");
		System.out.println(msg);
		System.out.println("--------------------------------------------");
	}
	
	public void testDb()
	{
		String pass = "pass";
		byte[] keyfile = new byte[] {1,2,3,4,5,6,7,8,9,10};
		int count = 100;

		out("CREATE DB");
		KeydbDatabase db = new KeydbDatabase();
		assertNotNull("Database", db);
		try {
			db.create(pass, keyfile, 1000);
		} catch (KeydbException e) {
			assertTrue("create", false);
		}
		
		out("CREATE GROUPS");
		int[] ids = new int[count];
		for(int i = 0; i < count; ++i) {
			KeydbGroup gr = new KeydbGroup(db);
			gr.parentId = 0;
			gr.imageIndex = i & 0xF;
			gr.level = 0;
			gr.name = "group"+i;
			gr.save();
			ids[i] = gr.id;
		}
		assertTrue("change", db.isChanged());
		
		out("LOCK");
		db.lock();
		assertTrue("lock", db.isLocked());
		
		out("UNLOCK");
		try {
			db.unlock(pass, keyfile);
		} catch (KeydbException e) {
			assertTrue("unlock", false);
		}
		assertTrue("unlock", !db.isLocked());
		
		out("CHECK GROUPS");
		for(int i = 0; i < count; ++i) {
			KeydbGroup gr = null;
			try {
				gr = db.getGroup(ids[i]);
			} catch (KeydbException e) {
				assertTrue("getGroup", false);
			}
			
			assertNotNull("group", gr);
			assertEquals("group"+i, gr.name);
			assertEquals(i & 0xF, gr.imageIndex);
			assertEquals(0, gr.parentId);
			assertEquals(0, gr.level);
		}
		
		out("REMOVE GROUPS");
		for(int i = 0; i < count; i += 2) {
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
		for(int j = 0; j < count; j += 2) {
			int i = j + 1;
			KeydbGroup gr = null;
			try {
				gr = db.getGroup(ids[i]);
			} catch (KeydbException e) {
				assertTrue("getGroup", false);
			}
			
			assertNotNull("group", gr);
			assertEquals("group"+i, gr.name);
			assertEquals(i & 0xF, gr.imageIndex);
			assertEquals(0, gr.parentId);
			assertEquals(0, gr.level);
		}
	}
}
