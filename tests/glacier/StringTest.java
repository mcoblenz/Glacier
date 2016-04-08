package edu.cmu.cs.glacier.tests;

import edu.cmu.cs.glacier.qual.Immutable;

import java.lang.String;

public @Immutable class StringTest {
    String s; // no error expected here because String should be treated as if it were declared @Immutable.
}
