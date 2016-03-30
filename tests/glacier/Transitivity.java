package edu.cmu.cs.glacier.tests;

import edu.cmu.cs.glacier.qual.Immutable;


@Immutable class Inner { 
	int x;
}

public @Immutable class Transitivity {
	Inner i;
	
	public Transitivity() {
	}
	
	public void test() {
		
	}
}
