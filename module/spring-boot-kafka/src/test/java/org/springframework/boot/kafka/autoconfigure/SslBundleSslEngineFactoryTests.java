/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.kafka.autoconfigure;

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
	void createClientSslEngineAppliesSslOptions() {
		SSLEngine sslEngine = createFactory().createClientSslEngine("localhost", 9093, "https");
		assertThat(sslEngine.getUseClientMode()).isTrue();
		assertThat(sslEngine.getEnabledCipherSuites()).containsExactly("TLS_AES_128_GCM_SHA256");
		assertThat(sslEngine.getEnabledProtocols()).containsExactly("TLSv1.3");
		assertThat(sslEngine.getSSLParameters().getEndpointIdentificationAlgorithm()).isEqualTo("https");
	}

	@Test
	void createServerSslEngineAppliesSslOptions() {
		SSLEngine sslEngine = createFactory().createServerSslEngine("localhost", 9093);
		assertThat(sslEngine.getUseClientMode()).isFalse();
		assertThat(sslEngine.getEnabledCipherSuites()).containsExactly("TLS_AES_128_GCM_SHA256");
		assertThat(sslEngine.getEnabledProtocols()).containsExactly("TLSv1.3");
	}

	private SslBundleSslEngineFactory createFactory() {
		SslOptions options = SslOptions.of(new String[] { "TLS_AES_128_GCM_SHA256" }, new String[] { "TLSv1.3" });
		SslBundle sslBundle = SslBundle.of(SslStoreBundle.NONE, SslBundleKey.NONE, options);
		SslBundleSslEngineFactory factory = new SslBundleSslEngineFactory();
		factory.configure(Map.of(SslBundle.class.getName(), sslBundle));
		return factory;
	}

}
