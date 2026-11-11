/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.tracing.test.autoconfigure;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;

/**
 * Annotation that can be applied to a test class to enable auto-configuration for
 * tracing.
 * <p>
 * If this annotation is applied to a sliced test, a no-op {@link Tracer} and an
 * {@link ObservationRegistry} are added to the application context.
 *
 * @author Moritz Halbritter
 * @since 4.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ImportAutoConfiguration
public @interface AutoConfigureTracing {

	/**
	 * Whether traces should be reported to external systems in the test.
	 * @return whether traces should be reported to external systems in the test
	 */
	boolean export() default true;

}
