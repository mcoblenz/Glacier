## Glacier helps keep your immutable data immutable.

### What does Glacier do? ###
Glacier enforces transitive class immutability in Java.
* Transitive: if a class is immutable, then every field must be immutable. This means that all reachable state from an immutable object's fields is immutable
* Class: the immutability of an object depends only on its class's immutability declaration.
* Immutability: state in an object is not changable through any reference to the object.

### Is Glacier ready to use? ###
Glacier works, but it has very limited knowledge of which JDK classes are immutable. You can help! Check out the source code, edit src/edu/cmu/cs/glacier/jdk.astub with annotations for the classes you care about, and submit a pull request. 

### How do I use Glacier? ###
A Glacier binary release is available!
1. Add the Glacier Maven repository to your list of repositories: http://www.cs.cmu.edu/~mcoblenz/maven2
2. Add a dependency on Glacier. The component is edu.cmu.cs.glacier:glacier:0.1.
3. Add the Glacier annotation processor to your build configuration (edu.cmu.cs.glacier.GlacierChecker).
4. Annotate your immutable classes with `@Immutable`. You will need to import edu.cmu.cs.glacier.qual.Immutable.
5. Build.

For example, here is a simple bogus class that you can use for testing. If everything is working, Glacier will report an error:
~~~~
import edu.cmu.cs.glacier.qual.*;

@Immutable public class BogusImmutable {
    java.util.Date d;
}
~~~~

### Publications
Glacier has been shown to help users express and enforce immutability, preventing bugs. Please take a look at our paper:

Michael Coblenz, Whitney Nelson, Jonathan Aldrich, Brad Myers, and Joshua Sunshine. 2017. Glacier: transitive class immutability for Java. In Proceedings of the 39th 
International Conference on Software Engineering (ICSE '17), May 20 - 28, 2017,  496-506. [ACM DL](http://dl.acm.org/citation.cfm?id=3097428)

Michael Coblenz, Joshua Sunshine, Jonathan Aldrich, Brad Myers, Sam Weber, Forrest Shull, "Exploring Language Support for Immutability" ICSE'2016: The 38th International Conference on Software Engineering, Austin, TX, May 14 - 22, 2016. pp. 736-747. [ACM DL](http://dl.acm.org/citation.cfm?id=2884798)

Michael Coblenz, Joshua Sunshine, Brad Myers, Sam Weber, Forrest Shull, "Comparing Transitive to Non-Transitive Object Immutability", The Sixth Workshop on Evaluation and Usability of Programming Languages and Tools (PLATEAU'2015), at SPLASH 2015, October 26, 2015, Pittsburgh, PA, pp. 47-48. [ACM DL](http://dl.acm.org/citation.cfm?id=2846688)

If you want to replicate the study, you can use the same materials we did. You'll need to install IntelliJ (we used IntelliJ IDEA Community 2016.2) and JDK 1.8 separately. [Replication Package](https://raw.githubusercontent.com/mcoblenz/Glacier/master/ReplicationPackage.zip)
