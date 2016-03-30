package tests;

import static org.checkerframework.framework.test.TestConfigurationBuilder.buildDefaultConfiguration;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.checkerframework.framework.test.CheckerFrameworkTest;
import org.junit.Test;
import org.checkerframework.framework.test.TestConfiguration;
import org.checkerframework.framework.test.TestUtilities;
import org.checkerframework.framework.test.TypecheckExecutor;
import org.checkerframework.framework.test.TypecheckResult;
import org.junit.runners.Parameterized.Parameters;

/**
 * JUnit tests for the Glacier Checker -- testing -AskipDefs command-line argument.
 */
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
}
