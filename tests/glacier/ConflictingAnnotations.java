import edu.cmu.cs.glacier.qual.*;

@Immutable class ImmutClass {
}

@Mutable class MutableClass {
}

class DefaultMutableClass {
}

@Immutable interface ImmutInterface {
}

interface MutableInterface {
}

public class ConflictingAnnotations {
    //::error: (type.invalid)
    @Mutable ImmutClass o1;

    //::error: (type.invalid)
    @Immutable MutableClass o2;

    //::error: (type.invalid)
    @Immutable DefaultMutableClass o3;

    //::error: (type.invalid)
    @Mutable ImmutInterface i1;

    //::error: (type.invalid)
    @Immutable MutableInterface i2;
}
