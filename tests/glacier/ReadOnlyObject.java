import edu.cmu.cs.glacier.qual.ReadOnly;

public class ReadOnlyObject {
    public Object foo() {
	Object cat = null;
	return true ? String.valueOf(1) : cat;
    }
    
}
