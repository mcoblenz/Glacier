import edu.cmu.cs.glacier.qual.*;

@Immutable class ImmutClass {
}

@MaybeMutable class MutableClass {
}

class DefaultMutableClass {
}

@Immutable interface ImmutInterface {
}

interface MutableInterface {
}

public class ConflictingAnnotations {
    // ::error: (type.invalid)
    @MaybeMutable ImmutClass o1;

    // ::error: (type.invalid)
    @Immutable MutableClass o2;

    // ::error: (type.invalid)
    @Immutable DefaultMutableClass o3;

    // ::error: (type.invalid)
    @MaybeMutable ImmutInterface i1;

    // ::error: (type.invalid)
    @Immutable MutableInterface i2;
}
