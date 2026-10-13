/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.autoconfigure;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;

/**
 * Annotation that can be used to override
 * {@link EnableAutoConfiguration @EnableAutoConfiguration}. Often used in combination
 * with {@link ImportAutoConfiguration @ImportAutoConfiguration} to limit the
 * auto-configuration classes that are loaded.
 *
 * @author Phillip Webb
 * @since 1.4.0
 * @see EnableAutoConfiguration#ENABLED_OVERRIDE_PROPERTY
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OverrideAutoConfiguration {

	/**
	 * The value of the {@link EnableAutoConfiguration#ENABLED_OVERRIDE_PROPERTY enabled
	 * override property}.
	 * @return the override value
	 */
	boolean enabled();

}
