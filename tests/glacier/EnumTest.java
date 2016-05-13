import edu.cmu.cs.glacier.qual.Immutable;

@Immutable enum Underline{
    NONE,
    SINGLE,
    DOUBLE,
    SINGLE_ACCOUNTING,
    DOUBLE_ACCOUNTING
}

public class EnumTest {
    Underline u;

    public void foo() {
	u.ordinal();
    }
}
