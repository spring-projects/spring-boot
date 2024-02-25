/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.opentelemetry;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.OpenTelemetrySdkBuilder;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.resources.ResourceBuilder;
import io.opentelemetry.sdk.trace.SdkTracerProvider;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for OpenTelemetry.
 *
 * @author Moritz Halbritter
 * @since 3.2.0
 */
@AutoConfiguration
@ConditionalOnClass(OpenTelemetrySdk.class)
@EnableConfigurationProperties(OpenTelemetryProperties.class)
public class OpenTelemetryAutoConfiguration {

	/**
	 * Default value for application name if {@code spring.application.name} is not set.
	 */
	private static final String DEFAULT_APPLICATION_NAME = "unknown_service";

	private static final AttributeKey<String> ATTRIBUTE_KEY_SERVICE_NAME = AttributeKey.stringKey("service.name");

	/**
	 * Creates an instance of OpenTelemetrySdk if no other bean of type OpenTelemetry is
	 * present.
	 * @param tracerProvider ObjectProvider of SdkTracerProvider to be used for creating
	 * the tracer provider.
	 * @param propagators ObjectProvider of ContextPropagators to be used for creating the
	 * propagators.
	 * @param loggerProvider ObjectProvider of SdkLoggerProvider to be used for creating
	 * the logger provider.
	 * @param meterProvider ObjectProvider of SdkMeterProvider to be used for creating the
	 * meter provider.
	 * @return An instance of OpenTelemetrySdk.
	 */
	@Bean
	@ConditionalOnMissingBean(OpenTelemetry.class)
	OpenTelemetrySdk openTelemetry(ObjectProvider<SdkTracerProvider> tracerProvider,
			ObjectProvider<ContextPropagators> propagators, ObjectProvider<SdkLoggerProvider> loggerProvider,
			ObjectProvider<SdkMeterProvider> meterProvider) {
		OpenTelemetrySdkBuilder builder = OpenTelemetrySdk.builder();
		tracerProvider.ifAvailable(builder::setTracerProvider);
		propagators.ifAvailable(builder::setPropagators);
		loggerProvider.ifAvailable(builder::setLoggerProvider);
		meterProvider.ifAvailable(builder::setMeterProvider);
		return builder.build();
	}

	/**
	 * Creates a {@link Resource} object for OpenTelemetry based on the provided
	 * environment and properties. If a bean of type {@link Resource} is already present
	 * in the application context, this method will not be executed.
	 * @param environment the {@link Environment} object containing the application's
	 * environment properties
	 * @param properties the {@link OpenTelemetryProperties} object containing the
	 * OpenTelemetry properties
	 * @return a {@link Resource} object representing the OpenTelemetry resource
	 */
	@Bean
	@ConditionalOnMissingBean
	Resource openTelemetryResource(Environment environment, OpenTelemetryProperties properties) {
		String applicationName = environment.getProperty("spring.application.name", DEFAULT_APPLICATION_NAME);
		return Resource.getDefault()
			.merge(Resource.create(Attributes.of(ATTRIBUTE_KEY_SERVICE_NAME, applicationName)))
			.merge(toResource(properties));
	}

	/**
	 * Converts the given OpenTelemetryProperties object to a Resource object.
	 * @param properties the OpenTelemetryProperties object to convert
	 * @return the converted Resource object
	 */
	private static Resource toResource(OpenTelemetryProperties properties) {
		ResourceBuilder builder = Resource.builder();
		properties.getResourceAttributes().forEach(builder::put);
		return builder.build();
	}

}
