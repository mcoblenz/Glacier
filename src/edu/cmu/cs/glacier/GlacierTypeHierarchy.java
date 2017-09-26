package edu.cmu.cs.glacier;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;

import edu.cmu.cs.glacier.qual.MaybeMutable;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.DefaultTypeHierarchy;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.javacutil.TypesUtils;

import edu.cmu.cs.glacier.qual.Immutable;

public class GlacierTypeHierarchy extends DefaultTypeHierarchy {
	
    public GlacierTypeHierarchy(final BaseTypeChecker checker, final QualifierHierarchy qualifierHierarchy,
            boolean ignoreRawTypes, boolean invariantArrayComponents) {
    	super(checker, qualifierHierarchy, ignoreRawTypes, invariantArrayComponents);
    }
    
    /**
     * Compare the primary annotations of subtype and supertype.
     * @param annosCanBeEmpty Indicates that annotations may be missing from the typemirror.
     * @return true if the primary annotation on subtype {@literal <:} primary annotation on supertype for the current top or
     * both annotations are null.  False is returned if one annotation is null and the other is not.
     */
    @Override
    protected boolean isPrimarySubtype(AnnotatedTypeMirror subtype, AnnotatedTypeMirror supertype,
                                       boolean annosCanBeEmpty) {
		if (TypesUtils.isObject(supertype.getUnderlyingType()) && supertype.hasAnnotation(MaybeMutable.class)){
			// Everything is a subtype of @MaybeMutable Object, regardless of annotations.
			return true;
		}

		if (super.isPrimarySubtype(subtype, supertype, annosCanBeEmpty)) {
			return true;
		}
		
		// The subtype might be an immutable class that implements a mutable interface.
		// If so, we'll return true even though immutable is not a subtype of mutable by itself.
		if (supertype.getUnderlyingType().getKind() == TypeKind.DECLARED && subtype.getUnderlyingType().getKind() == TypeKind.DECLARED) {
			DeclaredType declaredSupertype = (DeclaredType)(supertype.getUnderlyingType());
			Element supertypeElement = declaredSupertype.asElement();
			if (subtype.hasAnnotation(Immutable.class) && supertype.hasAnnotation(MaybeMutable.class) &&
					supertypeElement.getKind() == ElementKind.INTERFACE) {
				// Do we need to check to make sure the subtype implements the supertype's interface?
				return true;
			}
		}
		return false;
    }

}
