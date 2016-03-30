package edu.cmu.cs.glacier.qual;

import java.lang.annotation.Target;

import org.checkerframework.framework.qual.SubtypeOf;

import java.lang.annotation.ElementType;

@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({GlacierTop.class})
public @interface Immutable { }
