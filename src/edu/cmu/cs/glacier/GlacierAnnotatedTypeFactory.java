package edu.cmu.cs.glacier;

import java.lang.reflect.AnnotatedArrayType;
import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Type.AnnotatedType;
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
        	selfType.removeAnnotation(MUTABLE);
        	selfType.addAnnotation(IMMUTABLE);
        }
        else {
        	selfType.addAnnotation(MUTABLE);
        	selfType.removeAnnotation(IMMUTABLE);
        }
        
        return selfType;
    }
	
	// TODO: Forbid @Immutable and @Mutable annotations on this-parameters of methods.

	/*
	 * Returns the declared annotation, if any, on the class containing this tree.
	 */
	private AnnotationMirror declaredGlacierAnnotation(Tree tree) {
		TreePath path = getPath(tree);
        ClassTree enclosingClass = TreeUtils.enclosingClass(path);
        
        List<? extends AnnotationTree> classAnnotations = enclosingClass.getModifiers().getAnnotations();
        boolean classIsImmutable = false;
        boolean classIsMutable = false;
        for (AnnotationTree at : classAnnotations) {
            Element anno = TreeInfo.symbol((JCTree) at.getAnnotationType());
            if (anno.toString().equals(Immutable.class.getName())) {
            	classIsImmutable = true;
            }
            else if (anno.toString().equals(Mutable.class.getName())) {
            	classIsMutable = true;
            }
        }
        assert(!(classIsMutable && classIsImmutable));
        
        if (classIsImmutable) {
        	return IMMUTABLE;
        }
        else if (classIsMutable) {
        	return MUTABLE;
        }
        else {
        	return null;
        }
	}
	
	// Modifies the input type.
	private void inferAnnotationsForType(Tree tree, AnnotatedTypeMirror type) {
		switch(type.getUnderlyingType().getKind()) {
		case ARRAY:
			// Arrays default to mutable.
			if (!type.hasAnnotation(IMMUTABLE)) {
				type.addAnnotation(MUTABLE);
			}
			
			AnnotatedTypeMirror.AnnotatedArrayType annotatedArrayType = (AnnotatedTypeMirror.AnnotatedArrayType)type;
			AnnotatedTypeMirror annotatedComponentType = annotatedArrayType.getComponentType();
			
			inferAnnotationsForType(tree, annotatedComponentType);
			break;
		case BOOLEAN:
			type.addAnnotation(IMMUTABLE);
			break;
		case BYTE:
			type.addAnnotation(IMMUTABLE);
			break;
		case CHAR:
			type.addAnnotation(IMMUTABLE);
			break;
		case DECLARED:
			DeclaredType declaredType = (DeclaredType)type.getUnderlyingType();
			//System.out.println("declared type: " + declaredType);
			
			if (tree.getKind() == Tree.Kind.CLASS && !type.hasAnnotation(Immutable.class)) {
				// Classes are mutable by default.
				type.addAnnotation(MUTABLE);
				
				//System.out.println("annotating tree " + tree + ": " + type + " mutable because classes are mutable by default.");
			}
			else {
				//System.out.println("annotating declared type: " + declaredType + " on tree " + tree);
				// Look up the original declaration of this class and find out its annotation.
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
				else {
					System.out.println("danger: not annotating declaredType " + declaredType);
				}
			}
			
			AnnotatedDeclaredType annotatedDeclaredType = (AnnotatedDeclaredType)type;
			List<? extends AnnotatedTypeMirror> typeArguments = annotatedDeclaredType.getTypeArguments();
			for (AnnotatedTypeMirror typeArg : typeArguments) {
				inferAnnotationsForType(tree, typeArg);
			}
			break;
		case DOUBLE:
			type.addAnnotation(IMMUTABLE);
			break;
		case ERROR:
			// Nothing to do here.
			break;
		case EXECUTABLE:
			AnnotatedExecutableType methodType = (AnnotatedExecutableType)type;
			MethodTree methodTree = (MethodTree)tree;
			List<? extends VariableTree> methodParameters = methodTree.getParameters();
			
			boolean methodIsConstructor = com.sun.tools.javac.tree.TreeInfo.isConstructor((JCTree)tree);
			if (methodIsConstructor) {
				AnnotatedTypeMirror returnType = methodType.getReturnType();
				
				AnnotationMirror methodMutableAnnotation = returnType.getAnnotation(Mutable.class);
				//System.out.println("methodImmutableAnnotation: " + methodImmutableAnnotation);
				
				AnnotationMirror treeAnnotation = declaredGlacierAnnotation(tree);
				
				//System.out.println("class was declared immutable: " + classIsImmutable);
				if (treeAnnotation != null && treeAnnotation.getAnnotationType().getAnnotation(Immutable.class) != null) {
					if (methodMutableAnnotation != null) {
						// Can't have a mutable constructor in an immutable class.
						checker.report(Result.failure("Can't have mutable constructor on immutable class"), tree);
					}
					else {
						returnType.addAnnotation(IMMUTABLE);
						//System.out.println("after adding annotation, methodType is: " + methodType);
						//System.out.println("after adding annotation, type is: " + type);

					}
				}
				else {
					returnType.addAnnotation(MUTABLE);
				}
			}
			else {
				inferAnnotationsForType(methodTree, methodType.getReturnType());
			}
			
			// Infer annotations on parameters.
			List<AnnotatedTypeMirror> parameterTypes = methodType.getParameterTypes();
			for (int i = 0; i < parameterTypes.size(); i++) {
				AnnotatedTypeMirror parameterType = parameterTypes.get(i);
				VariableTree parameter = methodParameters.get(i);
				inferAnnotationsForType(parameter, parameterType);
			}

			break;
		case FLOAT:
			type.addAnnotation(IMMUTABLE);
			break;
		case INT:
			type.addAnnotation(IMMUTABLE);
			break;
		case INTERSECTION:
			// TODO
			break;
		case LONG:
			type.addAnnotation(IMMUTABLE);
			break;
		case NONE:
			// Shouldn't happen?
			break;
		case NULL:
			type.addAnnotation(GlacierBottom.class);
			break;
		case OTHER:
			// Shouldn't happen.
			break;
		case PACKAGE:
			// Shouldn't happen.
			break;
		case SHORT:
			type.addAnnotation(IMMUTABLE);
			break;
		case TYPEVAR:
			// TODO
			break;
		case UNION:
			// TODO
			break;
		case VOID:
			// Nothing to do.
			break;
		case WILDCARD:
			// TODO
			break;
		default:
			break;
		}
	}
	
	@Override
	public void annotateImplicit(Tree tree, @Mutable AnnotatedTypeMirror type, boolean iUseFlow) {
		//System.out.println("tree annotateImplicit:" + tree + ", " + type);
		
		inferAnnotationsForType(tree, type);
		
		//System.out.println("after tree annotateImplicit:" + tree + ", " + type);
	}
	

}
