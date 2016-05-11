package edu.cmu.cs.glacier;

import javax.lang.model.element.AnnotationMirror;

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
		return super.isSubtype(subtype, supertype);
	}
	@Override
	public boolean isSubtype(AnnotatedTypeMirror subtype, AnnotatedTypeMirror supertype, AnnotationMirror top) {
		if (TypesUtils.isObject(supertype.getUnderlyingType())){
			// Everything is a subtype of Object, regardless of annotations.
			return true;
		}
		return super.isSubtype(subtype, supertype, top);
	}
}
