package edu.cmu.cs.glacier.tests;

import edu.cmu.cs.glacier.qual.Immutable;


class TE_Inner { 
	int x;
}

public @Immutable class TransitivityError {
	// :: error: glacier.mutable.invalid
	TE_Inner i;
	
	public TransitivityError() {
	}
	
	public void test() {
		
	}
}
