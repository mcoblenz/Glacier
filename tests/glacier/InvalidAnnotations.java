import edu.cmu.cs.glacier.qual.*;

// ::error: (type.invalid.annotations.on.use)
@GlacierBottom class InvalidBottom {};

public class InvalidAnnotations {
    // ::error: (type.invalid.annotations.on.use)
    InvalidBottom b;
}
