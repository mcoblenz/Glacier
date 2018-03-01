package tests;

import org.checkerframework.framework.test.CheckerFrameworkPerFileTest;
import org.checkerframework.framework.test.PerFileSuite;
import org.junit.runner.Result;
import org.junit.runner.RunWith;
import org.junit.runner.notification.Failure;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
/**
 * JUnit tests for the Glacier Checker -- testing -AskipDefs command-line argument.
 */
/*
public class GlacierTest extends CheckerFrameworkTest {	
	
    public GlacierTest(File testFile) {
	super(testFile,
	      edu.cmu.cs.glacier.GlacierChecker.class,
	      "glacier",
	      "-Anomsgtext");
		System.out.println("GlacierTest constructor");
   }

    
        @Parameters
	public static String[] getTestDirs() {
        	System.out.println("getTestDirs()");
	    return new String[]{"glacier"};
	}
        
        /*
        @Test
        public void run() {
        	System.out.println("RUNNING");
            boolean shouldEmitDebugInfo = true; // TestUtilities.getShouldEmitDebugInfo();
            List<String> customizedOptions = customizeOptions(Collections.unmodifiableList(checkerOptions));
            TestConfiguration config = buildDefaultConfiguration(checkerDir, testFile, checkerName, customizedOptions,
                                                                 shouldEmitDebugInfo);
            TypecheckResult testResult = new TypecheckExecutor().runTest(config);
            TestUtilities.assertResultsAreValid(testResult);
        }
*/

public class GlacierTest {
	public static void main(String[] args) {
		System.out.println("Trying to run tests");
		org.junit.runner.JUnitCore jc = new org.junit.runner.JUnitCore();
		Result run = jc.run(GlacierCheckerTests.class);

		if (run.wasSuccessful()) {
			System.out.println("Run was successful with " + run.getRunCount() + " test(s)!");
		} else {
			System.out.println("Run had " + run.getFailureCount() + " failure(s) out of "
					+ run.getRunCount() + " run(s)!");

			for (Failure f : run.getFailures()) {
				System.out.println(f.toString());
			}
		}
	}
	
	@RunWith(PerFileSuite.class)
	public static class GlacierCheckerTests extends CheckerFrameworkPerFileTest {
	    public GlacierCheckerTests(File testFile) {
	        super(testFile, edu.cmu.cs.glacier.GlacierChecker.class, "glacier", "-Anomsgtext");
	    }

	    @Parameters
	    public static String[] getTestDirs() {
	    	System.out.println("getTestDirs()");
	        return new String[]{"glacier"};
	    }
	}
}



