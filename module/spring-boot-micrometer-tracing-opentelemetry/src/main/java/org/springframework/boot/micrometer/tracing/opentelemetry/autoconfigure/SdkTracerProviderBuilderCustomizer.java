/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.tracing.opentelemetry.autoconfigure;

import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;

/**
 * Callback interface that can be used to customize the {@link SdkTracerProviderBuilder}
 * that is used to create the auto-configured {@link SdkTracerProvider}.
 *
 * @author Yanming Zhou
 * @since 4.0.0
 */
@FunctionalInterface
public interface SdkTracerProviderBuilderCustomizer {

	/**
	 * Customize the given {@code builder}.
	 * @param builder the builder to customize
	 */
	void customize(SdkTracerProviderBuilder builder);

}
