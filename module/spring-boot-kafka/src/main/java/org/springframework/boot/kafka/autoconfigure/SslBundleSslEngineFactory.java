/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.kafka.autoconfigure;

import java.io.IOException;
import java.security.KeyStore;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;

import org.apache.kafka.common.security.auth.SslEngineFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.ssl.SslBundle;
import org.springframework.util.Assert;

/**
 * An {@link SslEngineFactory} that configures creates an {@link SSLEngine} from an
 * {@link SslBundle}.
 *
 * @author Andy Wilkinson
 * @author Scott Frederick
 * @since 4.0.0
 */
public class SslBundleSslEngineFactory implements SslEngineFactory {

	private static final String SSL_BUNDLE_CONFIG_NAME = SslBundle.class.getName();

	private @Nullable Map<String, ?> configs;

	private volatile @Nullable SslBundle sslBundle;

	@Override
	public void configure(Map<String, ?> configs) {
		this.configs = configs;
		this.sslBundle = (SslBundle) configs.get(SSL_BUNDLE_CONFIG_NAME);
	}

	@Override
	public void close() throws IOException {

	}

	@Override
	public SSLEngine createClientSslEngine(String peerHost, int peerPort, String endpointIdentification) {
		SslBundle sslBundle = this.sslBundle;
		Assert.state(sslBundle != null, "'sslBundle' must not be null");
		SSLEngine sslEngine = sslBundle.createSslContext().createSSLEngine(peerHost, peerPort);
		sslEngine.setUseClientMode(true);
		SSLParameters sslParams = sslEngine.getSSLParameters();
		sslParams.setEndpointIdentificationAlgorithm(endpointIdentification);
		sslEngine.setSSLParameters(sslParams);
		return sslEngine;
	}

	@Override
	public SSLEngine createServerSslEngine(String peerHost, int peerPort) {
		SslBundle sslBundle = this.sslBundle;
		Assert.state(sslBundle != null, "'sslBundle' must not be null");
		SSLEngine sslEngine = sslBundle.createSslContext().createSSLEngine(peerHost, peerPort);
		sslEngine.setUseClientMode(false);
		return sslEngine;
	}

	@Override
	public boolean shouldBeRebuilt(Map<String, Object> nextConfigs) {
		return !nextConfigs.equals(this.configs);
	}

	@Override
	public Set<String> reconfigurableConfigs() {
		return Set.of(SSL_BUNDLE_CONFIG_NAME);
	}

	@Override
	public @Nullable KeyStore keystore() {
		SslBundle sslBundle = this.sslBundle;
		Assert.state(sslBundle != null, "'sslBundle' must not be null");
		return sslBundle.getStores().getKeyStore();
	}

	@Override
	public @Nullable KeyStore truststore() {
		SslBundle sslBundle = this.sslBundle;
		Assert.state(sslBundle != null, "'sslBundle' must not be null");
		return sslBundle.getStores().getTrustStore();
	}

}
