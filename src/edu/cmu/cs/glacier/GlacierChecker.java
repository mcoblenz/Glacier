package edu.cmu.cs.glacier;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
public class GlacierChecker extends BaseTypeChecker {	
	@Override
    protected BaseTypeVisitor<GlacierAnnotatedTypeFactory> createSourceVisitor() {
		return new GlacierVisitor(this);
	}

	
}
