package edu.cmu.cs.glacier.tests;

import edu.cmu.cs.glacier.qual.Immutable;

import java.lang.Cloneable;

@Immutable interface ImmutableInterface {
}

//:: error: glacier.interface.immutable
public class InvalidImmutableInterfaceClass implements Cloneable, ImmutableInterface {

}
