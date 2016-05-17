import edu.cmu.cs.glacier.qual.Immutable;

interface MutableInterface {};

@Immutable public class ImmutableClassMutableInterface implements MutableInterface {
    public static void bar(MutableInterface m) { };

    public static void foo() {
	ImmutableClassMutableInterface x = null;
	bar(x);
    }

};
