## Glacier helps keep your immutable data immutable.

### What does Glacier do? ###
Glacier enforces transitive class immutability in Java.
* Transitive: if a class is immutable, then every field must be immutable. This means that all reachable state from an immutable object's fields is immutable
* Class: the immutability of an object depends only on its class's immutability declaration.
* Immutability: state in an object is not changable through any reference to the object.

### How do I use Glacier? ###
Using Glacier is easy:
1. Install Glacier.
2. Add the Glacier checker to your build configuration.
3. Annotate your code.
4. Build.

### Publications
Glacier has been shown to help users express and enforce immutability, preventing bugs. Please take a look at our paper:

Michael Coblenz, Whitney Nelson, Jonathan Aldrich, Brad Myers, and Joshua Sunshine. 2017. Glacier: transitive class immutability for Java. In Proceedings of the 39th International Conference on Software Engineering (ICSE '17). IEEE Press, Piscataway, NJ, USA, 496-506. DOI: https://doi.org/10.1109/ICSE.2017.52

If you want to replicate the study, you can use the same materials we did. You'll need to install IntelliJ (we used IntelliJ IDEA Community 2016.2) and JDK 1.8 separately. [Replication Package](https://raw.githubusercontent.com/mcoblenz/Glacier/master/ReplicationPackage.zip)
