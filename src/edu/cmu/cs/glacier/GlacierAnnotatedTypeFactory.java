package edu.cmu.cs.glacier;

import java.util.List;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.type.DeclaredType;

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
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Type.MethodType;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeInfo;

import edu.cmu.cs.glacier.qual.GlacierBottom;
import edu.cmu.cs.glacier.qual.Immutable;
import edu.cmu.cs.glacier.qual.Mutable;


public class GlacierAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {
	protected final AnnotationMirror MUTABLE, IMMUTABLE, GLACIER_BOTTOM;

	public GlacierAnnotatedTypeFactory(BaseTypeChecker checker) {
		super(checker);
		
		MUTABLE = AnnotationUtils.fromClass(elements, Mutable.class);
		IMMUTABLE = AnnotationUtils.fromClass(elements, Immutable.class);
		GLACIER_BOTTOM = AnnotationUtils.fromClass(elements, GlacierBottom.class);
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
        
        return selfType;
    }
	
	// TODO: Forbid @Immutable and @Mutable annotations on this-parameters of methods.

	@Override
	public void annotateImplicit(Tree tree, @Mutable AnnotatedTypeMirror type, boolean iUseFlow) {
		//System.out.println("tree annotateImplicit:" + tree + ", " + type);
		
		// Constructors get return types that match their own class's immutability annotation.
		if (type.getUnderlyingType() instanceof MethodType) {
			boolean methodIsConstructor = com.sun.tools.javac.tree.TreeInfo.isConstructor((JCTree)tree);
			if (methodIsConstructor) {
				AnnotatedExecutableType methodType = (AnnotatedExecutableType)type;
				AnnotatedTypeMirror returnType = methodType.getReturnType();
				
				AnnotationMirror methodMutableAnnotation = returnType.getAnnotation(Mutable.class);
				//System.out.println("methodImmutableAnnotation: " + methodImmutableAnnotation);
				
				
		        TreePath path = getPath(tree);
		        ClassTree enclosingClass = TreeUtils.enclosingClass(path);
		        
		        List<? extends AnnotationTree> classAnnotations = enclosingClass.getModifiers().getAnnotations();
		        boolean classIsImmutable = false;
		        for (AnnotationTree at : classAnnotations) {
		            Element anno = TreeInfo.symbol((JCTree) at.getAnnotationType());
		            if (anno.toString().equals(Immutable.class.getName())) {
		            	classIsImmutable = true;
		            }
		        }
				
				//System.out.println("class was declared immutable: " + classIsImmutable);
				if (classIsImmutable) {
					if (methodMutableAnnotation != null) {
						// Can't have a mutable constructor in an immutable class.
						checker.report(Result.failure("Can't have mutable constructor on immutable class"), tree);
					}
					else {
						returnType.addAnnotation(Immutable.class);
						//System.out.println("after adding annotation, methodType is: " + methodType);
						//System.out.println("after adding annotation, type is: " + type);

					}
				}
				
			}
			
		}
		else if (type.getUnderlyingType() instanceof DeclaredType) {
			DeclaredType declaredType = (DeclaredType)type.getUnderlyingType();
			//System.out.println("declared type: " + declaredType);
			
			if (tree.getKind() == Tree.Kind.CLASS && !type.hasAnnotation(Immutable.class)) {
				// Classes are mutable by default.
				type.addAnnotation(Mutable.class);
				
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
		if (!type.hasAnnotation(IMMUTABLE) && !type.hasAnnotation(MUTABLE)) {
			super.annotateImplicit(tree, type, iUseFlow);	
		}
		//System.out.println("after annotateImplicit: " + type);
		//System.out.println("");
	}
	

}
