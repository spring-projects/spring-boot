/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.opentelemetry.autoconfigure.logging.otlp;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.opentelemetry.autoconfigure.OtlpProperties;
import org.springframework.boot.opentelemetry.autoconfigure.logging.otlp.OtlpLoggingConfigurations.ConnectionDetails;
import org.springframework.boot.opentelemetry.autoconfigure.logging.otlp.OtlpLoggingConfigurations.Exporters;
import org.springframework.context.annotation.Import;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for OpenTelemetry logging exports.
 *
 * @author Toshiaki Maki
 * @since 4.0.0
 */
@AutoConfiguration
@ConditionalOnClass({ OpenTelemetry.class, SdkLoggerProvider.class })
@EnableConfigurationProperties({ OtlpLoggingProperties.class, OtlpProperties.class })
@Import({ ConnectionDetails.class, Exporters.class })
public final class OtlpLoggingAutoConfiguration {

}
