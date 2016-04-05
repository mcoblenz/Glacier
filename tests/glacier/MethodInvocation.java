package edu.cmu.cs.glacier.tests;

import edu.cmu.cs.glacier.qual.*;

@Immutable interface Interface {
	public void doStuff();
}

 public class MethodInvocation {
	public void foo() {
		Interface i = null;
		i.doStuff();
	}
}
