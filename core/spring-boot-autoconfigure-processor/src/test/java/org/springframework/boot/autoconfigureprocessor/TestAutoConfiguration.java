/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigureprocessor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

/**
 * Alternative to Spring Boot's {@code @AutoConfiguration} for testing (removes the need
 * for a dependency on the real annotation).
 *
 * @author Moritz Halbritter
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@TestAutoConfigureBefore
@TestAutoConfigureAfter
public @interface TestAutoConfiguration {

	@AliasFor(annotation = TestAutoConfigureBefore.class, attribute = "value")
	Class<?>[] before() default {};

	@AliasFor(annotation = TestAutoConfigureBefore.class, attribute = "name")
	String[] beforeName() default {};

	@AliasFor(annotation = TestAutoConfigureAfter.class, attribute = "value")
	Class<?>[] after() default {};

	@AliasFor(annotation = TestAutoConfigureAfter.class, attribute = "name")
	String[] afterName() default {};

}
