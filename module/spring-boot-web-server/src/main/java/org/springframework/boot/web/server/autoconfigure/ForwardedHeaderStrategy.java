/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.web.server.autoconfigure;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MutuallyExclusiveConfigurationPropertiesException;
import org.springframework.boot.web.server.autoconfigure.ServerProperties.ForwardHeadersStrategy;
import org.springframework.core.env.Environment;

/**
 * Strategies for supporting forwarded headers.
 *
 * @author Phillip Webb
 * @since 4.2.0
 */
public enum ForwardedHeaderStrategy {

	/**
	 * Use the underlying web server's support forwarded headers.
	 */
	WEB_SERVER,

	/**
	 * Use Spring's support for handling forwarded headers.
	 */
	FRAMEWORK,

	/**
	 * Ignore forwarded headers.
	 */
	NONE;

	private static final String STRATEGY_PROPERTY = "server.request.header.forwarded.strategy";

	private static final String DEPRECATED_STRATEGY_PROPERTY = "server.forward-headers-strategy";

	@SuppressWarnings("removal")
	static @Nullable ForwardedHeaderStrategy get(Environment environment) {
		Binder binder = Binder.get(environment);
		ForwardedHeaderStrategy strategy = binder.bind(STRATEGY_PROPERTY, ForwardedHeaderStrategy.class).orElse(null);
		ForwardHeadersStrategy deprecatedStrategy = binder
			.bind(DEPRECATED_STRATEGY_PROPERTY, ForwardHeadersStrategy.class)
			.orElse(null);
		return get(strategy, deprecatedStrategy);
	}

	@SuppressWarnings("removal")
	static @Nullable ForwardedHeaderStrategy get(ServerProperties serverProperties) {
		ForwardedHeaderStrategy strategy = serverProperties.getRequest().getHeader().getForwarded().getStrategy();
		ForwardHeadersStrategy deprecatedStrategy = serverProperties.getForwardHeadersStrategy();
		return get(strategy, deprecatedStrategy);
	}

	@SuppressWarnings("removal")
	private static @Nullable ForwardedHeaderStrategy get(@Nullable ForwardedHeaderStrategy strategy,
			@Nullable ForwardHeadersStrategy deprecatedStrategy) {
		MutuallyExclusiveConfigurationPropertiesException.throwIfMultipleNonNullValuesIn((entries) -> {
			entries.put(DEPRECATED_STRATEGY_PROPERTY, deprecatedStrategy);
			entries.put(STRATEGY_PROPERTY, strategy);
		});
		return (strategy != null) ? strategy : adaptDeprecated(deprecatedStrategy);
	}

	@SuppressWarnings("removal")
	static @Nullable ForwardedHeaderStrategy adaptDeprecated(@Nullable ForwardHeadersStrategy value) {
		if (value == null) {
			return null;
		}
		return switch (value) {
			case NATIVE -> WEB_SERVER;
			case FRAMEWORK -> FRAMEWORK;
			case NONE -> NONE;
		};
	}

}
