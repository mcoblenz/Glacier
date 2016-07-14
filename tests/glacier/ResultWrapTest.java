import edu.cmu.cs.glacier.qual.MaybeMutable;
	
	public class ResultWrapTest {
		
		ResultWrapTest() {
			// while visiting this, the return type must be annotated correctly?
		}
		
		static class ResultWrap<T extends @MaybeMutable Object> {
		}
		
		final ResultWrap<String> input = null;
	}