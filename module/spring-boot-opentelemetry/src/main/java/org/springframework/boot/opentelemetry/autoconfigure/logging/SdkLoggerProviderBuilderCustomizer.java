/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.opentelemetry.autoconfigure.logging;

import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.SdkLoggerProviderBuilder;

/**
 * Callback interface that can be used to customize the {@link SdkLoggerProviderBuilder}
 * that is used to create the auto-configured {@link SdkLoggerProvider}.
 *
 * @author Toshiaki Maki
 * @since 4.0.0
 */
@FunctionalInterface
public interface SdkLoggerProviderBuilderCustomizer {

	/**
	 * Customize the given {@code builder}.
	 * @param builder the builder to customize
	 */
	void customize(SdkLoggerProviderBuilder builder);

}
