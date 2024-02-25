/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.kafka;

import java.io.IOException;
import java.security.KeyStore;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;

import org.apache.kafka.common.security.auth.SslEngineFactory;

import org.springframework.boot.ssl.SslBundle;

/**
 * An {@link SslEngineFactory} that configures creates an {@link SSLEngine} from an
 * {@link SslBundle}.
 *
 * @author Andy Wilkinson
 * @author Scott Frederick
 * @since 3.2.0
 */
public class SslBundleSslEngineFactory implements SslEngineFactory {

	private static final String SSL_BUNDLE_CONFIG_NAME = SslBundle.class.getName();

	private Map<String, ?> configs;

	private volatile SslBundle sslBundle;

	/**
     * Configures the SslBundleSslEngineFactory with the provided configurations.
     * 
     * @param configs A map of configurations for the SslBundleSslEngineFactory.
     */
    @Override
	public void configure(Map<String, ?> configs) {
		this.configs = configs;
		this.sslBundle = (SslBundle) configs.get(SSL_BUNDLE_CONFIG_NAME);
	}

	/**
     * Closes the SSL engine factory.
     *
     * @throws IOException if an I/O error occurs while closing the SSL engine factory.
     */
    @Override
	public void close() throws IOException {

	}

	/**
     * Creates a client SSL engine with the specified peer host, peer port, and endpoint identification algorithm.
     * 
     * @param peerHost The host name of the peer.
     * @param peerPort The port number of the peer.
     * @param endpointIdentification The endpoint identification algorithm to be used.
     * @return The created client SSL engine.
     */
    @Override
	public SSLEngine createClientSslEngine(String peerHost, int peerPort, String endpointIdentification) {
		SSLEngine sslEngine = this.sslBundle.createSslContext().createSSLEngine(peerHost, peerPort);
		sslEngine.setUseClientMode(true);
		SSLParameters sslParams = sslEngine.getSSLParameters();
		sslParams.setEndpointIdentificationAlgorithm(endpointIdentification);
		sslEngine.setSSLParameters(sslParams);
		return sslEngine;
	}

	/**
     * Creates a server SSL engine for the given peer host and port.
     * 
     * @param peerHost the host name of the peer
     * @param peerPort the port number of the peer
     * @return the created server SSL engine
     */
    @Override
	public SSLEngine createServerSslEngine(String peerHost, int peerPort) {
		SSLEngine sslEngine = this.sslBundle.createSslContext().createSSLEngine(peerHost, peerPort);
		sslEngine.setUseClientMode(false);
		return sslEngine;
	}

	/**
     * Determines whether the SslBundleSslEngineFactory should be rebuilt based on the provided nextConfigs.
     * 
     * @param nextConfigs the next set of configurations to compare with the current configurations
     * @return true if the nextConfigs are different from the current configurations, false otherwise
     */
    @Override
	public boolean shouldBeRebuilt(Map<String, Object> nextConfigs) {
		return !nextConfigs.equals(this.configs);
	}

	/**
     * Returns a set of reconfigurable configurations.
     *
     * @return a set of reconfigurable configurations
     */
    @Override
	public Set<String> reconfigurableConfigs() {
		return Set.of(SSL_BUNDLE_CONFIG_NAME);
	}

	/**
     * Returns the KeyStore object associated with this SSL bundle.
     * 
     * @return the KeyStore object
     */
    @Override
	public KeyStore keystore() {
		return this.sslBundle.getStores().getKeyStore();
	}

	/**
     * Returns the truststore used by the SSL engine factory.
     *
     * @return the truststore used by the SSL engine factory
     */
    @Override
	public KeyStore truststore() {
		return this.sslBundle.getStores().getTrustStore();
	}

}
