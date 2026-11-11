/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.tracing.opentelemetry.autoconfigure.otlp;

import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporterBuilder;

/**
 * Callback interface that can be implemented by beans wishing to customize the
 * {@link OtlpGrpcSpanExporterBuilder} to fine-tune its auto-configuration.
 *
 * @author Dmytro Nosan
 * @since 4.0.0
 */
@FunctionalInterface
public interface OtlpGrpcSpanExporterBuilderCustomizer {

	/**
	 * Customize the {@link OtlpGrpcSpanExporterBuilder}.
	 * @param builder the builder to customize
	 */
	void customize(OtlpGrpcSpanExporterBuilder builder);

}
