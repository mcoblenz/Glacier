import edu.cmu.cs.glacier.qual.*;


@Immutable  interface SColor {
}

@Immutable abstract class AbstractColorAdv implements SColor {
}

//::error: (glacier.subclass.mutable)
class ColorImpl extends AbstractColorAdv {
	//::error: (assignment.type.incompatible)
	public static final AbstractColorAdv BLACK = new ColorImpl("#000000");
	
	
	ColorImpl(String fontColor) {
		
	}
}

public class FontImpl {
	FontImpl(String fontColor) {
		SColor a = new ColorImpl(fontColor);
		SColor c = fontColor != null ? new ColorImpl(fontColor) : null;
	}
}