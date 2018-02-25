package edu.cmu.cs.glacier.qual;

import java.lang.annotation.Target;

import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.ImplicitFor;
import org.checkerframework.framework.qual.TypeKind;

import java.lang.annotation.ElementType;

@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({ReadOnly.class})
@ImplicitFor(types = {  TypeKind.BOOLEAN, TypeKind.BYTE,
						TypeKind.CHAR, TypeKind.DOUBLE,
        				TypeKind.FLOAT, TypeKind.INT,
        				TypeKind.LONG, TypeKind.SHORT },
typeNames = { java.lang.Boolean.class,
			  java.lang.Byte.class,
			  java.lang.Short.class,
			  java.lang.Character.class,
			  java.lang.Integer.class,
			  java.lang.Long.class,
			  java.lang.Float.class,
			  java.lang.Double.class }
		)
public @interface Immutable { }
