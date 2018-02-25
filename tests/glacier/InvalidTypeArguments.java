import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import edu.cmu.cs.glacier.qual.Immutable;
import edu.cmu.cs.glacier.qual.MaybeMutable;

public class InvalidTypeArguments {
    interface MutableObject {}
    @Immutable interface ImmutableObject {}

    class Generic<T> {}
    Generic<MutableObject> mutableObjectGeneric;
    Generic<ImmutableObject> immutableObjectGeneric;

	class BogusImmutableGeneric<@Immutable T>{}
	// :: error: (type.argument.type.incompatible)
	BogusImmutableGeneric<MutableObject> mutableObjectBogusImmutableGeneric;
	BogusImmutableGeneric<ImmutableObject> immutableObjectBogusImmutableGeneric;
		

    class ImmutableGeneric<@Immutable T extends @Immutable Object>{}
    // :: error: (type.argument.type.incompatible)
    ImmutableGeneric<MutableObject> mutableObjectImmutableGeneric;
    ImmutableGeneric<ImmutableObject> immutableObjectImmutableGeneric;

    class MutableGeneric<@MaybeMutable T>{}
    MutableGeneric<MutableObject> mutableObjectMutableGeneric;
    // :: error: (type.argument.type.incompatible)
    MutableGeneric<ImmutableObject> immutableObjectMutableGeneric;

    public class UnmodifiableCollection {
        public Iterator<MutableObject> getCellIterator() {
            Collection<MutableObject> cellCollection = null;
            Collections.unmodifiableCollection(cellCollection);
            return Collections.unmodifiableCollection(cellCollection).iterator();
        }
    }
}
