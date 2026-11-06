/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.web.server;

import org.springframework.util.Assert;

/**
 * Configuration for forwarded request headers.
 *
 * @param naming header naming
 * @param usePrefix if the 'prefix' forward header should be used.
 * @author Brian Clozel
 * @author Phillip Webb
 * @since 4.2.0
 */
public record ForwardedRequestHeaders(Naming naming, boolean usePrefix) {

	/**
	 * A {@link ForwardedRequestHeaders} instance using {@link Naming#X_FORWARDED} and
	 * {@code usePrefix} of {@code false}.
	 */
	public static ForwardedRequestHeaders X_FORWARDED_WITHOUT_PREFIX = new ForwardedRequestHeaders(Naming.X_FORWARDED,
			false);

	public ForwardedRequestHeaders {
		Assert.notNull(naming, "'naming' must not be null");
		Assert.state(!usePrefix || naming == Naming.X_FORWARDED,
				"'usePrefix' can only be used with 'x-forwarded' naming");
	}

	/**
	 * Naming for forwarded headers.
	 */
	public enum Naming {

		/**
		 * Named using the de facto 'X-FOWARDED-*' standard.
		 */
		X_FORWARDED,

		/**
		 * Named using the RFC-7239 standard.
		 */
		RFC_7239

	}

}
