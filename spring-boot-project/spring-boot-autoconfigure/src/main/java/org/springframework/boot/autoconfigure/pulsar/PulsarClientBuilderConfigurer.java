/*
 * Copyright 2023-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.pulsar;

import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.pulsar.client.api.ClientBuilder;
import org.apache.pulsar.client.api.ProxyProtocol;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.api.SizeUnit;

import org.springframework.boot.autoconfigure.pulsar.PulsarProperties.Client;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.util.LambdaSafe;
import org.springframework.pulsar.core.PulsarClientBuilderCustomizer;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.unit.DataSize;

/**
 * Configure Pulsar {@link ClientBuilder} with sensible defaults and apply a list of
 * optional {@link PulsarClientBuilderCustomizer customizers}.
 *
 * @author Chris Bono
 * @since 3.2.0
 */
public class PulsarClientBuilderConfigurer {

	private final PulsarProperties properties;

	private final List<PulsarClientBuilderCustomizer> customizers;

	/**
	 * Creates a new configurer that will use the given properties for configuration.
	 * @param properties properties to use
	 * @param customizers list of customizers to apply or empty list if no customizers
	 */
	public PulsarClientBuilderConfigurer(PulsarProperties properties, List<PulsarClientBuilderCustomizer> customizers) {
		Assert.notNull(properties, "properties must not be null");
		Assert.notNull(customizers, "customizers must not be null");
		this.properties = properties;
		this.customizers = customizers;
	}

	/**
	 * Configure the specified {@link ClientBuilder}. The builder can be further tuned and
	 * default settings can be overridden.
	 * @param clientBuilder the {@link ClientBuilder} instance to configure
	 */
	public void configure(ClientBuilder clientBuilder) {
		applyProperties(this.properties, clientBuilder);
		applyCustomizers(this.customizers, clientBuilder);
	}

	@SuppressWarnings("deprecation")
	protected void applyProperties(PulsarProperties pulsarProperties, ClientBuilder clientBuilder) {
		var map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		var clientProperties = pulsarProperties.getClient();
		map.from(clientProperties::getServiceUrl).to(clientBuilder::serviceUrl);
		map.from(clientProperties::getListenerName).to(clientBuilder::listenerName);
		map.from(clientProperties::getNumIoThreads).to(clientBuilder::ioThreads);
		map.from(clientProperties::getNumListenerThreads).to(clientBuilder::listenerThreads);
		map.from(clientProperties::getNumConnectionsPerBroker).to(clientBuilder::connectionsPerBroker);
		map.from(clientProperties::getMaxConcurrentLookupRequest).to(clientBuilder::maxConcurrentLookupRequests);
		map.from(clientProperties::getMaxLookupRequest).to(clientBuilder::maxLookupRequests);
		map.from(clientProperties::getMaxLookupRedirects).to(clientBuilder::maxLookupRedirects);
		map.from(clientProperties::getMaxNumberOfRejectedRequestPerConnection)
			.to(clientBuilder::maxNumberOfRejectedRequestPerConnection);
		map.from(clientProperties::getUseTcpNoDelay).to(clientBuilder::enableTcpNoDelay);

		// Authentication properties
		applyAuthentication(clientProperties, clientBuilder);

		// TLS properties
		map.from(clientProperties::getUseTls).to(clientBuilder::enableTls);
		map.from(clientProperties::getTlsHostnameVerificationEnable).to(clientBuilder::enableTlsHostnameVerification);
		map.from(clientProperties::getTlsTrustCertsFilePath).to(clientBuilder::tlsTrustCertsFilePath);
		map.from(clientProperties::getTlsCertificateFilePath).to(clientBuilder::tlsCertificateFilePath);
		map.from(clientProperties::getTlsKeyFilePath).to(clientBuilder::tlsKeyFilePath);
		map.from(clientProperties::getTlsAllowInsecureConnection).to(clientBuilder::allowTlsInsecureConnection);
		map.from(clientProperties::getUseKeyStoreTls).to(clientBuilder::useKeyStoreTls);
		map.from(clientProperties::getSslProvider).to(clientBuilder::sslProvider);
		map.from(clientProperties::getTlsTrustStoreType).to(clientBuilder::tlsTrustStoreType);
		map.from(clientProperties::getTlsTrustStorePath).to(clientBuilder::tlsTrustStorePath);
		map.from(clientProperties::getTlsTrustStorePassword).to(clientBuilder::tlsTrustStorePassword);
		map.from(clientProperties::getTlsCiphers).to(clientBuilder::tlsCiphers);
		map.from(clientProperties::getTlsProtocols).to(clientBuilder::tlsProtocols);

		map.from(clientProperties::getStatsInterval)
			.as(Duration::toSeconds)
			.to(clientBuilder, (cb, val) -> cb.statsInterval(val, TimeUnit.SECONDS));
		map.from(clientProperties::getKeepAliveInterval)
			.asInt(Duration::toMillis)
			.to(clientBuilder, (cb, val) -> cb.keepAliveInterval(val, TimeUnit.MILLISECONDS));
		map.from(clientProperties::getConnectionTimeout)
			.asInt(Duration::toMillis)
			.to(clientBuilder, (cb, val) -> cb.connectionTimeout(val, TimeUnit.MILLISECONDS));
		map.from(clientProperties::getOperationTimeout)
			.asInt(Duration::toMillis)
			.to(clientBuilder, (cb, val) -> cb.operationTimeout(val, TimeUnit.MILLISECONDS));
		map.from(clientProperties::getLookupTimeout)
			.asInt(Duration::toMillis)
			.to(clientBuilder, (cb, val) -> cb.lookupTimeout(val, TimeUnit.MILLISECONDS));
		map.from(clientProperties::getInitialBackoffInterval)
			.as(Duration::toMillis)
			.to(clientBuilder, (cb, val) -> cb.startingBackoffInterval(val, TimeUnit.MILLISECONDS));
		map.from(clientProperties::getMaxBackoffInterval)
			.as(Duration::toMillis)
			.to(clientBuilder, (cb, val) -> cb.maxBackoffInterval(val, TimeUnit.MILLISECONDS));
		map.from(clientProperties::getEnableBusyWait).to(clientBuilder::enableBusyWait);
		map.from(clientProperties::getMemoryLimit)
			.as(DataSize::toBytes)
			.to(clientBuilder, (cb, val) -> cb.memoryLimit(val, SizeUnit.BYTES));
		map.from(clientProperties::getEnableTransaction).to(clientBuilder::enableTransaction);
		map.from(clientProperties::getProxyServiceUrl)
			.to((proxyUrl) -> clientBuilder.proxyServiceUrl(proxyUrl, ProxyProtocol.SNI));
		map.from(clientProperties::getDnsLookupBindAddress)
			.to((bindAddr) -> clientBuilder.dnsLookupBind(bindAddr, clientProperties.getDnsLookupBindPort()));
		map.from(clientProperties::getSocks5ProxyAddress)
			.as(this::parseSocketAddress)
			.to(clientBuilder::socks5ProxyAddress);
		map.from(clientProperties::getSocks5ProxyUsername).to(clientBuilder::socks5ProxyUsername);
		map.from(clientProperties::getSocks5ProxyPassword).to(clientBuilder::socks5ProxyPassword);
	}

	private void applyAuthentication(Client clientProperties, ClientBuilder clientBuilder) {
		if (StringUtils.hasText(clientProperties.getAuthParams())
				&& !CollectionUtils.isEmpty(clientProperties.getAuthentication())) {
			throw new IllegalArgumentException(
					"Cannot set both spring.pulsar.client.authParams and spring.pulsar.client.authentication.*");
		}
		var authPluginClass = clientProperties.getAuthPluginClassName();
		if (StringUtils.hasText(authPluginClass)) {
			var authParams = clientProperties.getAuthParams();
			if (clientProperties.getAuthentication() != null) {
				authParams = AuthParameterUtils.maybeConvertToEncodedParamString(clientProperties.getAuthentication());
			}
			try {
				clientBuilder.authentication(authPluginClass, authParams);
			}
			catch (PulsarClientException.UnsupportedAuthenticationException ex) {
				throw new IllegalArgumentException("Unable to configure authentication: " + ex.getMessage(), ex);
			}
		}
	}

	private InetSocketAddress parseSocketAddress(String address) {
		try {
			var uri = URI.create(address);
			return new InetSocketAddress(uri.getHost(), uri.getPort());
		}
		catch (Exception ex) {
			throw new IllegalArgumentException("Invalid address: " + ex.getMessage(), ex);
		}
	}

	protected void applyCustomizers(List<PulsarClientBuilderCustomizer> clientBuilderCustomizers,
			ClientBuilder clientBuilder) {
		LambdaSafe.callbacks(PulsarClientBuilderCustomizer.class, clientBuilderCustomizers, clientBuilder)
			.withLogger(PulsarClientBuilderConfigurer.class)
			.invoke((customizer) -> customizer.customize(clientBuilder));
	}

}
