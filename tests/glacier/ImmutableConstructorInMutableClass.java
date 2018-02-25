package edu.cmu.cs.glacier.tests;

import edu.cmu.cs.glacier.qual.Immutable;



public class ImmutableConstructorInMutableClass { 
	// :: error: (type.invalid.annotations.on.use)
	@Immutable public ImmutableConstructorInMutableClass() {
	}
	
	public void aMethod() {
		
	}
}