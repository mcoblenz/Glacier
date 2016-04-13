package edu.cmu.cs.glacier;

import java.util.List;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import org.checkerframework.framework.source.SourceChecker;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.source.Result;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeInfo;

import edu.cmu.cs.glacier.qual.GlacierBottom;
import edu.cmu.cs.glacier.qual.Immutable;
import edu.cmu.cs.glacier.qual.Mutable;

public class GlacierVisitor extends BaseTypeVisitor<GlacierAnnotatedTypeFactory> {
    /** The {@link SourceChecker} for error reporting. */
    protected final SourceChecker checker;
    

	public GlacierVisitor(BaseTypeChecker checker) {
		super(checker);

		this.checker = checker;
	}
	
	private static boolean modifiersIncludeImmutable(ModifiersTree modifiers) {
		boolean foundImmutable = false;
		
		List <? extends AnnotationTree> annotations = modifiers.getAnnotations();
		for (AnnotationTree a : annotations) {
	        Element element = TreeInfo.symbol((JCTree) a.getAnnotationType());
	        if (element.toString().equals(Immutable.class.getName())) {
	        	foundImmutable = true;
	        }
		}
		
		return foundImmutable;
	}
	
	@Override
	public Void visitClass(ClassTree node, Void p) {
		super.visitClass(node, p);
		
		boolean classIsImmutable = modifiersIncludeImmutable(node.getModifiers());

		/*		
		if (classIsImmutable) {
        	System.out.println("Class" + node + "is immutable!");
		}
		else {
        	System.out.println("Class" + node + "is NOT immutable!");
		}
		*/
		
		if (classIsImmutable) {
			// Check to make sure all fields are immutable.
			List <? extends Tree> members = node.getMembers();
			for (Tree t : members) {
				if (t.getKind() == Kind.VARIABLE) {					
		            AnnotatedTypeMirror variableType = atypeFactory.fromMember(t);
					
					boolean fieldIsImmutable = variableType.hasAnnotation(Immutable.class);
					if (!fieldIsImmutable) {
						TypeMirror underlyingType = variableType.getUnderlyingType();
						
						// Primitive types are always immutable, but we need to separately
						// typecheck them as if they were final.
						if (!(underlyingType instanceof PrimitiveType)) { 
							checker.report(Result.failure("glacier.mutable.invalid"), t);
						}
					}
				}
			}
		}
		
		Tree superclass = node.getExtendsClause();
		if (superclass != null) {
			AnnotatedTypeMirror superclassType = atypeFactory.getAnnotatedType(superclass);
			if (superclassType.hasAnnotation(Immutable.class) && !classIsImmutable) {
				checker.report(Result.failure("glacier.subclass.mutable", node.getSimpleName(), superclass.toString()), node);
			}
		}

		
		// Check to make sure that if any implemented interface is immutable, the class is immutable.
		List<? extends Tree> interfaces = node.getImplementsClause();
		for (Tree implementedInterface : interfaces) {
				AnnotatedTypeMirror interfaceType = atypeFactory.getAnnotatedType(implementedInterface);
				
				if (interfaceType.hasAnnotation(Immutable.class)) {
					if (!classIsImmutable) {
						checker.report(Result.failure("glacier.interface.immutable", node.getSimpleName(), implementedInterface), node);
					}
					break;	
				}
		}
		
		return null;
	}
	
	/* 
	 * In addition to the superclass implementation, this checks to make sure that if this assignment is to a field,
	 * the containing class is mutable. 
	 * @see org.checkerframework.common.basetype.BaseTypeVisitor#visitAssignment(com.sun.source.tree.AssignmentTree, java.lang.Void)
	 */
    @Override
    public Void visitAssignment(AssignmentTree node, Void p) {
    	super.visitAssignment(node, p);
    	
		ExpressionTree variable = node.getVariable();
		
		AnnotatedTypeMirror ownerType = null;
		
		if (TreeUtils.isFieldAccess(variable)) {
			if (variable.getKind().equals(Tree.Kind.MEMBER_SELECT)) {
				// explicit field access
				MemberSelectTree memberSelect = (MemberSelectTree) variable;
				ownerType = atypeFactory.getAnnotatedType(memberSelect.getExpression());            
			} else if (variable.getKind().equals(Tree.Kind.IDENTIFIER)) {
				// implicit field access
				// Need to know the type of the containing class.
				ownerType = visitorState.getClassType();
			}


			if (ownerType.getAnnotation(Immutable.class) != null) {
				checker.report(Result.failure("glacier.assignment"), node);
			}
		}

    	return null;
    }

    /**
     * Tests that the qualifiers present on the useType are valid qualifiers,
     * given the qualifiers on the declaration of the type, declarationType.
     * 
     * For Glacier, we don't allow qualifiers on the useType that are not redundant.
     */
    @Override
    public boolean isValidUse(AnnotatedDeclaredType declarationType,
            AnnotatedDeclaredType useType, Tree tree) {
    	
    	AnnotationMirror immutableAnnotation = AnnotationUtils.fromClass(elements, Immutable.class);
    	AnnotationMirror useAnnotation = useType.getAnnotationInHierarchy(immutableAnnotation);
  
    	return declarationType.hasAnnotation(useAnnotation);
    }
    
    protected Set<? extends AnnotationMirror> getExceptionParameterLowerBoundAnnotations() {
    	java.util.HashSet<AnnotationMirror> h = new java.util.HashSet<> (2);
    	
    	h.add(AnnotationUtils.fromClass(elements, GlacierBottom.class));
    	
        return h;
    }
    
    protected Set<? extends AnnotationMirror> getThrowUpperBoundAnnotations() {
        return atypeFactory.getQualifierHierarchy().getTopAnnotations();
    }
    
    
    protected void commonAssignmentCheck(AnnotatedTypeMirror varType,
            AnnotatedTypeMirror valueType, Tree valueTree, /*@CompilerMessageKey*/ 
    String errorKey,
            boolean isLocalVariableAssignment) {
    	// It's okay to assign from an immutable class to an variable of type Object,
    	// even if the variable is mutable.
    	
    	if ((types.directSupertypes(varType.getUnderlyingType())).size() == 0) {
    		// We're assigning to something of type Object, so its annotations don't matter.
    		// No other checks to do.
    	}
    	else {
    		super.commonAssignmentCheck(varType, valueType, valueTree, errorKey, isLocalVariableAssignment);
    	}
    	
    }

    /*
    protected boolean skipReceiverSubtypeCheck(MethodInvocationTree node,
            AnnotatedTypeMirror methodDefinitionReceiver,
            AnnotatedTypeMirror methodCallReceiver) {
    	// If the method takes Object, it can take an @Immutable object too.
    	return ((types.directSupertypes(methodDefinitionReceiver.getUnderlyingType())).size() == 0);

    }*/

    @Override
    protected void checkTypecastSafety(TypeCastTree node, Void p) {
    	// For now, do nothing. There's nothing to check that isn't already expressed by Java's type system.
    }
}
