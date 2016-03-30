package edu.cmu.cs.glacier;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
public class GlacierChecker extends BaseTypeChecker {

	/*
    @Override
    // This whole method exists only to catch exceptions so we can find out about them for debugging.
    protected SourceVisitor<Void, Void> createSourceVisitor() {
    	System.out.println("beginning of createSourceVisitor()");

    	try {
    		this.visitor = new GlacierVisitor(this);
    		System.out.println("after new GlacierVisitor");
    	}
    	catch (Exception e) {
    		System.out.println("Error creating visitor. Exception: " + e);
    	}

    	SourceVisitor<Void, Void>> v = null;
    	try {
    		v = super.createSourceVisitor();
    	}
    	finally {
        	System.out.println("after createSourceVisitor() in finally;");

    	}
    	System.out.println("createSourceVisitor: " + v);
    	return v;
    }
    */
	
	@Override
    protected BaseTypeVisitor<GlacierAnnotatedTypeFactory> createSourceVisitor() {
		return new GlacierVisitor(this);
	}

	
}
