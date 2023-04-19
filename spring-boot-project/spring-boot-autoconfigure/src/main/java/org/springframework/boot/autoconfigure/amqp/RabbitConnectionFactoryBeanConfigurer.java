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

package org.springframework.boot.autoconfigure.amqp;

import java.time.Duration;

import com.rabbitmq.client.impl.CredentialsProvider;
import com.rabbitmq.client.impl.CredentialsRefreshService;

import org.springframework.amqp.rabbit.connection.RabbitConnectionFactoryBean;
import org.springframework.boot.autoconfigure.amqp.RabbitConnectionDetails.Address;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;

/**
 * Configures {@link RabbitConnectionFactoryBean} with sensible defaults.
 *
 * @author Chris Bono
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 2.6.0
 */
public class RabbitConnectionFactoryBeanConfigurer {

	private final RabbitProperties rabbitProperties;

	private final ResourceLoader resourceLoader;

	private final RabbitConnectionDetails connectionDetails;

	private CredentialsProvider credentialsProvider;

	private CredentialsRefreshService credentialsRefreshService;

	/**
	 * Creates a new configurer that will use the given {@code resourceLoader} and
	 * {@code properties}.
	 * @param resourceLoader the resource loader
	 * @param properties the properties
	 */
	public RabbitConnectionFactoryBeanConfigurer(ResourceLoader resourceLoader, RabbitProperties properties) {
		this(resourceLoader, properties, new PropertiesRabbitConnectionDetails(properties));
	}

	/**
	 * Creates a new configurer that will use the given {@code resourceLoader},
	 * {@code properties}, and {@code connectionDetails}. The connection details have
	 * priority over the properties.
	 * @param resourceLoader the resource loader
	 * @param properties the properties
	 * @param connectionDetails the connection details.
	 * @since 3.1.0
	 */
	public RabbitConnectionFactoryBeanConfigurer(ResourceLoader resourceLoader, RabbitProperties properties,
			RabbitConnectionDetails connectionDetails) {
		Assert.notNull(resourceLoader, "ResourceLoader must not be null");
		Assert.notNull(properties, "Properties must not be null");
		Assert.notNull(connectionDetails, "ConnectionDetails must not be null");
		this.resourceLoader = resourceLoader;
		this.rabbitProperties = properties;
		this.connectionDetails = connectionDetails;
	}

	public void setCredentialsProvider(CredentialsProvider credentialsProvider) {
		this.credentialsProvider = credentialsProvider;
	}

	public void setCredentialsRefreshService(CredentialsRefreshService credentialsRefreshService) {
		this.credentialsRefreshService = credentialsRefreshService;
	}

	/**
	 * Configure the specified rabbit connection factory bean. The factory bean can be
	 * further tuned and default settings can be overridden. It is the responsibility of
	 * the caller to invoke {@link RabbitConnectionFactoryBean#afterPropertiesSet()}
	 * though.
	 * @param factory the {@link RabbitConnectionFactoryBean} instance to configure
	 */
	public void configure(RabbitConnectionFactoryBean factory) {
		Assert.notNull(factory, "RabbitConnectionFactoryBean must not be null");
		factory.setResourceLoader(this.resourceLoader);
		Address address = this.connectionDetails.getFirstAddress();
		PropertyMapper map = PropertyMapper.get();
		map.from(address::host).whenNonNull().to(factory::setHost);
		map.from(address::port).to(factory::setPort);
		map.from(this.connectionDetails::getUsername).whenNonNull().to(factory::setUsername);
		map.from(this.connectionDetails::getPassword).whenNonNull().to(factory::setPassword);
		map.from(this.connectionDetails::getVirtualHost).whenNonNull().to(factory::setVirtualHost);
		map.from(this.rabbitProperties::getRequestedHeartbeat)
			.whenNonNull()
			.asInt(Duration::getSeconds)
			.to(factory::setRequestedHeartbeat);
		map.from(this.rabbitProperties::getRequestedChannelMax).to(factory::setRequestedChannelMax);
		RabbitProperties.Ssl ssl = this.rabbitProperties.getSsl();
		if (ssl.determineEnabled()) {
			factory.setUseSSL(true);
			map.from(ssl::getAlgorithm).whenNonNull().to(factory::setSslAlgorithm);
			map.from(ssl::getKeyStoreType).to(factory::setKeyStoreType);
			map.from(ssl::getKeyStore).to(factory::setKeyStore);
			map.from(ssl::getKeyStorePassword).to(factory::setKeyStorePassphrase);
			map.from(ssl::getKeyStoreAlgorithm).whenNonNull().to(factory::setKeyStoreAlgorithm);
			map.from(ssl::getTrustStoreType).to(factory::setTrustStoreType);
			map.from(ssl::getTrustStore).to(factory::setTrustStore);
			map.from(ssl::getTrustStorePassword).to(factory::setTrustStorePassphrase);
			map.from(ssl::getTrustStoreAlgorithm).whenNonNull().to(factory::setTrustStoreAlgorithm);
			map.from(ssl::isValidateServerCertificate)
				.to((validate) -> factory.setSkipServerCertificateValidation(!validate));
			map.from(ssl::getVerifyHostname).to(factory::setEnableHostnameVerification);
		}
		map.from(this.rabbitProperties::getConnectionTimeout)
			.whenNonNull()
			.asInt(Duration::toMillis)
			.to(factory::setConnectionTimeout);
		map.from(this.rabbitProperties::getChannelRpcTimeout)
			.whenNonNull()
			.asInt(Duration::toMillis)
			.to(factory::setChannelRpcTimeout);
		map.from(this.credentialsProvider).whenNonNull().to(factory::setCredentialsProvider);
		map.from(this.credentialsRefreshService).whenNonNull().to(factory::setCredentialsRefreshService);
	}

}
