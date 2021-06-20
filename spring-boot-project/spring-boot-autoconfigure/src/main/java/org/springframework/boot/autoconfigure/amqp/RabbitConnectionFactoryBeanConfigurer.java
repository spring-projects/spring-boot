package org.springframework.boot.autoconfigure.amqp;

import java.time.Duration;

import com.rabbitmq.client.impl.CredentialsProvider;
import com.rabbitmq.client.impl.CredentialsRefreshService;

import org.springframework.amqp.rabbit.connection.RabbitConnectionFactoryBean;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;

/**
 * Configures {@link RabbitConnectionFactoryBean} with sensible defaults.
 *
 * @author Chris Bono
 * @since 2.6.0
 */
public class RabbitConnectionFactoryBeanConfigurer {

	private RabbitProperties rabbitProperties;

	private ResourceLoader resourceLoader;

	private CredentialsProvider credentialsProvider;

	private CredentialsRefreshService credentialsRefreshService;

	public RabbitProperties getRabbitProperties() {
		return rabbitProperties;
	}

	public void setRabbitProperties(RabbitProperties rabbitProperties) {
		this.rabbitProperties = rabbitProperties;
	}

	public ResourceLoader getResourceLoader() {
		return resourceLoader;
	}

	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	public CredentialsProvider getCredentialsProvider() {
		return credentialsProvider;
	}

	public void setCredentialsProvider(CredentialsProvider credentialsProvider) {
		this.credentialsProvider = credentialsProvider;
	}

	public CredentialsRefreshService getCredentialsRefreshService() {
		return credentialsRefreshService;
	}

	public void setCredentialsRefreshService(CredentialsRefreshService credentialsRefreshService) {
		this.credentialsRefreshService = credentialsRefreshService;
	}

	/**
	 * Configure the specified rabbit connection factory bean. The factory bean can be
	 * further tuned and default settings can be overridden. It is the repsonsiblity of
	 * the caller to invoke {@link RabbitConnectionFactoryBean#afterPropertiesSet()}
	 * though.
	 * @param factory the {@link RabbitConnectionFactoryBean} instance to configure
	 */
	public void configure(RabbitConnectionFactoryBean factory) {
		Assert.notNull(factory, "RabbitConnectionFactoryBean must not be null");
		factory.setResourceLoader(resourceLoader);
		PropertyMapper map = PropertyMapper.get();
		map.from(this.rabbitProperties::determineHost).whenNonNull().to(factory::setHost);
		map.from(this.rabbitProperties::determinePort).to(factory::setPort);
		map.from(this.rabbitProperties::determineUsername).whenNonNull().to(factory::setUsername);
		map.from(this.rabbitProperties::determinePassword).whenNonNull().to(factory::setPassword);
		map.from(this.rabbitProperties::determineVirtualHost).whenNonNull().to(factory::setVirtualHost);
		map.from(this.rabbitProperties::getRequestedHeartbeat).whenNonNull().asInt(Duration::getSeconds)
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
		map.from(this.rabbitProperties::getConnectionTimeout).whenNonNull().asInt(Duration::toMillis)
				.to(factory::setConnectionTimeout);
		map.from(this.rabbitProperties::getChannelRpcTimeout).whenNonNull().asInt(Duration::toMillis)
				.to(factory::setChannelRpcTimeout);
		map.from(credentialsProvider).whenNonNull().to(factory::setCredentialsProvider);
		map.from(credentialsRefreshService).whenNonNull().to(factory::setCredentialsRefreshService);
	}

}
