/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.amqp.rabbitmq.autoconfigure;

import java.util.function.Function;

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
		ParsedAddress parsedAddress = ParsedAddress.of(this.properties.getAddress());
		String host = get(parsedAddress, ParsedAddress::host, AmqpRabbitProperties::getHost);
		Integer port = get(parsedAddress, ParsedAddress::port, AmqpRabbitProperties::getPort);
		Assert.state(host != null, "Missing RabbitMQ host");
		return new Address(host, (port != null) ? port : defaultPort(parsedAddress));
	}

	private int defaultPort(ParsedAddress parsedAddress) {
		boolean secure = (parsedAddress.secure() != null) ? parsedAddress.secure()
				: StringUtils.hasText(this.properties.getSsl().getBundle());
		return (!secure) ? Address.DEFAULT_PORT : Address.DEFAULT_SECURE_PORT;
	}

	@Override
	public @Nullable String getUsername() {
		return get(ParsedAddress::username, AmqpRabbitProperties::getUsername);
	}

	@Override
	public @Nullable String getPassword() {
		return get(ParsedAddress::password, AmqpRabbitProperties::getPassword);
	}

	@Override
	public @Nullable String getVirtualHost() {
		return get(ParsedAddress::virtualHost, AmqpRabbitProperties::getVirtualHost);
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

	private <T> @Nullable T get(Function<ParsedAddress, ? extends @Nullable T> primary,
			Function<AmqpRabbitProperties, ? extends @Nullable T> fallback) {
		return get(ParsedAddress.of(this.properties.getAddress()), primary, fallback);
	}

	private <T> @Nullable T get(ParsedAddress parsedAddress, Function<ParsedAddress, ? extends @Nullable T> primary,
			Function<AmqpRabbitProperties, ? extends @Nullable T> fallback) {
		T result = primary.apply(parsedAddress);
		return (result != null) ? result : fallback.apply(this.properties);
	}

	record ParsedAddress(@Nullable Boolean secure, @Nullable String host, @Nullable Integer port,
			@Nullable String username, @Nullable String password, @Nullable String virtualHost) {

		private static final String PREFIX = "amqp://";

		private static final String PREFIX_SECURE = "amqps://";

		private static final String DEFAULT_VIRTUAL_HOST = "/";

		private static final ParsedAddress NONE = new ParsedAddress(null, null, null, null, null, null);

		static ParsedAddress of(@Nullable String value) {
			if (!StringUtils.hasText(value)) {
				return NONE;
			}
			value = value.trim();
			Boolean secure = null;
			if (value.startsWith(PREFIX)) {
				secure = false;
				value = value.substring(PREFIX.length());
			}
			else if (value.startsWith(PREFIX_SECURE)) {
				secure = true;
				value = value.substring(PREFIX_SECURE.length());
			}
			String[] credentialsAndConnection = StringUtils.split(value, "@");
			String[] usernameAndPassword = (credentialsAndConnection != null)
					? StringUtils.split(credentialsAndConnection[0], ":") : null;
			String username = (usernameAndPassword != null) ? usernameAndPassword[0] : null;
			if (username == null && credentialsAndConnection != null) {
				username = credentialsAndConnection[0];
			}
			String password = (usernameAndPassword != null) ? usernameAndPassword[1] : null;
			String connection = (credentialsAndConnection != null) ? credentialsAndConnection[1] : value;
			String virtualHost = null;
			int virtualHostIndex = connection.indexOf('/');
			if (virtualHostIndex >= 0) {
				virtualHost = connection.substring(virtualHostIndex + 1);
				virtualHost = (!virtualHost.isEmpty()) ? virtualHost : DEFAULT_VIRTUAL_HOST;
				connection = connection.substring(0, virtualHostIndex);
			}
			int bracketIndex = connection.lastIndexOf(']');
			int colonIndex = connection.lastIndexOf(':');
			if (colonIndex == -1 || colonIndex < bracketIndex) {
				return new ParsedAddress(secure, connection, null, username, password, virtualHost);
			}
			String host = connection.substring(0, colonIndex);
			Integer port = Integer.parseInt(connection.substring(colonIndex + 1));
			return new ParsedAddress(secure, host, port, username, password, virtualHost);
		}

	}

}
