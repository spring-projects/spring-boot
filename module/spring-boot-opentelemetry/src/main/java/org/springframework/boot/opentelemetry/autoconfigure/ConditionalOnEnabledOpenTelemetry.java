/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.opentelemetry.autoconfigure;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.context.annotation.Conditional;

/**
 * {@link Conditional @Conditional} that checks whether OpenTelemetry is enabled. It
 * matches if the value of the {@code management.opentelemetry.enabled} property is
 * {@code true} or if it is not configured.
 *
 * @author Moritz Halbritter
 * @since 4.1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Documented
@ConditionalOnBooleanProperty(name = "management.opentelemetry.enabled", matchIfMissing = true)
public @interface ConditionalOnEnabledOpenTelemetry {

}
