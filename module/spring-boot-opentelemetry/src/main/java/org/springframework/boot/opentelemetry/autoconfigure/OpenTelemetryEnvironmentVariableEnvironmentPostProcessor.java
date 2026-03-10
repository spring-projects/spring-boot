/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.opentelemetry.autoconfigure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.OriginTrackedMapPropertySource;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.boot.opentelemetry.autoconfigure.OpenTelemetryEnvironmentVariables.EnvVariable;
import org.springframework.boot.origin.OriginTrackedValue;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Converts OpenTelemetry SDK environment variables into Spring Boot properties.
 * <p>
 * Can be disabled by setting {@code management.opentelemetry.map-environment-variables}
 * to {@code false}.
 *
 * @author Moritz Halbritter
 * @since 4.1.0
 */
public class OpenTelemetryEnvironmentVariableEnvironmentPostProcessor implements EnvironmentPostProcessor {

	private static final String ENABLED_PROPERTY = "management.opentelemetry.map-environment-variables";

	private final Log logger;

	private final OpenTelemetryEnvironmentVariables environmentVariables;

	public OpenTelemetryEnvironmentVariableEnvironmentPostProcessor(DeferredLogFactory logFactory) {
		this(logFactory, OpenTelemetryEnvironmentVariables.forSystemEnv(logFactory));
	}

	OpenTelemetryEnvironmentVariableEnvironmentPostProcessor(DeferredLogFactory logFactory,
			OpenTelemetryEnvironmentVariables environmentVariables) {
		this.logger = logFactory.getLog(OpenTelemetryEnvironmentVariableEnvironmentPostProcessor.class);
		this.environmentVariables = environmentVariables;
	}

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		if (!environment.getProperty(ENABLED_PROPERTY, Boolean.class, true)) {
			return;
		}
		Map<String, OriginTrackedValue> map = new HashMap<>();
		mapEnabled(map);
		mapPropagators(map);
		mapSampler(map);
		mapMetricsEnvironmentVariables(map);
		mapTracesEnvironmentVariables(map);
		mapLogsEnvironmentVariables(map);
		if (map.isEmpty()) {
			return;
		}
		environment.getPropertySources()
			.addFirst(new OriginTrackedMapPropertySource("openTelemetryEnvironmentVariables", map));
	}

	private void mapEnabled(Map<String, OriginTrackedValue> map) {
		this.environmentVariables.getBoolean("OTEL_SDK_DISABLED")
			.addToMap(map, "management.opentelemetry.enabled", this::mapDisabledFlag);
	}

	private void mapSampler(Map<String, OriginTrackedValue> map) {
		this.environmentVariables.getString("OTEL_TRACES_SAMPLER")
			.addToMap(map, "management.opentelemetry.tracing.sampler", this::mapSamplerType);
		this.environmentVariables.getString("OTEL_TRACES_SAMPLER_ARG")
			.addToMap(map, "management.tracing.sampling.probability", this::mapSamplerProbability);
	}

	private void mapPropagators(Map<String, OriginTrackedValue> map) {
		this.environmentVariables.getString("OTEL_PROPAGATORS")
			.addListToMap(map, "management.tracing.propagation.type", this::mapPropagationType);
		this.environmentVariables.getString("OTEL_PROPAGATORS")
			.addToMap(map, "management.tracing.baggage.enabled", this::mapBaggageEnabled);
	}

	private void mapMetricsEnvironmentVariables(Map<String, OriginTrackedValue> map) {
		this.environmentVariables
			.getString("OTEL_EXPORTER_OTLP_METRICS_ENDPOINT", "OTEL_EXPORTER_OTLP_ENDPOINT",
					(value) -> combineUrl(value, "v1/metrics"))
			.addToMap(map, "management.otlp.metrics.export.url");
		this.environmentVariables.getString("OTEL_EXPORTER_OTLP_METRICS_TEMPORALITY_PREFERENCE")
			.addToMap(map, "management.otlp.metrics.export.aggregation-temporality", this::mapAggregationTemporality);
		this.environmentVariables.getString("OTEL_EXPORTER_OTLP_METRICS_DEFAULT_HISTOGRAM_AGGREGATION")
			.addToMap(map, "management.otlp.metrics.export.histogram-flavor", this::mapHistogramFlavor);
		this.environmentVariables.getString("OTEL_EXPORTER_OTLP_METRICS_COMPRESSION", "OTEL_EXPORTER_OTLP_COMPRESSION")
			.addToMap(map, "management.otlp.metrics.export.compression-mode", this::mapCompression);
		this.environmentVariables.getTimeout("OTEL_EXPORTER_OTLP_METRICS_TIMEOUT", "OTEL_EXPORTER_OTLP_TIMEOUT")
			.addToMap(map, "management.otlp.metrics.export.read-timeout");
		this.environmentVariables.getString("OTEL_EXPORTER_OTLP_METRICS_HEADERS", "OTEL_EXPORTER_OTLP_HEADERS")
			.addMappingToMap(map, "management.otlp.metrics.export.headers", this::mapHeaders);
		this.environmentVariables.getDuration("OTEL_METRIC_EXPORT_INTERVAL")
			.addToMap(map, "management.otlp.metrics.export.step");
		this.environmentVariables.getString("OTEL_METRICS_EXPORTER")
			.addToMap(map, "management.otlp.metrics.export.enabled", this::mapExportEnabled);
		EnvVariable certificate = this.environmentVariables.getString("OTEL_EXPORTER_OTLP_METRICS_CERTIFICATE",
				"OTEL_EXPORTER_OTLP_CERTIFICATE");
		EnvVariable clientKey = this.environmentVariables.getString("OTEL_EXPORTER_OTLP_METRICS_CLIENT_KEY",
				"OTEL_EXPORTER_OTLP_CLIENT_KEY");
		EnvVariable clientCertificate = this.environmentVariables
			.getString("OTEL_EXPORTER_OTLP_METRICS_CLIENT_CERTIFICATE", "OTEL_EXPORTER_OTLP_CLIENT_CERTIFICATE");
		addSslBundle(map, "management.otlp.metrics.export.ssl.bundle", "opentelemetry-metrics", certificate, clientKey,
				clientCertificate);
	}

	private void mapTracesEnvironmentVariables(Map<String, OriginTrackedValue> map) {
		this.environmentVariables
			.getString("OTEL_EXPORTER_OTLP_TRACES_ENDPOINT", "OTEL_EXPORTER_OTLP_ENDPOINT",
					(value) -> combineUrl(value, "v1/traces"))
			.addToMap(map, "management.opentelemetry.tracing.export.otlp.endpoint");
		this.environmentVariables.getString("OTEL_EXPORTER_OTLP_TRACES_COMPRESSION", "OTEL_EXPORTER_OTLP_COMPRESSION")
			.addToMap(map, "management.opentelemetry.tracing.export.otlp.compression", this::mapCompression);
		this.environmentVariables.getTimeout("OTEL_EXPORTER_OTLP_TRACES_TIMEOUT", "OTEL_EXPORTER_OTLP_TIMEOUT")
			.addToMap(map, "management.opentelemetry.tracing.export.otlp.timeout");
		this.environmentVariables.getString("OTEL_EXPORTER_OTLP_TRACES_PROTOCOL", "OTEL_EXPORTER_OTLP_PROTOCOL")
			.addToMap(map, "management.opentelemetry.tracing.export.otlp.transport", this::mapProtocol);
		this.environmentVariables.getString("OTEL_EXPORTER_OTLP_TRACES_HEADERS", "OTEL_EXPORTER_OTLP_HEADERS")
			.addMappingToMap(map, "management.opentelemetry.tracing.export.otlp.headers", this::mapHeaders);
		this.environmentVariables.getDuration("OTEL_BSP_SCHEDULE_DELAY")
			.addToMap(map, "management.opentelemetry.tracing.export.schedule-delay");
		this.environmentVariables.getTimeout("OTEL_BSP_EXPORT_TIMEOUT")
			.addToMap(map, "management.opentelemetry.tracing.export.timeout");
		this.environmentVariables.getInt("OTEL_BSP_MAX_QUEUE_SIZE")
			.addToMap(map, "management.opentelemetry.tracing.export.max-queue-size");
		this.environmentVariables.getInt("OTEL_BSP_MAX_EXPORT_BATCH_SIZE")
			.addToMap(map, "management.opentelemetry.tracing.export.max-batch-size");
		this.environmentVariables.getString("OTEL_TRACES_EXPORTER")
			.addToMap(map, "management.tracing.export.otlp.enabled", this::mapExportEnabled);
		this.environmentVariables.getInt("OTEL_SPAN_ATTRIBUTE_VALUE_LENGTH_LIMIT", "OTEL_ATTRIBUTE_VALUE_LENGTH_LIMIT")
			.addToMap(map, "management.opentelemetry.tracing.limits.max-attribute-value-length");
		this.environmentVariables.getInt("OTEL_SPAN_ATTRIBUTE_COUNT_LIMIT", "OTEL_ATTRIBUTE_COUNT_LIMIT")
			.addToMap(map, "management.opentelemetry.tracing.limits.max-attributes");
		this.environmentVariables.getInt("OTEL_SPAN_EVENT_COUNT_LIMIT")
			.addToMap(map, "management.opentelemetry.tracing.limits.max-events");
		this.environmentVariables.getInt("OTEL_SPAN_LINK_COUNT_LIMIT")
			.addToMap(map, "management.opentelemetry.tracing.limits.max-links");
		this.environmentVariables.getInt("OTEL_EVENT_ATTRIBUTE_COUNT_LIMIT")
			.addToMap(map, "management.opentelemetry.tracing.limits.max-attributes-per-event");
		this.environmentVariables.getInt("OTEL_LINK_ATTRIBUTE_COUNT_LIMIT")
			.addToMap(map, "management.opentelemetry.tracing.limits.max-attributes-per-link");
		this.environmentVariables.getString("OTEL_METRICS_EXEMPLAR_FILTER")
			.addToMap(map, "management.tracing.exemplars.include", this::mapExemplarsInclude);
		EnvVariable certificate = this.environmentVariables.getString("OTEL_EXPORTER_OTLP_TRACES_CERTIFICATE",
				"OTEL_EXPORTER_OTLP_CERTIFICATE");
		EnvVariable clientKey = this.environmentVariables.getString("OTEL_EXPORTER_OTLP_TRACES_CLIENT_KEY",
				"OTEL_EXPORTER_OTLP_CLIENT_KEY");
		EnvVariable clientCertificate = this.environmentVariables
			.getString("OTEL_EXPORTER_OTLP_TRACES_CLIENT_CERTIFICATE", "OTEL_EXPORTER_OTLP_CLIENT_CERTIFICATE");
		addSslBundle(map, "management.opentelemetry.tracing.export.otlp.ssl.bundle", "opentelemetry-tracing",
				certificate, clientKey, clientCertificate);
	}

	private void mapLogsEnvironmentVariables(Map<String, OriginTrackedValue> map) {
		this.environmentVariables
			.getString("OTEL_EXPORTER_OTLP_LOGS_ENDPOINT", "OTEL_EXPORTER_OTLP_ENDPOINT",
					(value) -> combineUrl(value, "v1/logs"))
			.addToMap(map, "management.opentelemetry.logging.export.otlp.endpoint");
		this.environmentVariables.getString("OTEL_EXPORTER_OTLP_LOGS_COMPRESSION", "OTEL_EXPORTER_OTLP_COMPRESSION")
			.addToMap(map, "management.opentelemetry.logging.export.otlp.compression", this::mapCompression);
		this.environmentVariables.getTimeout("OTEL_EXPORTER_OTLP_LOGS_TIMEOUT", "OTEL_EXPORTER_OTLP_TIMEOUT")
			.addToMap(map, "management.opentelemetry.logging.export.otlp.timeout");
		this.environmentVariables.getString("OTEL_EXPORTER_OTLP_LOGS_PROTOCOL", "OTEL_EXPORTER_OTLP_PROTOCOL")
			.addToMap(map, "management.opentelemetry.logging.export.otlp.transport", this::mapProtocol);
		this.environmentVariables.getString("OTEL_EXPORTER_OTLP_LOGS_HEADERS", "OTEL_EXPORTER_OTLP_HEADERS")
			.addMappingToMap(map, "management.opentelemetry.logging.export.otlp.headers", this::mapHeaders);
		this.environmentVariables.getDuration("OTEL_BLRP_SCHEDULE_DELAY")
			.addToMap(map, "management.opentelemetry.logging.export.schedule-delay");
		this.environmentVariables.getTimeout("OTEL_BLRP_EXPORT_TIMEOUT")
			.addToMap(map, "management.opentelemetry.logging.export.timeout");
		this.environmentVariables.getInt("OTEL_BLRP_MAX_QUEUE_SIZE")
			.addToMap(map, "management.opentelemetry.logging.export.max-queue-size");
		this.environmentVariables.getInt("OTEL_BLRP_MAX_EXPORT_BATCH_SIZE")
			.addToMap(map, "management.opentelemetry.logging.export.max-batch-size");
		this.environmentVariables.getString("OTEL_LOGS_EXPORTER")
			.addToMap(map, "management.logging.export.otlp.enabled", this::mapExportEnabled);
		this.environmentVariables
			.getInt("OTEL_LOGRECORD_ATTRIBUTE_VALUE_LENGTH_LIMIT", "OTEL_ATTRIBUTE_VALUE_LENGTH_LIMIT")
			.addToMap(map, "management.opentelemetry.logging.limits.max-attribute-value-length");
		this.environmentVariables.getInt("OTEL_LOGRECORD_ATTRIBUTE_COUNT_LIMIT", "OTEL_ATTRIBUTE_COUNT_LIMIT")
			.addToMap(map, "management.opentelemetry.logging.limits.max-attributes");
		EnvVariable certificate = this.environmentVariables.getString("OTEL_EXPORTER_OTLP_LOGS_CERTIFICATE",
				"OTEL_EXPORTER_OTLP_CERTIFICATE");
		EnvVariable clientKey = this.environmentVariables.getString("OTEL_EXPORTER_OTLP_LOGS_CLIENT_KEY",
				"OTEL_EXPORTER_OTLP_CLIENT_KEY");
		EnvVariable clientCertificate = this.environmentVariables
			.getString("OTEL_EXPORTER_OTLP_LOGS_CLIENT_CERTIFICATE", "OTEL_EXPORTER_OTLP_CLIENT_CERTIFICATE");
		addSslBundle(map, "management.opentelemetry.logging.export.otlp.ssl.bundle", "opentelemetry-logging",
				certificate, clientKey, clientCertificate);
	}

	private void addSslBundle(Map<String, OriginTrackedValue> map, String bundleKey, String bundleName,
			EnvVariable certificateFile, EnvVariable clientKeyFile, EnvVariable clientCertificateFile) {
		EnvVariable firstPresent = this.environmentVariables.getFirstPresent(certificateFile, clientKeyFile,
				clientCertificateFile);
		if (firstPresent == null) {
			return;
		}
		map.put(bundleKey, OriginTrackedValue.of(bundleName, this.environmentVariables.getOrigin(firstPresent)));
		if (certificateFile.isPresent()) {
			String value = certificateFile.value();
			map.put("spring.ssl.bundle.pem.%s.truststore.certificate".formatted(bundleName), OriginTrackedValue
				.of("file:%s".formatted(value), this.environmentVariables.getOrigin(certificateFile)));
		}
		if (clientKeyFile.isPresent()) {
			String value = clientKeyFile.value();
			map.put("spring.ssl.bundle.pem.%s.keystore.private-key".formatted(bundleName), OriginTrackedValue
				.of("file:%s".formatted(value), this.environmentVariables.getOrigin(clientKeyFile)));
		}
		if (clientCertificateFile.isPresent()) {
			String value = clientCertificateFile.value();
			map.put("spring.ssl.bundle.pem.%s.keystore.certificate".formatted(bundleName), OriginTrackedValue
				.of("file:%s".formatted(value), this.environmentVariables.getOrigin(clientCertificateFile)));
		}
	}

	private @Nullable String mapDisabledFlag(String name, String value) {
		// OTEL_SDK_DISABLED: true means disabled
		// management.opentelemetry.enabled: true means enabled
		return switch (value) {
			case "true" -> "false";
			case "false" -> "true";
			default -> {
				this.logger.warn("Invalid value for environment variable '%s': '%s'".formatted(name, value));
				yield null;
			}
		};
	}

	private String mapBaggageEnabled(String name, String value) {
		Set<String> propagators = toSet(value);
		if (propagators.contains("none")) {
			if (propagators.size() > 1) {
				this.logger.warn("Environment variable '%s' contains 'none', but also contains more elements: '%s'"
					.formatted(name, value));
			}
			return "false";
		}
		return Boolean.toString(propagators.contains("baggage"));
	}

	private List<String> mapPropagationType(String name, String value) {
		Set<String> propagators = toSet(value);
		if (propagators.contains("none")) {
			if (propagators.size() > 1) {
				this.logger.warn("Environment variable '%s' contains 'none', but also contains more elements: '%s'"
					.formatted(name, value));
			}
			return Collections.emptyList();
		}
		List<String> result = new ArrayList<>();
		for (String propagator : propagators) {
			switch (propagator) {
				case "tracecontext" -> result.add("W3C");
				case "b3" -> result.add("B3");
				case "b3multi" -> result.add("B3_MULTI");
				case "baggage" -> {
				}
				default -> this.logger
					.warn("Unsupported propagator '%s' in environment variable '%s'".formatted(propagator, name));
			}
		}
		return result;
	}

	private Map<String, String> mapHeaders(String name, String value) {
		return W3CHeaderParser.parse(value);
	}

	private String mapExportEnabled(String name, String value) {
		Set<String> exporters = toSet(value);
		if (exporters.contains("none")) {
			if (exporters.size() > 1) {
				this.logger.warn("Environment variable '%s' contains 'none', but also contains more elements: '%s'"
					.formatted(name, value));
			}
			return "false";
		}
		if (exporters.contains("otlp")) {
			return "true";
		}
		return "false";
	}

	private @Nullable String mapProtocol(String name, String value) {
		return switch (value.toLowerCase(Locale.ROOT)) {
			case "grpc" -> "GRPC";
			case "http/protobuf" -> "HTTP";
			default -> {
				this.logger.warn("Invalid value for environment variable '%s': '%s'".formatted(name, value));
				yield null;
			}
		};
	}

	private @Nullable String mapExemplarsInclude(String name, String value) {
		return switch (value.toLowerCase(Locale.ROOT)) {
			case "always_on" -> "ALL";
			case "always_off" -> "NONE";
			case "trace_based" -> "SAMPLED_TRACES";
			default -> {
				this.logger.warn("Invalid value for environment variable '%s': '%s'".formatted(name, value));
				yield null;
			}
		};
	}

	private @Nullable String mapSamplerType(String name, String value) {
		return switch (value.toLowerCase(Locale.ROOT)) {
			case "always_on" -> "ALWAYS_ON";
			case "always_off" -> "ALWAYS_OFF";
			case "traceidratio" -> "TRACE_ID_RATIO";
			case "parentbased_always_on" -> "PARENT_BASED_ALWAYS_ON";
			case "parentbased_always_off" -> "PARENT_BASED_ALWAYS_OFF";
			case "parentbased_traceidratio" -> "PARENT_BASED_TRACE_ID_RATIO";
			default -> {
				this.logger.warn("Invalid value for environment variable '%s': '%s'".formatted(name, value));
				yield null;
			}
		};
	}

	private @Nullable String mapSamplerProbability(String name, String value) {
		EnvVariable sampler = this.environmentVariables.getString("OTEL_TRACES_SAMPLER");
		if (!sampler.isPresent()) {
			return null;
		}
		String samplerValue = sampler.value();
		Assert.state(samplerValue != null, "'samplerValue' must not be null");
		return switch (samplerValue.toLowerCase(Locale.ROOT)) {
			case "traceidratio", "parentbased_traceidratio" -> {
				try {
					double probability = Double.parseDouble(value);
					if (probability < 0.0 || probability > 1.0) {
						this.logger
							.warn("Invalid value for environment variable '%s'. Must be between 0.0 and 1.0: '%s'"
								.formatted(name, value));
						yield null;
					}
					yield String.valueOf(probability);
				}
				catch (NumberFormatException ex) {
					this.logger.warn("Invalid value for environment variable '%s': '%s'".formatted(name, value));
					yield null;
				}
			}
			default -> {
				this.logger
					.warn("Unsupported environment variable '%s' for sampler '%s' ".formatted(name, samplerValue));
				yield null;
			}
		};
	}

	private @Nullable String mapCompression(String name, String value) {
		return switch (value.toLowerCase(Locale.ROOT)) {
			case "gzip" -> "GZIP";
			case "none" -> "NONE";
			default -> {
				this.logger.warn("Invalid value for environment variable '%s': '%s'".formatted(name, value));
				yield null;
			}
		};
	}

	private @Nullable String mapHistogramFlavor(String name, String value) {
		return switch (value.toLowerCase(Locale.ROOT)) {
			case "explicit_bucket_histogram" -> "EXPLICIT_BUCKET_HISTOGRAM";
			case "base2_exponential_bucket_histogram" -> "BASE2_EXPONENTIAL_BUCKET_HISTOGRAM";
			default -> {
				this.logger.warn("Invalid value for environment variable '%s': '%s'".formatted(name, value));
				yield null;
			}
		};
	}

	private @Nullable String mapAggregationTemporality(String name, String value) {
		return switch (value.toLowerCase(Locale.ROOT)) {
			case "cumulative" -> "CUMULATIVE";
			case "delta" -> "DELTA";
			default -> {
				this.logger.warn("Invalid value for environment variable '%s': '%s'".formatted(name, value));
				yield null;
			}
		};
	}

	private String combineUrl(String base, String path) {
		Assert.state(!path.startsWith("/"), "'path' must not start with '/'");
		if (base.endsWith("/")) {
			return base + path;
		}
		return base + "/" + path;
	}

	private Set<String> toSet(String value) {
		Set<String> result = new LinkedHashSet<>();
		for (String entry : StringUtils.commaDelimitedListToStringArray(value)) {
			entry = entry.toLowerCase(Locale.ROOT).trim();
			if (entry.isEmpty()) {
				continue;
			}
			result.add(entry);
		}
		return result;
	}

}
