/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics.test.autoconfigure;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.ObservationRegistry;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;

/**
 * Annotation that can be applied to a test class to enable auto-configuration for
 * metrics.
 * <p>
 * If this annotation is applied to a sliced test, an in-memory {@link MeterRegistry} and
 * an {@link ObservationRegistry} are added to the application context.
 *
 * @author Moritz Halbritter
 * @author Lordwill Kandiro
 * @since 4.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ImportAutoConfiguration
public @interface AutoConfigureMetrics {

	/**
	 * Whether metrics should be reported to external systems in the test.
	 * @return whether metrics should be reported to external systems in the test
	 */
	boolean export() default true;

	/**
	 * Whether {@link MeterRegistry meter registries} should be added to Micrometer's
	 * {@link io.micrometer.core.instrument.Metrics#globalRegistry global registry} in the
	 * test. If {@code management.metrics.use-global-registry} has been set explicitly,
	 * its value is used and this attribute has no effect.
	 * @return whether the global registry should be used in the test
	 * @since 4.2.0
	 */
	boolean useGlobalRegistry() default false;

}
