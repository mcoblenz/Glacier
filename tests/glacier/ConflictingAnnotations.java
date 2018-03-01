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
    // ::error: (type.invalid.annotations.on.use)
    @MaybeMutable ImmutClass o1;

    // ::error: (type.invalid.annotations.on.use)
    @Immutable MutableClass o2;

    // ::error: (type.invalid.annotations.on.use)
    @Immutable DefaultMutableClass o3;

    // ::error: (type.invalid.annotations.on.use)
    @MaybeMutable ImmutInterface i1;

    // ::error: (type.invalid.annotations.on.use)
    @Immutable MutableInterface i2;
}
