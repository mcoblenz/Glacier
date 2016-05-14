package edu.cmu.cs.glacier;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.DefaultTypeHierarchy;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.javacutil.TypesUtils;

import edu.cmu.cs.glacier.qual.Immutable;
import edu.cmu.cs.glacier.qual.Mutable;

import javax.lang.model.util.Types;

public class GlacierTypeHierarchy extends DefaultTypeHierarchy {
	
    public GlacierTypeHierarchy(final BaseTypeChecker checker, final QualifierHierarchy qualifierHierarchy,
            boolean ignoreRawTypes, boolean invariantArrayComponents) {
    	super(checker, qualifierHierarchy, ignoreRawTypes, invariantArrayComponents, true);
    }


    @Override
	public boolean isSubtype(AnnotatedTypeMirror subtype, AnnotatedTypeMirror supertype) {    	
		if (TypesUtils.isObject(supertype.getUnderlyingType())){
			// Everything is a subtype of Object, regardless of annotations.
			return true;
		}
		if (super.isSubtype(subtype, supertype)) {
			return true;
		}
		
		// The subtype might be an immutable class that implements a mutable interface.
		// If so, we'll return true even though immutable is not a subtype of mutable by itself.
		if (supertype.getUnderlyingType().getKind() == TypeKind.DECLARED && subtype.getUnderlyingType().getKind() == TypeKind.DECLARED) {
			DeclaredType declaredSupertype = (DeclaredType)(supertype.getUnderlyingType());
			Element supertypeElement = declaredSupertype.asElement();
			if (subtype.hasAnnotation(Immutable.class) && supertype.hasAnnotation(Mutable.class) &&
					supertypeElement.getKind() == ElementKind.INTERFACE) {
				// Do we need to check to make sure the subtype implements the supertype's interface?
				return true;
			}
		}
		return false;

	}
	@Override
	public boolean isSubtype(AnnotatedTypeMirror subtype, AnnotatedTypeMirror supertype, AnnotationMirror top) {
		if (TypesUtils.isObject(supertype.getUnderlyingType())){
			// Everything is a subtype of Object, regardless of annotations.
			return true;
		}
		if (super.isSubtype(subtype, supertype, top)) {
			return true;
		}
				
		// The subtype might be an immutable class that implements a mutable interface.
		// If so, we'll return true even though immutable is not a subtype of mutable by itself.
		if (supertype.getUnderlyingType().getKind() == TypeKind.DECLARED && subtype.getUnderlyingType().getKind() == TypeKind.DECLARED) {
			DeclaredType declaredSupertype = (DeclaredType)(supertype.getUnderlyingType());
			Element supertypeElement = declaredSupertype.asElement();
			if (subtype.hasAnnotation(Immutable.class) && supertype.hasAnnotation(Mutable.class) &&
					supertypeElement.getKind() == ElementKind.INTERFACE) {
				// Do we need to check to make sure the subtype implements the supertype's interface?
				return true;
			}
		}
		return false;	
		}
}
