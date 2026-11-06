/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.amqp.rabbitmq.autoconfigure;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Adapts {@link AmqpRabbitProperties} to {@link AmqpRabbitConnectionDetails}.
 *
 * @author Eddú Meléndez
 * @author Mohamed Aziz Hadjkacem
 * @author Phillip Webb
 */
class PropertiesAmqpRabbitConnectionDetails implements AmqpRabbitConnectionDetails {

	private final AmqpRabbitProperties properties;

	private final @Nullable SslBundles sslBundles;

	PropertiesAmqpRabbitConnectionDetails(AmqpRabbitProperties properties, @Nullable SslBundles sslBundles) {
		this.properties = properties;
		this.sslBundles = sslBundles;
	}

	@Override
	public Address getAddress() {
		RabbitUri primaryUri = primaryUri();
		boolean secure = StringUtils.hasText(this.properties.getSsl().getBundle());
		if (RabbitUri.NONE.equals(primaryUri)) {
			String host = this.properties.getHost();
			Integer port = this.properties.getPort();
			Assert.state(host != null, "Missing RabbitMQ host");
			return new Address(host, (port != null) ? port : RabbitUri.NONE.determineOrDefaultPort(secure));
		}
		return new Address(primaryUri.determineHost(null), primaryUri.determineOrDefaultPort(secure));
	}

	@Override
	public @Nullable String getUsername() {
		return primaryUri().determineUsername(this.properties.getUsername());
	}

	@Override
	public @Nullable String getPassword() {
		return primaryUri().determinePassword(this.properties.getPassword());
	}

	@Override
	public @Nullable String getVirtualHost() {
		return primaryUri().determineVirtualHost(this.properties.getVirtualHost());
	}

	@Override
	public @Nullable SslBundle getSslBundle() {
		String bundle = this.properties.getSsl().getBundle();
		if (StringUtils.hasLength(bundle)) {
			Assert.notNull(this.sslBundles, "SSL bundle name has been set but no SSL bundles found in context");
			return this.sslBundles.getBundle(bundle);
		}
		return null;
	}

	private RabbitUri primaryUri() {
		return RabbitUri.of(this.properties.getAddress());
	}

}
