/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.architecture.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Indexed;

@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Indexed
public @interface TestConfigurationProperties {

	@AliasFor("prefix")
	String value() default "";

	@AliasFor("value")
	String prefix() default "";

	boolean ignoreInvalidFields() default false;

	boolean ignoreUnknownFields() default true;

}
