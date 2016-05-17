import java.util.Arrays;
import edu.cmu.cs.glacier.qual.Immutable;

public class ColorImpl {
    private byte @Immutable [] _rgb;
    private byte [] _mutableRgb;
    
    public int hashCode() {
    	Arrays.hashCode(_mutableRgb); // This should be OK too
    	return Arrays.hashCode(_rgb);
    }
}
