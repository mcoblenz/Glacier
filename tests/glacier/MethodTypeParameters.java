import edu.cmu.cs.glacier.qual.Immutable;

class TypeParameters<E extends @Immutable Object> {
    static <E> TypeParameters<E> asImmutableList(@Immutable Object[] elements) {
	return null;
    }
}
