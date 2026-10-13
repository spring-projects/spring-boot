/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jackson2;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

/**
 * Provides a mixin class implementation that registers with Jackson when using
 * {@link JsonMixinModule}.
 *
 * @author Guirong Hu
 * @see JsonMixinModule
 * @since 4.0.0
 * @deprecated since 4.0.0 for removal in 4.3.0 in favor of Jackson 3 and
 * {@link org.springframework.boot.jackson.JacksonMixin}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Deprecated(since = "4.0.0", forRemoval = true)
@SuppressWarnings("removal")
public @interface JsonMixin {

	/**
	 * Alias for the {@link #type()} attribute. Allows for more concise annotation
	 * declarations e.g.: {@code @JsonMixin(MyType.class)} instead of
	 * {@code @JsonMixin(type=MyType.class)}.
	 * @return the mixed-in classes
	 */
	@AliasFor("type")
	Class<?>[] value() default {};

	/**
	 * The types that are handled by the provided mix-in class. {@link #value()} is an
	 * alias for (and mutually exclusive with) this attribute.
	 * @return the mixed-in classes
	 */
	@AliasFor("value")
	Class<?>[] type() default {};

}
