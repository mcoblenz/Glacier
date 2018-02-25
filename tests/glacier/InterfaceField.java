import edu.cmu.cs.glacier.qual.Immutable;

interface AnInterface {};

@Immutable interface ImmutableInterface extends AnInterface {};

@Immutable public class InterfaceField {
    // ::error: (glacier.mutable.invalid)
    AnInterface o;
    ImmutableInterface o2;
}
