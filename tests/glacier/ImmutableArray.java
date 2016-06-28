import edu.cmu.cs.glacier.qual.Immutable;

@Immutable class ImmutableArray {
    private byte @Immutable [] _rgb;

    private String @Immutable [] _strings;

    // Immutable array of mutable objects is mutable.
    //::error: (glacier.mutable.invalid)
    private java.util.Date @Immutable [] _dates;

    // Mutable array of primitives is mutable.
    //::error: (glacier.mutable.invalid)
    private int [] _ints;
}
