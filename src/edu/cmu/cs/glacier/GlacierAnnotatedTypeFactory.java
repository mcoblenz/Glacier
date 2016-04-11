package edu.cmu.cs.glacier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.DefaultTypeHierarchy;
import org.checkerframework.framework.type.ElementAnnotationApplier;
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

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type.ArrayType;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeInfo;

import edu.cmu.cs.glacier.qual.GlacierBottom;
import edu.cmu.cs.glacier.qual.GlacierTop;
import edu.cmu.cs.glacier.qual.Immutable;
import edu.cmu.cs.glacier.qual.Mutable;


public class GlacierAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {
	protected static boolean FLOW_BY_DEFAULT = false; // No flow needed for Glacier. Disabling this avoids inferring Top incorrectly.
	/*
	private class GlacierTreeAnnotator extends TreeScanner <Void, AnnotatedTypeMirror> {


	    public Void visitVariable(VariableTree tree, AnnotatedTypeMirror type) {
	    	if (type.getUnderlyingType().getKind() == TypeKind.DECLARED) {
	    		DeclaredType declaredType = (DeclaredType)type.getUnderlyingType();
	    		//System.out.println("declared type: " + declaredType);

	    		if (tree.getKind() == Tree.Kind.CLASS && !type.hasAnnotation(Immutable.class)) {
	    			// Classes are mutable by default.
	    			type.addAnnotation(MUTABLE);

	    			//System.out.println("annotating tree " + tree + ": " + type + " mutable because classes are mutable by default.");
	    		}
	    		else {
	    			Element classElt = declaredType.asElement();
	    			if (classElt != null) {
	    				AnnotatedTypeMirror classType = fromElement(classElt);
	    				assert classType != null : "Unexpected null type for class element: " + classElt;

	    				if (classType.hasAnnotation(IMMUTABLE)) {
	    					type.addAnnotation(IMMUTABLE);
	    				}
	    				else {
	    					type.addAnnotation(MUTABLE);
	    				}

	    			}
	    		}
	    	}	    
	    	return null;
	    }
	}
	*/
	
	protected final AnnotationMirror MUTABLE, IMMUTABLE, GLACIER_BOTTOM;

	public GlacierAnnotatedTypeFactory(BaseTypeChecker checker) {
		super(checker);
		
		MUTABLE = AnnotationUtils.fromClass(elements, Mutable.class);
		IMMUTABLE = AnnotationUtils.fromClass(elements, Immutable.class);
		GLACIER_BOTTOM = AnnotationUtils.fromClass(elements, GlacierBottom.class);
		setUseFlow(false);
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

        if (enclosingClassType.hasAnnotation(IMMUTABLE)) {
			if (selfType.hasAnnotation(MUTABLE)) {
        		checker.report(Result.failure("Can't have mutable this because the class was declared immutable"), tree);
        	}
        	selfType.addAnnotation(IMMUTABLE);
        }
        else {
        	selfType.addAnnotation(MUTABLE);
        	
			if (selfType.hasAnnotation(IMMUTABLE)) {
        		checker.report(Result.failure("Can't have immutable this because the class was declared mutable"), tree);
        	}

        }
        
        return selfType;
    }
	
	
	private static boolean isWhitelistedImmutableClass(Element element) {
		if (element.asType().toString().equals("java.lang.String")) {
			return true;
		}
		// TODO: add more classes.
		return false;
	}
	
	private static boolean isAutoboxedImmutableClass(Types types, AnnotatedTypeMirror type) {
		// Surely there is a better API for doing this than having to try/catch.
		try {
			types.unboxedType(type.getUnderlyingType());
//			System.out.println("found autoboxed immutable class: " + type);
			return true;
		}
		catch (IllegalArgumentException e) {
//			System.out.println("not an autoboxed immutable class: " + type);

			return false;
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
            	
            	if (isHardCodedImmutable) {
                	type.addAnnotation(Immutable.class);
                }
                else {
                	AnnotatedTypeMirror classType = p.fromElement(classElt);
                	assert classType != null : "Unexpected null type for class element: " + classElt;
                	// If the class type has no annotations, infer @Mutable.
                	if (!classType.hasAnnotation(Immutable.class) && !type.isAnnotatedInHierarchy(p.GLACIER_BOTTOM)) {
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
//            assert(!type.hasAnnotation(GlacierBottom.class));

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
//            assert(!type.hasAnnotation(GlacierBottom.class));
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
			if (!type.hasAnnotation(Immutable.class)) {
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
            
            
			// For now, if any bound is immutable, the whole thing has to be immutable.
			boolean foundImmutableBound = false;
			if (type.getExtendsBound().getAnnotation(Immutable.class) != null) {
				foundImmutableBound = true;
			}
			
			if (type.getSuperBound().getAnnotation(Immutable.class) != null) {
				foundImmutableBound = true;
			}
			
			
			if (foundImmutableBound) {
				type.addAnnotation(Immutable.class);
				assert(!type.hasAnnotation(Mutable.class));
			}
			else {
				type.addAnnotation(Mutable.class);
				assert(!type.hasAnnotation(Immutable.class));
			}

            return r;
        }
        
    }


}
