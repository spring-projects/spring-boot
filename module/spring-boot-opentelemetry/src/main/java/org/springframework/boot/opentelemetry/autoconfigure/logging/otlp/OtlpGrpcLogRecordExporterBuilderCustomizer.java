/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.opentelemetry.autoconfigure.logging.otlp;

import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporterBuilder;

/**
 * Callback interface that can be implemented by beans wishing to customize the
 * {@link OtlpGrpcLogRecordExporterBuilder} whilst retaining default auto-configuration.
 *
 * @author Joaquin Santana
 * @since 4.1.0
 */
@FunctionalInterface
public interface OtlpGrpcLogRecordExporterBuilderCustomizer {

	/**
	 * Customize the {@link OtlpGrpcLogRecordExporterBuilder}.
	 * @param builder the builder to customize
	 */
	void customize(OtlpGrpcLogRecordExporterBuilder builder);

}
