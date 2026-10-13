/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.tracing.autoconfigure;

import io.micrometer.tracing.Tracer;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for a no-op implementation of
 * {@link Tracer}.
 *
 * @author Moritz Halbritter
 * @since 4.0.0
 */
@AutoConfiguration(before = MicrometerTracingAutoConfiguration.class)
@ConditionalOnClass(Tracer.class)
@ConditionalOnMissingBean(Tracer.class)
public final class NoopTracerAutoConfiguration {

	@Bean
	Tracer noopTracer() {
		return Tracer.NOOP;
	}

}
