/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jackson;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

/**
 * Provides a mixin class implementation that registers with Jackson when using
 * {@link JacksonMixinModule}.
 *
 * @author Guirong Hu
 * @see JacksonMixinModule
 * @since 4.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface JacksonMixin {

	/**
	 * Alias for the {@link #type()} attribute. Allows for more concise annotation
	 * declarations e.g.: {@code @JacksonMixin(MyType.class)} instead of
	 * {@code @JacksonMixin(type=MyType.class)}.
	 * @return the mixed-in classes
	 */
	@AliasFor("type")
	Class<?>[] value() default {};

	/**
	 * The types that are handled by the provided mixin class. {@link #value()} is an
	 * alias for (and mutually exclusive with) this attribute.
	 * @return the mixed-in classes
	 */
	@AliasFor("value")
	Class<?>[] type() default {};

}
