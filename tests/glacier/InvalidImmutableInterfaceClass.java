package edu.cmu.cs.glacier.tests;

import edu.cmu.cs.glacier.qual.Immutable;

import java.lang.Cloneable;

@Immutable interface ImmutableInterface {
}

//:: error: glacier.interface.invalid
public class InvalidImmutableInterfaceClass implements Cloneable, ImmutableInterface {

}
