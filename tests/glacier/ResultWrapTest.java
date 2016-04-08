import edu.cmu.cs.glacier.qual.Mutable;
	
	public class ResultWrapTest {
		
		ResultWrapTest() {
			// while visiting this, the return type must be annotated correctly?
		}
		
		static class ResultWrap<T extends @Mutable Object> {
		}
		
		final ResultWrap<String> input = null;
	}