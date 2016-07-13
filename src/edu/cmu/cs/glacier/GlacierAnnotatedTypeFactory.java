package edu.cmu.cs.glacier;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.util.Types;

import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.ElementAnnotationApplier;
import org.checkerframework.framework.type.SyntheticArrays;
import org.checkerframework.framework.type.TypeHierarchy;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedIntersectionType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedNoType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedNullType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedUnionType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.type.visitor.AnnotatedTypeScanner;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;

import edu.cmu.cs.glacier.qual.*;


public class GlacierAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {
	protected final AnnotationMirror MUTABLE, IMMUTABLE, GLACIER_BOTTOM, READ_ONLY;

	public GlacierAnnotatedTypeFactory(BaseTypeChecker checker) {
		super(checker, false); // Must disable flow analysis for correct behavior in Glacier.
		
		MUTABLE = AnnotationUtils.fromClass(elements, Mutable.class);
		IMMUTABLE = AnnotationUtils.fromClass(elements, Immutable.class);
		GLACIER_BOTTOM = AnnotationUtils.fromClass(elements, GlacierBottom.class);
		READ_ONLY = AnnotationUtils.fromClass(elements, ReadOnly.class);
		this.postInit();
	}

	/*
	 * Superclass's implementation assumes that type of "this" should be according to the annotations on the "this" parameter.
	 * But instead, for immutability, we want the annotations to be according to the containing class.
	 */
	@Override
	   public AnnotatedDeclaredType getSelfType(Tree tree) {
		AnnotatedDeclaredType selfType = super.getSelfType(tree);
		
        TreePath path = getPath(tree);
        ClassTree enclosingClass = TreeUtils.enclosingClass(path);
        if (enclosingClass == null) {
            // I hope this only happens when tree is a fake tree that
            // we created, e.g. when desugaring enhanced-for-loops.
            enclosingClass = getCurrentClassTree(tree);
        }
        AnnotatedDeclaredType enclosingClassType = getAnnotatedType(enclosingClass);

        if (!selfType.isAnnotatedInHierarchy(READ_ONLY)) { // If there's already an annotation on selfType and it conflicts with Mutable, that error will be found by the type validity check elsewhere.
        	if (enclosingClassType.isAnnotatedInHierarchy(READ_ONLY)) {
        		annotateInheritedFromClass(selfType, enclosingClassType.getAnnotations());
        	}
        	else {
        		selfType.addAnnotation(MUTABLE);
        	}
        }
        return selfType;
    }
	
	
	private static boolean isWhitelistedImmutableClass(Element element) {
		if (element.asType().toString().equals("java.lang.String") ||
			element.asType().toString().equals("java.lang.Number")) {
			return true;
		}
		// TODO: add more classes.
		return false;
	}
	
	private static boolean isAutoboxedImmutableClass(Types types, AnnotatedTypeMirror type) {
		// Surely there is a better API for doing this than having to try/catch.
		try {
			types.unboxedType(type.getUnderlyingType());
			return true;
		}
		catch (IllegalArgumentException e) {
			return false;
		}
	}

    /**
     * A callback method for the AnnotatedTypeFactory subtypes to customize
     * AnnotatedTypes.asMemberOf().  Overriding methods should merely change
     * the annotations on the subtypes, without changing the types.
     *
     * In this override, we make the receiver's annotations match the return type's annotations, which have already been set up correctly.
     *
     * @param type  the annotated type of the element
     * @param owner the annotated type of the receiver of the accessing tree
     * @param element   the element of the field or method
     */
    @Override
    public void postAsMemberOf(AnnotatedTypeMirror type,
                               AnnotatedTypeMirror owner, Element element) {
        super.postAsMemberOf(type, owner, element);
        if (SyntheticArrays.isArrayClone(owner, element)) {
            // The superclass has already made the return type be the type of the owner. But
            // this isn't quite what we want because the return type should be annotated GlacierBottom, not whatever the owner is.


            // clone() should be:
            // @A Object @GlacierBottom [] clone(@ReadOnly Array this)

            AnnotatedExecutableType executableType = (AnnotatedExecutableType)type;

            AnnotatedTypeMirror receiverType = executableType.getReceiverType();
            receiverType.removeAnnotationInHierarchy(READ_ONLY);
            receiverType.addAnnotation(READ_ONLY);

            AnnotatedArrayType arrayReturnType = (AnnotatedArrayType)executableType.getReturnType();
            arrayReturnType.removeAnnotationInHierarchy(READ_ONLY);
            arrayReturnType.addAnnotation(GLACIER_BOTTOM);
        }
    }


    // TODO: Forbid @Immutable and @Mutable annotations on this-parameters of methods.
	
    protected void annotateInheritedFromClass(/*@Mutable*/ AnnotatedTypeMirror type) {
    	GlacierInheritedFromClassAnnotator.INSTANCE.visit(type, this);
    }
    
    /**
     * Callback to determine what to do with the annotations from a class declaration.
     * 
     * Ugh. This should not be here, but is due to visibility limitations.
     */
    protected void annotateInheritedFromClass(/*@Mutable*/ AnnotatedTypeMirror type,
            Set<AnnotationMirror> fromClass) {
        type.addMissingAnnotations(fromClass);
    }
    

    @Override
    protected TypeHierarchy createTypeHierarchy() {
        return new GlacierTypeHierarchy(checker, getQualifierHierarchy(),
                                        checker.hasOption("ignoreRawTypeArguments"),
                                        checker.hasOption("invariantArrays"));
    }
    
    /**
     * A singleton utility class for pulling annotations down from a class
     * type.
     *
     * HACK HACK HACK: It would be preferable to inherit from InheritedFromClassAnnotator, but that class has a private constructor.
     *
     * @see #annotateInheritedFromClass
     */
    protected static class GlacierInheritedFromClassAnnotator
            extends AnnotatedTypeScanner<Void, GlacierAnnotatedTypeFactory> {

        /** The singleton instance. */
        public static final GlacierInheritedFromClassAnnotator INSTANCE
            = new GlacierInheritedFromClassAnnotator();

        
        private GlacierInheritedFromClassAnnotator() {}

        @Override
        public Void visitExecutable(AnnotatedExecutableType type, GlacierAnnotatedTypeFactory p) {

        	// KEY DIFFERENCE VS. SUPERCLASS: Visit the receiver too!
        	scanAndReduce(type.getReceiverType(), p, null);

        	// ANOTHER KEY DIFFERENCE VS. SUPERCLASS: visit
            // constructor return types (which somewhat act like
            // the receiver).
            scan(type.getReturnType(), p);
            

            scanAndReduce(type.getParameterTypes(), p, null);
            scanAndReduce(type.getThrownTypes(), p, null);
            scanAndReduce(type.getTypeVariables(), p, null);
            return null;
        }

        @Override
        public Void visitDeclared(AnnotatedDeclaredType type, GlacierAnnotatedTypeFactory p) {
            Element classElt = type.getUnderlyingType().asElement();
//            assert(!type.hasAnnotation(GlacierBottom.class));
            

            // Only add annotations from the class declaration if there
            // are no annotations from that hierarchy already on the type.

            if (classElt != null) {
            	boolean isHardCodedImmutable = isAutoboxedImmutableClass(p.types, type) || 
            			isWhitelistedImmutableClass(classElt);
            	
            	if (isHardCodedImmutable && !type.isAnnotatedInHierarchy(p.READ_ONLY)) {
                	type.addAnnotation(Immutable.class);
                }
                else {
                	AnnotatedTypeMirror classType = p.fromElement(classElt);
                	assert classType != null : "Unexpected null type for class element: " + classElt;
                	// If the class type has no annotations, infer @Mutable.
                	if (!classType.isAnnotatedInHierarchy(p.READ_ONLY) && !type.isAnnotatedInHierarchy(p.READ_ONLY)) {
                		type.addAnnotation(Mutable.class);
                	}
                	else {
                		p.annotateInheritedFromClass(type, classType.getAnnotations());
                	}
                }
            }
//            assert(!type.hasAnnotation(GlacierBottom.class));
            
//            System.out.println("visitDeclared " + type);
            return super.visitDeclared(type, p);
        }
        

        private final Map<TypeParameterElement, AnnotatedTypeVariable> visited =
                new HashMap<TypeParameterElement, AnnotatedTypeVariable>();

        @Override
        public Void visitTypeVariable(AnnotatedTypeVariable type, GlacierAnnotatedTypeFactory p) {
            TypeParameterElement tpelt = (TypeParameterElement) type.getUnderlyingType().asElement();
            if (!visited.containsKey(tpelt)) {
                visited.put(tpelt, type);
                if (type.getAnnotations().isEmpty() &&
                        type.getUpperBound().getAnnotations().isEmpty() &&
                        tpelt.getEnclosingElement().getKind() != ElementKind.TYPE_PARAMETER) {
                    ElementAnnotationApplier.apply(type, tpelt, p);
                }
                
                super.visitTypeVariable(type, p);
                
                
                visited.remove(tpelt);
            }

            return null;
        }

        @Override
        public void reset() {
            visited.clear();
            super.reset();
        }


        @Override
        public Void visitIntersection(AnnotatedIntersectionType type, GlacierAnnotatedTypeFactory p) {
            if (visitedNodes.containsKey(type)) {
                return visitedNodes.get(type);
            }
            visitedNodes.put(type, null);
            Void r = scan(type.directSuperTypes(), p);
            return r;
        }

        @Override
        public Void visitUnion(AnnotatedUnionType type, GlacierAnnotatedTypeFactory p) {
            if (visitedNodes.containsKey(type)) {
                return visitedNodes.get(type);
            }
            visitedNodes.put(type, null);
            Void r = scan(type.getAlternatives(), p);
            return r;
        }

        @Override
        public Void visitArray(AnnotatedArrayType type, GlacierAnnotatedTypeFactory p) {
			// Arrays default to mutable.
			if (!type.isAnnotatedInHierarchy(p.READ_ONLY)) {
				type.addAnnotation(Mutable.class);
			}
			        	
        	Void r = scan(type.getComponentType(), p);
            return r;
        }

        @Override
        public Void visitNoType(AnnotatedNoType type, GlacierAnnotatedTypeFactory p) {
            return null;
        }

        @Override
        public Void visitNull(AnnotatedNullType type, GlacierAnnotatedTypeFactory p) {
            return null;
        }

        @Override
        public Void visitPrimitive(AnnotatedPrimitiveType type, GlacierAnnotatedTypeFactory p) {
        	// All primitives are immutable.
        	if (!type.isAnnotatedInHierarchy(p.READ_ONLY)) {
        		type.addAnnotation(Immutable.class);
        	}
            return null;
        }

        @Override
        public Void visitWildcard(AnnotatedWildcardType type, GlacierAnnotatedTypeFactory p) {
            if (visitedNodes.containsKey(type)) {
                return visitedNodes.get(type);
            }
            visitedNodes.put(type, null);
            Void r = scan(type.getExtendsBound(), p);
            visitedNodes.put(type, r);
            r = scanAndReduce(type.getSuperBound(), p, r);
            visitedNodes.put(type, r);
            return r;
        }
        
    }


}
