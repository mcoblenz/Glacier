package edu.cmu.cs.glacier.tests;

import edu.cmu.cs.glacier.qual.*;

@Immutable public class EqualsTest {
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		return false;
	}
}

class EqualsTest2 {
	// ::error: (override.param.invalid)
	public boolean equals(@Immutable Object obj) {
		if (this == obj)
			return true;
		return false;
	}
}
