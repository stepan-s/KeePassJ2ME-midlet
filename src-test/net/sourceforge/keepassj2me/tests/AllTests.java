package net.sourceforge.keepassj2me.tests;

import j2meunit.framework.Test;
import j2meunit.framework.TestCase;
import j2meunit.framework.TestSuite;

public class AllTests extends TestCase {
	public AllTests()
	{
		super("null");
	}

	public AllTests(String name)
	{
		super(name);
	}
	
	public Test suite()
	{
		TestSuite suite = new TestSuite();
		
		suite.addTest(new KeydbTest().suite());

		return suite;
	}
}
