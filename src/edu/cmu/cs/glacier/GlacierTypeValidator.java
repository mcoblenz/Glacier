package edu.cmu.cs.glacier;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeValidator;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.javacutil.TypesUtils;

import com.sun.source.tree.Tree;

import edu.cmu.cs.glacier.qual.GlacierBottom;
import edu.cmu.cs.glacier.qual.Immutable;

public class GlacierTypeValidator extends BaseTypeValidator {

	public GlacierTypeValidator(BaseTypeChecker checker, BaseTypeVisitor<?> visitor,
			AnnotatedTypeFactory atypeFactory) {
		super(checker, visitor, atypeFactory);
	}
	
	/**
     * @return true if the effective annotations on the upperBound are above those on the lowerBound
     */
	
	@Override
    public boolean areBoundsValid(final AnnotatedTypeMirror upperBound, final AnnotatedTypeMirror lowerBound) {
		if (TypesUtils.isObject(upperBound.getUnderlyingType()) && !upperBound.hasAnnotation(Immutable.class)) {
			// If the upper bound is Object, it'll default to Mutable, in which case an Immutable lower bound is acceptable.
			return true;
		}
		return super.areBoundsValid(upperBound, lowerBound);
    }
    
    
    protected void reportValidityResult(
            final /*@CompilerMessageKey*/ String errorType,
            final AnnotatedTypeMirror type, final Tree p) {
        checker.report(Result.failure(errorType, type.getAnnotations(),
                        type.getUnderlyingType().toString()), p);
        isValid = false;
    }
    
    
    @Override
    public Void visitDeclared(AnnotatedDeclaredType type, Tree tree) {
    	if (type.hasAnnotation(GlacierBottom.class)) {
    		reportError(type, tree);
    	}
    	else {
    		return super.visitDeclared(type, tree);
    	}
		return null;
    }
    

}
