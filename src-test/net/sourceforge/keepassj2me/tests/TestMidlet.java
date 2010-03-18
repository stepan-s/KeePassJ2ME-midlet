package net.sourceforge.keepassj2me.tests;

import j2meunit.midletui.TestRunner;

public class TestMidlet extends TestRunner {
	public TestMidlet() {
		
	}
	public void startApp() {
		start(new String[] { "net.sourceforge.keepassj2me.tests.AllTests" });
	}
}
