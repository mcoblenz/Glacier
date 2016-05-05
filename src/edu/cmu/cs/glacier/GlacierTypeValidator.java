package edu.cmu.cs.glacier;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeValidator;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;

import com.sun.source.tree.Tree;

public class GlacierTypeValidator extends BaseTypeValidator {

	public GlacierTypeValidator(BaseTypeChecker checker, BaseTypeVisitor<?> visitor,
			AnnotatedTypeFactory atypeFactory) {
		super(checker, visitor, atypeFactory);
	}
    
    protected void reportValidityResult(
            final /*@CompilerMessageKey*/ String errorType,
            final AnnotatedTypeMirror type, final Tree p) {
        checker.report(Result.failure(errorType, type.getAnnotations(),
                        type.getUnderlyingType().toString()), p);
        isValid = false;
    }

}
