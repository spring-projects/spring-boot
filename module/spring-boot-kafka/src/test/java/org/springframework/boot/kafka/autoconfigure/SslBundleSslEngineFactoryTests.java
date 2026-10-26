/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.kafka.autoconfigure;

import java.io.IOException;
import java.util.Map;

import javax.net.ssl.SSLEngine;

import org.junit.jupiter.api.Test;

import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundleKey;
import org.springframework.boot.ssl.SslOptions;
import org.springframework.boot.ssl.SslStoreBundle;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SslBundleSslEngineFactory}.
 *
 * @author Jialin Chen
 */
class SslBundleSslEngineFactoryTests {

	@Test
	void createClientSslEngineAppliesSslOptions() throws IOException {
		SslOptions sslOptions = SslOptions.of(new String[] { "TLS_AES_128_GCM_SHA256" }, new String[] { "TLSv1.3" });
		try (SslBundleSslEngineFactory factory = createFactory(sslOptions)) {
			SSLEngine sslEngine = factory.createClientSslEngine("localhost", 9093, "https");
			assertThat(sslEngine.getUseClientMode()).isTrue();
			assertThat(sslEngine.getEnabledCipherSuites()).containsExactly("TLS_AES_128_GCM_SHA256");
			assertThat(sslEngine.getEnabledProtocols()).containsExactly("TLSv1.3");
			assertThat(sslEngine.getSSLParameters().getEndpointIdentificationAlgorithm()).isEqualTo("https");
		}
	}

	@Test
	void createServerSslEngineAppliesSslOptions() throws IOException {
		SslOptions sslOptions = SslOptions.of(new String[] { "TLS_AES_128_GCM_SHA256" }, new String[] { "TLSv1.3" });
		try (SslBundleSslEngineFactory factory = createFactory(sslOptions)) {
			SSLEngine sslEngine = factory.createServerSslEngine("localhost", 9093);
			assertThat(sslEngine.getUseClientMode()).isFalse();
			assertThat(sslEngine.getEnabledCipherSuites()).containsExactly("TLS_AES_128_GCM_SHA256");
			assertThat(sslEngine.getEnabledProtocols()).containsExactly("TLSv1.3");
		}
	}

	private SslBundleSslEngineFactory createFactory(SslOptions sslOptions) {
		SslBundle sslBundle = SslBundle.of(SslStoreBundle.NONE, SslBundleKey.NONE, sslOptions);
		SslBundleSslEngineFactory factory = new SslBundleSslEngineFactory();
		factory.configure(Map.of(SslBundle.class.getName(), sslBundle));
		return factory;
	}

}
