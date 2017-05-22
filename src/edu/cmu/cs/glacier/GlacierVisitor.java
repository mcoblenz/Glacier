package edu.cmu.cs.glacier;

/*>>>
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
*/

import java.lang.annotation.Annotation;
import java.util.*;

import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;

import com.sun.source.tree.*;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeParameterBounds;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.TypeHierarchy;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.util.ContractsUtils;
import org.checkerframework.framework.util.ContractsUtils.Postcondition;
import org.checkerframework.framework.util.ContractsUtils.ConditionalPostcondition;
import org.checkerframework.framework.util.ContractsUtils.Contract;
import org.checkerframework.framework.util.ContractsUtils.Precondition;
import org.checkerframework.framework.util.FlowExpressionParseUtil;
import org.checkerframework.framework.util.FlowExpressionParseUtil.FlowExpressionContext;
import org.checkerframework.framework.util.FlowExpressionParseUtil.FlowExpressionParseException;
import org.checkerframework.javacutil.*;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeValidator;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.util.PurityUtils;
import org.checkerframework.framework.source.Result;

import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.tree.JCTree;

import edu.cmu.cs.glacier.qual.*;

public class GlacierVisitor extends BaseTypeVisitor<GlacierAnnotatedTypeFactory> {

	public GlacierVisitor(BaseTypeChecker checker) {
		super(checker);
	}
	
    @Override
    protected BaseTypeValidator createTypeValidator() {
        return new GlacierTypeValidator(checker, this, atypeFactory);
    }
	
	private static boolean modifiersIncludeModifier(ModifiersTree modifiers, Class<? extends Annotation> modifier) {
		boolean foundImmutable = false;
		
		List <? extends AnnotationTree> annotations = modifiers.getAnnotations();
		for (AnnotationMirror a : InternalUtils.annotationsFromTypeAnnotationTrees(annotations)) {
	        if (AnnotationUtils.areSameByClass(a,modifier)) {
	        	foundImmutable = true;
	        }
		}
		
		return foundImmutable;
	}

    private boolean typeIsImmutable(AnnotatedTypeMirror type) {
        TypeMirror underlyingType = type.getUnderlyingType();

        boolean fieldIsImmutable = type.hasAnnotation(Immutable.class);
        if (!fieldIsImmutable) {
            // Check for permitted special cases: primitives and type parameters.
            boolean typeIsPrimitive = underlyingType instanceof PrimitiveType;
            boolean typeIsImmutableClassTypeParameter = false;
            if (underlyingType.getKind() == TypeKind.TYPEVAR) {
                // Type variables can be assumed to be immutable if they are on an immutable class. Otherwise they are unsafe.
                TypeVariable typeVariable = (TypeVariable) underlyingType;
                TypeParameterElement typeVariableElement = (TypeParameterElement) (typeVariable.asElement());
                Element elementEnclosingTypeVariable = typeVariableElement.getGenericElement();
                if (elementEnclosingTypeVariable.getKind() == ElementKind.CLASS || elementEnclosingTypeVariable.getKind() == ElementKind.ENUM) {
                    // Check to see if this class is immutable.
                    TypeElement classElement = (TypeElement) elementEnclosingTypeVariable;
                    AnnotatedTypeMirror classTypeMirror = atypeFactory.getAnnotatedType(classElement);
                    if (classTypeMirror.hasAnnotation(Immutable.class)) {
                        typeIsImmutableClassTypeParameter = true;
                    }
                }
            }

            if (!typeIsPrimitive && !typeIsImmutableClassTypeParameter) {
                return false;
            }
        }

        if (underlyingType.getKind() == TypeKind.ARRAY) {
            // Arrays must be immutable arrays of immmutable objects.
            AnnotatedArrayType arrayType = (AnnotatedArrayType) type;
            AnnotatedTypeMirror componentType = arrayType.getComponentType();
            return typeIsImmutable(componentType);
        }

        return true;
    }

    private void checkFieldIsImmutable(ClassTree deepestClassTree, Tree containingTree, TypeElement immediateContainingElement, Element element, boolean alsoCheckFinal) {
        AnnotatedTypeMirror fieldType = atypeFactory.getAnnotatedType(element);
        if (!typeIsImmutable(fieldType)) {
            if (alsoCheckFinal) {
                checker.report(Result.failure("glacier.mutablemember", deepestClassTree.getSimpleName(), immediateContainingElement, element), deepestClassTree);
            } else {
                if (fieldType.getUnderlyingType().getKind() == TypeKind.ARRAY) {
                    AnnotatedArrayType arrayType = (AnnotatedArrayType)fieldType;
                    if (!arrayType.hasAnnotation(Immutable.class)) {
                        checker.report(Result.failure("glacier.mutable.wholearray.invalid"), element);
                    }
                    else {
                        checker.report(Result.failure("glacier.mutable.array.invalid"), element);
                    }
                }
                else {
                    checker.report(Result.failure("glacier.mutable.invalid"), element);
                }
            }
        }

        if (alsoCheckFinal) {
            if (!ElementUtils.isFinal(element)) {
                checker.report(Result.failure("glacier.nonfinalmember", deepestClassTree.getSimpleName(), immediateContainingElement, element), deepestClassTree);
            }
        }
    }

    private void checkElementMembersAreImmutable(ClassTree outermostTree, Tree containingTree, Element elem, boolean alsoCheckFinal) {
        if (elem.getKind() == ElementKind.CLASS || elem.getKind() == ElementKind.ENUM) {
            TypeElement typeElement = (TypeElement)elem;
            List<? extends Element> elements = typeElement.getEnclosedElements();


            for (Element e : elements) {
                if (e.getKind() == ElementKind.FIELD) {
                    // Check to make sure this field is immutable and final.
                    checkFieldIsImmutable(outermostTree, containingTree, typeElement, e, alsoCheckFinal);
                }
            }

            TypeMirror superclassType = typeElement.getSuperclass();
            if (superclassType != null && superclassType.getKind() == TypeKind.DECLARED) {
                DeclaredType superclassDeclaredType = (DeclaredType)superclassType;
                Element superclassElement = superclassDeclaredType.asElement();
                checkElementMembersAreImmutable(outermostTree, containingTree, superclassElement, alsoCheckFinal);
            }
        }
    }

    private void checkAllClassMembersAreImmutable(ClassTree outermostTree, Tree containingTree, boolean alsoCheckFinal) {
        if (containingTree instanceof ExpressionTree) {
            Element elem = TreeUtils.elementFromUse((ExpressionTree)containingTree);
            checkElementMembersAreImmutable(outermostTree, containingTree, elem, alsoCheckFinal);
        }
    }



    private void checkImmediateMembersAreImmutable(ClassTree classTree, boolean alsoCheckFinal) {
        TypeElement classTreeAsElement = TreeUtils.elementFromDeclaration(classTree);
        List <? extends Tree> members = classTree.getMembers();
        for (Tree t : members) {
            if (t.getKind() == Kind.VARIABLE) {
                checkFieldIsImmutable(classTree, classTree, classTreeAsElement, TreeUtils.elementFromDeclaration((VariableTree) t), false);
            }
        }
    }


    /**
     * Type-check classTree. Subclasses should override this method instead of {@link
     * #visitClass(ClassTree, Void)}.
     *
     * @param classTree class to check
     */
    @Override
    public void processClassTree(ClassTree classTree) {
        boolean classIsImmutable = modifiersIncludeModifier(classTree.getModifiers(), Immutable.class);
        boolean classIsReadonly = modifiersIncludeModifier(classTree.getModifiers(), ReadOnly.class);

        if (classIsReadonly) {
            checker.report(Result.failure("glacier.readonly.class"), classTree);
        }

        if (classIsImmutable) {
            // Check to make sure all fields are immutable.
            checkImmediateMembersAreImmutable(classTree, false);
        }

        Tree superclass = classTree.getExtendsClause();
        if (superclass != null) {
            AnnotatedTypeMirror superclassType = atypeFactory.getAnnotatedType(superclass);
            if (superclassType.hasAnnotation(Immutable.class) && !classIsImmutable) {
                checker.report(Result.failure("glacier.subclass.mutable", classTree.getSimpleName(), superclass.toString()), classTree);
            }
            else if (classIsImmutable && !superclassType.hasAnnotation(Immutable.class)) {
                // Check members of all superclasses to make sure they're all immutable and final.
                checkAllClassMembersAreImmutable(classTree, superclass, true);
            }
        }


        // Check to make sure that if any implemented interface is immutable, the class is immutable.
        List<? extends Tree> interfaces = classTree.getImplementsClause();
        for (Tree implementedInterface : interfaces) {
            AnnotatedTypeMirror interfaceType = atypeFactory.getAnnotatedType(implementedInterface);

            if (interfaceType.hasAnnotation(Immutable.class)) {
                if (!classIsImmutable) {
                    checker.report(Result.failure("glacier.interface.immutable", classTree.getSimpleName(), implementedInterface), classTree);
                }
                break;
            }
        }

        super.processClassTree(classTree);
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
		
		if (TreeUtils.isFieldAccess(variable)) {
			AnnotatedTypeMirror ownerType = null;

			if (variable.getKind().equals(Tree.Kind.MEMBER_SELECT)) {
				// explicit field access
				MemberSelectTree memberSelect = (MemberSelectTree) variable;
				ownerType = atypeFactory.getAnnotatedType(memberSelect.getExpression());            
			} else if (variable.getKind().equals(Tree.Kind.IDENTIFIER)) {
				// implicit field access
				// Need to know the type of the containing class.
				ownerType = visitorState.getClassType();
			}

			// Assignment to fields of immutable classes is allowed during their constructors and outside methods.
			MethodTree methodTree = visitorState.getMethodTree();
			if (methodTree != null) {
				boolean methodIsConstructor = TreeUtils.isConstructor(methodTree);

				AnnotatedDeclaredType classType = visitorState.getClassType();

                // Even in constructors, you can't assign to fields of OTHER classes.
				boolean classOwnsAssignedField = ownerType.getUnderlyingType().equals(classType.getUnderlyingType()); 
				AnnotationMirror ownerAnnotationMirror = ownerType.getAnnotationInHierarchy(atypeFactory.READ_ONLY);

                Element fieldElement = TreeUtils.elementFromUse(variable);
                boolean fieldIsStatic = ElementUtils.isStatic(fieldElement);

				if ((!methodIsConstructor || !classOwnsAssignedField || fieldIsStatic) &&
					!atypeFactory.getQualifierHierarchy().isSubtype(ownerAnnotationMirror, atypeFactory.MAYBE_MUTABLE)) {
					checker.report(Result.failure("glacier.assignment"), node);
				}
			}
		}
		else if (variable.getKind() == Tree.Kind.ARRAY_ACCESS) {
			ArrayAccessTree arrayAccessTree = (ArrayAccessTree)variable;
			
			AnnotatedTypeMirror arrayType = atypeFactory.getAnnotatedType(arrayAccessTree.getExpression());
			AnnotationMirror arrayTypeAnnotation = arrayType.getAnnotationInHierarchy(atypeFactory.READ_ONLY);
			if (!atypeFactory.getQualifierHierarchy().isSubtype(arrayTypeAnnotation, atypeFactory.MAYBE_MUTABLE)) {
				checker.report(Result.failure("glacier.assignment.array"), node);
			}
		}

    	return null;
    }

    /**
     * Tests that the qualifiers present on the useType are valid qualifiers,
     * given the qualifiers on the declaration of the type, declarationType.
     * 
     * For Glacier, we don't allow qualifiers on the useType that are not redundant -- except that for Object, anything goes.
     */
    @Override
    public boolean isValidUse(AnnotatedDeclaredType declarationType,
            AnnotatedDeclaredType useType, Tree tree) {
    	// Users can't specify bottom annotations.
    	if (useType.hasAnnotation(GlacierBottom.class)) {
    		return false;
    	}
    	
        if(TypesUtils.isObject(declarationType.getUnderlyingType())) {
            // If the declared type is Object, the use can have any Glacier annotation.
            return true;
        }

        // We need to make sure that users never apply annotations that conflict with the ones in the declarations. 
        // Check that directly rather than calling super.isValidUse().
        AnnotationMirror immutableAnnotation = AnnotationUtils.fromClass(elements, Immutable.class);
        AnnotationMirror useAnnotation = useType.getAnnotationInHierarchy(immutableAnnotation);

        if (useAnnotation != null) {
        	return declarationType.hasAnnotation(useAnnotation);
        }
        else {
        	return !declarationType.hasAnnotation(useAnnotation);
        }
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
    String errorKey) {
    	// It's okay to assign from an immutable class to an variable of type Object,
    	// even if the variable is mutable.
    	
    	if (TypesUtils.isObject(varType.getUnderlyingType()) && varType.hasAnnotation(MaybeMutable.class)) {
    		// We're assigning to something of type @MaybeMutable Object, so its annotations don't matter.
    		// No other checks to do.
    	}
    	else {
              super.commonAssignmentCheck(varType, valueType, valueTree, errorKey);
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

    // Type arguments to an immutable class must be immutable because those type arguments may be used on fields.
    // Type arguments on methods don't need any special checking.
    protected void checkTypeArguments(Tree toptree, List<? extends AnnotatedTypeParameterBounds> paramBounds, List<? extends AnnotatedTypeMirror> typeargs, List<? extends Tree> typeargTrees) {
        super.checkTypeArguments(toptree, paramBounds, typeargs, typeargTrees);

        AnnotatedTypeMirror toptreeType = atypeFactory.getAnnotatedType(toptree);

        if (toptreeType.hasAnnotation(Immutable.class) && (toptree.getKind() == Kind.CLASS || toptree.getKind() == Kind.PARAMETERIZED_TYPE)) {
            // Cases for toptree: ParameterizedTypeTree; MethodInvocationTree; NewClassTree.

            // Make sure all the type arguments have the @Immutable annotation.
            for (AnnotatedTypeMirror typearg : typeargs) {
                // Ignore type variables because we only want to check concrete types.
                if (typearg.getKind() != TypeKind.TYPEVAR && !typearg.hasAnnotation(Immutable.class)) {
                    // One last-ditch check: maybe typearg is a wildcard. If so, it suffices if the upper bound is immutable or a type variable.
                    boolean reportError = false;

                    if (typearg.getKind() == TypeKind.WILDCARD) {
                        AnnotatedTypeMirror.AnnotatedWildcardType annotatedWildcardType = (AnnotatedTypeMirror.AnnotatedWildcardType) typearg;
                        AnnotatedTypeMirror extendsBound = annotatedWildcardType.getExtendsBound();
                        if (extendsBound.getKind() != TypeKind.TYPEVAR && !extendsBound.hasAnnotation(Immutable.class)) {
                            reportError = true;
                        }
                    }
                    else {
                        reportError = true;
                    }

                    if (reportError) {

                        checker.report(Result.failure("glacier.typeparameter.mutable", toptree, typearg), toptree);

                    }
                }
            }
        }
    }

    /**
     * Indicates whether to skip subtype checks on the receiver when
     * checking method invocability. A visitor may, for example,
     * allow a method to be invoked even if the receivers are siblings
     * in a hierarchy, provided that some other condition (implemented
     * by the visitor) is satisfied.
     *
     * @param node                        the method invocation node
     * @param methodDefinitionReceiver    the ATM of the receiver of the method definition
     * @param methodCallReceiver          the ATM of the receiver of the method call
     *
     * @return whether to skip subtype checks on the receiver
     */
    @Override
    protected boolean skipReceiverSubtypeCheck(MethodInvocationTree node,
            AnnotatedTypeMirror methodDefinitionReceiver,
            AnnotatedTypeMirror methodCallReceiver) {
    	
    	TypeMirror definitionType = methodDefinitionReceiver.getUnderlyingType();
    	
    	// It's okay to invoke methods that are defined on java.lang.Object or on java.lang.Enum.
    	if (TypesUtils.isObject(definitionType)) {
    		return true;
    	}
    	else if (definitionType instanceof DeclaredType) {
    		DeclaredType declaredDefinitionType = (DeclaredType)definitionType;
    		return TypesUtils.getQualifiedName(declaredDefinitionType).contentEquals("java.lang.Enum");
    	}
    	return false;
    			
    }

    @Override
    protected OverrideChecker createOverrideChecker(Tree overriderTree,
                                                  AnnotatedExecutableType overrider,
                                                  AnnotatedTypeMirror overridingType,
                                                  AnnotatedTypeMirror overridingReturnType,
                                                  AnnotatedExecutableType overridden,
                                                  AnnotatedDeclaredType overriddenType,
                                                  AnnotatedTypeMirror overriddenReturnType) {
        return new GlacierOverrideChecker(overriderTree,
                overrider,
                overridingType,
                overridingReturnType,
                overridden,
                overriddenType,
                overriddenReturnType);
    }



    /**
     * Class to perform method override and method reference checks.
     *
     * Method references are checked similarly to method overrides, with the
     * method reference viewed as overriding the functional interface's method.
     *
     * Checks that an overriding method's return type, parameter types, and
     * receiver type are correct with respect to the annotations on the
     * overridden method's return type, parameter types, and receiver type.
     *
     * <p>
     * Furthermore, any contracts on the method must satisfy behavioral
     * subtyping, that is, postconditions must be at least as strong as the
     * postcondition on the superclass, and preconditions must be at most as
     * strong as the condition on the superclass.
     *
     * <p>
     * This method returns the result of the check, but also emits error
     * messages as a side effect.
     */
    private class GlacierOverrideChecker extends OverrideChecker {
        GlacierOverrideChecker(
                Tree overriderTree,
                AnnotatedExecutableType overrider,
                AnnotatedTypeMirror overridingType,
                AnnotatedTypeMirror overridingReturnType,
                AnnotatedExecutableType overridden,
                AnnotatedDeclaredType overriddenType,
                AnnotatedTypeMirror overriddenReturnType) {
            super(overriderTree,
                    overrider,
                    overridingType,
                    overridingReturnType,
                    overridden,
                    overriddenType,
                    overriddenReturnType);
        }

        @Override
        protected boolean checkReceiverOverride() {
            /*
             ** Don't do anything here because in Glacier, the only way for the receiver's annotation to be invalid is if it is also an invalid type.
            */
            return true;
        }
    }
}
