package edu.cmu.cs.glacier.tests;

import java.util.Date;

import edu.cmu.cs.glacier.qual.Immutable;


public @Immutable class InvalidAssignment {
	class Inner {
		public int i;
	}
	
	//:: error: glacier.mutable.invalid
	String s;
	//:: error: glacier.mutable.invalid
	Date d;
	//:: error: glacier.mutable.invalid
	Inner i;
	int x;
	
	public void setString(String s) {
		//:: error: glacier.assignment
		this.s = s;
	}
	
	public void setX(int x) {
		//:: error: glacier.assignment
		this.x = x;
	}
	
	public void setMonth(int month) {
		d.setMonth(month); // No error here; the problem is that d was mutable in the first place.
	}
	
	public void setInner(int i) {
		this.i.i = i; // No error here; the problem is that this.i was mutable in the first place.
	}
}
