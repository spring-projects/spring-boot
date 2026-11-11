/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.context.annotation.ImportCandidates;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;

/**
 * Import and apply the specified auto-configuration classes. Applies the same ordering
 * rules as {@code @EnableAutoConfiguration} but restricts the auto-configuration classes
 * to the specified set, rather than consulting {@link ImportCandidates}.
 * <p>
 * Can also be used to {@link #exclude()} specific auto-configuration classes such that
 * they will never be applied.
 * <p>
 * Generally, {@code @EnableAutoConfiguration} should be used in preference to this
 * annotation, however, {@code @ImportAutoConfiguration} can be useful in some situations
 * and especially when writing tests.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 1.3.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import(ImportAutoConfigurationImportSelector.class)
public @interface ImportAutoConfiguration {

	/**
	 * The auto-configuration classes that should be imported. This is an alias for
	 * {@link #classes()}.
	 * @return the classes to import
	 */
	@AliasFor("classes")
	Class<?>[] value() default {};

	/**
	 * The auto-configuration classes that should be imported. When empty, the classes are
	 * specified using a file in {@code META-INF/spring} where the file name is the
	 * fully-qualified name of the annotated class, suffixed with {@code .imports}. An
	 * entry in the file may be prefixed with {@code optional:} to indicate that the
	 * imported class should be ignored if it is not on the classpath.
	 * @return the classes to import
	 */
	@AliasFor("value")
	Class<?>[] classes() default {};

	/**
	 * Exclude specific auto-configuration classes such that they will never be applied.
	 * @return the classes to exclude
	 */
	Class<?>[] exclude() default {};

}
