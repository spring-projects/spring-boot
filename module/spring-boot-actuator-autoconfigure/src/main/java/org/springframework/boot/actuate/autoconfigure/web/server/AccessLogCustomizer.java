/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.autoconfigure.web.server;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.web.server.WebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.core.Ordered;
import org.springframework.lang.Contract;

/**
 * Base class for a {@link WebServerFactoryCustomizer} that customizes the web server's
 * access log.
 *
 * @param <T> the {@link WebServerFactory} type that can be customized
 * @author Andy Wilkinson
 * @since 4.0.0
 */
public abstract class AccessLogCustomizer<T extends WebServerFactory>
		implements WebServerFactoryCustomizer<T>, Ordered {

	private final @Nullable String prefix;

	protected AccessLogCustomizer(@Nullable String prefix) {
		this.prefix = prefix;
	}

	@Contract("!null -> !null")
	protected @Nullable String customizePrefix(@Nullable String existingPrefix) {
		if (this.prefix == null) {
			return existingPrefix;
		}
		if (existingPrefix == null) {
			return this.prefix;
		}
		if (existingPrefix.startsWith(this.prefix)) {
			return existingPrefix;
		}
		return this.prefix + existingPrefix;
	}

	@Override
	public int getOrder() {
		return 1;
	}

}
