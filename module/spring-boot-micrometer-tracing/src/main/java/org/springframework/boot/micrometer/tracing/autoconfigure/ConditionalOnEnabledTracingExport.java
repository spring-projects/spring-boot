/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.tracing.autoconfigure;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Conditional;

/**
 * {@link Conditional @Conditional} that checks whether tracing is enabled. It matches if
 * the value of the {@code management.tracing.export.enabled} property is {@code true} or
 * if it is not configured. If the {@link #value() tracing exporter name} is set, the
 * {@code management.tracing.export.<name>.enabled} property can be used to control the
 * behavior for the specific tracing exporter. In that case, the exporter specific
 * property takes precedence over the global property.
 *
 * @author Moritz Halbritter
 * @since 4.0.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Documented
@Conditional(OnEnabledTracingExportCondition.class)
public @interface ConditionalOnEnabledTracingExport {

	/**
	 * Name of the tracing exporter.
	 * @return the name of the tracing exporter
	 */
	String value() default "";

}
