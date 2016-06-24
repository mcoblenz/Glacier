import edu.cmu.cs.glacier.qual.Immutable;

@Immutable class Superclass<E> {
    void aMethod() {

    }
}

@Immutable public class TypeParameter<E> extends Superclass<E> {
    void aMethod() {

    }
}