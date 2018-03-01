import edu.cmu.cs.glacier.qual.Immutable;

@Immutable
public class ImmutablePrimitiveContainer {
    int x;

    public void setX(int x) {
	// ::error: (glacier.assignment)
	this.x = x;
    }
}
