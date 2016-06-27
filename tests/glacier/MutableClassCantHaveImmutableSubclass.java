import edu.cmu.cs.glacier.qual.Immutable;


class Mut {
    int x;
}

//::error: (glacier.nonfinalmember)
@Immutable class Immut extends Mut { }
