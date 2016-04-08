package edu.cmu.cs.glacier;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.DefaultTypeHierarchy;
import org.checkerframework.framework.type.QualifierHierarchy;

import edu.cmu.cs.glacier.qual.Immutable;
import edu.cmu.cs.glacier.qual.Mutable;

import javax.lang.model.util.Types;

public class GlacierTypeHierarchy extends DefaultTypeHierarchy {
	
    public GlacierTypeHierarchy(final BaseTypeChecker checker, final QualifierHierarchy qualifierHierarchy,
            boolean ignoreRawTypes, boolean invariantArrayComponents) {
    	super(checker, qualifierHierarchy, ignoreRawTypes, invariantArrayComponents);
    }
    
    @Override
    protected boolean isPrimarySubtype(AnnotatedTypeMirror subtype, AnnotatedTypeMirror supertype,
    		boolean annosCanBeEmpty) {
    	Types types = this.checker.getProcessingEnvironment().getTypeUtils();
    	if ((types.directSupertypes(supertype.getUnderlyingType())).size() == 0) {
    		// Everything is a subtype of Object, regardless of annotations.
    		return true;
    	}
    	else {
    		final AnnotationMirror subtypeAnno   = subtype.getAnnotationInHierarchy(currentTop);
    		final AnnotationMirror supertypeAnno = supertype.getAnnotationInHierarchy(currentTop);

    		if (isAnnoSubtype(subtypeAnno, supertypeAnno, annosCanBeEmpty)) {
    			return true;
    		}
    		
    		// The subtype might be an immutable class that implements a mutable interface.
    		// If so, we'll return true even though immutable is not a subtype of mutable by itself.
    		if (subtype.hasAnnotation(Immutable.class) && supertype.hasAnnotation(Mutable.class)) {
    			System.out.println("checking " + subtype.getUnderlyingType() + " <: " + supertype.getUnderlyingType());
    			// Check to make sure the subtype implements the supertype's interface.
    			return types.isSubtype(subtype.getUnderlyingType(), supertype.getUnderlyingType());
    		}
    		else {
    			return false;
    		}
    	}
    }

}
