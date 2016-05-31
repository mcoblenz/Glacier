package edu.cmu.cs.glacier.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

import org.checkerframework.framework.qual.*;

@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({ReadOnly.class})
@DefaultQualifierInHierarchy
@DefaultFor({TypeUseLocation.RESOURCE_VARIABLE, TypeUseLocation.LOCAL_VARIABLE, TypeUseLocation.EXCEPTION_PARAMETER,
    TypeUseLocation.IMPLICIT_UPPER_BOUND })
public @interface Mutable { }
