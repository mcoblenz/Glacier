package edu.cmu.cs.glacier.tests;

import edu.cmu.cs.glacier.qual.Immutable;

public @Immutable class ConstructorAssignment {
    public int x = 3; // static assignment is OK
    
    ConstructorAssignment() {
	x = 4; // constructor assignment is OK
    }

    void setX() {
	//::error: (glacier.assignment)
	x = 5;
    }
}

class OtherClass {
    OtherClass() {
	ConstructorAssignment c = new ConstructorAssignment();
	//::error: (glacier.assignment)
	c.x = 6;
    }
}
