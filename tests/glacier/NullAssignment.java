import edu.cmu.cs.glacier.qual.Immutable;

interface AnInterface {};


public class NullAssignment {
    public void takeObj(Object o) {
    }
    
    public void foo() {
	AnInterface i = null;
	takeObj(i);
    }
}
