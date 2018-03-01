import edu.cmu.cs.glacier.qual.*;
import java.lang.String;

// Classes can't be annotated ReadOnly in their declarations; @ReadOnly is only for method parameters.
// ::error: (glacier.readonly.class)
@ReadOnly public class ReadOnlyClass {
}

class ReadOnlyMethodClass {
    @ReadOnly ReadOnlyClass roc;

    int @ReadOnly [] readOnlyIntArray;
    
    // ::error: (type.invalid.annotations.on.use)
    void takeReadOnlyString(@ReadOnly String foo) {}
    void takeReadOnlyArray(String @ReadOnly [] foo) {
	// ::error: (glacier.assignment.array)
	foo[0] = "Hello, world!";
    }

    void takeImmutableArray(String @Immutable [] foo) {
	// ::error: (glacier.assignment.array)
	foo[0] = "Hello, world!";
    }
}
