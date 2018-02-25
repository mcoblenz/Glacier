package edu.cmu.cs.glacier.tests;

import edu.cmu.cs.glacier.qual.Immutable;


class Inner { 
	int x;
}

public @Immutable class TransitivityError {
	// :: error: glacier.mutable.invalid
	Inner i;
	
	public TransitivityError() {
	}
	
	public void test() {
		
	}
}
