/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.opentelemetry.autoconfigure.logging.otlp;

import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporterBuilder;

/**
 * Callback interface that can be implemented by beans wishing to customize the
 * {@link OtlpHttpLogRecordExporterBuilder} whilst retaining default auto-configuration.
 *
 * @author Joaquin Santana
 * @since 4.1.0
 */
@FunctionalInterface
public interface OtlpHttpLogRecordExporterBuilderCustomizer {

	/**
	 * Customize the {@link OtlpHttpLogRecordExporterBuilder}.
	 * @param builder the builder to customize
	 */
	void customize(OtlpHttpLogRecordExporterBuilder builder);

}
