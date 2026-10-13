/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics.autoconfigure.export;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Conditional;

/**
 * {@link Conditional @Conditional} that checks whether a metrics exporter is enabled. If
 * the {@code management.<name>.metrics.export.enabled} property is configured then its
 * value is used to determine if it matches. Otherwise, matches if the value of the
 * {@code management.defaults.metrics.export.enabled} property is {@code true} or if it is
 * not configured.
 *
 * @author Chris Bono
 * @since 4.0.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Documented
@Conditional(OnMetricsExportEnabledCondition.class)
public @interface ConditionalOnEnabledMetricsExport {

	/**
	 * The name of the metrics exporter.
	 * @return the name of the metrics exporter
	 */
	String value();

}
