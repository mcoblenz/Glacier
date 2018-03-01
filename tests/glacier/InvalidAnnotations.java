import edu.cmu.cs.glacier.qual.*;

// ::error: (type.invalid)
@GlacierBottom class InvalidBottom {};

public class InvalidAnnotations {
    // ::error: (type.invalid)
    InvalidBottom b;
}
