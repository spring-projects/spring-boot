/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.http.client;

import javax.net.ssl.SSLContext;

import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.DefaultHostnameVerifier;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslOptions;
import org.springframework.lang.Contract;

/**
 * Adapts {@link SslBundle} to an
 * <a href="https://hc.apache.org/httpcomponents-client-ga/">Apache HttpComponents</a>
 * {@link DefaultClientTlsStrategy}.
 *
 * @author Phillip Webb
 */
final class HttpComponentsSslBundleTlsStrategy {

	private HttpComponentsSslBundleTlsStrategy() {
	}

	@Contract("!null -> !null")
	static @Nullable DefaultClientTlsStrategy get(@Nullable SslBundle sslBundle) {
		if (sslBundle == null) {
			return null;
		}
		SslOptions options = sslBundle.getOptions();
		SSLContext sslContext = sslBundle.createSslContext();
		return new DefaultClientTlsStrategy(sslContext, options.getEnabledProtocols(), options.getCiphers(), null,
				new DefaultHostnameVerifier());
	}

}
