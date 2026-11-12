/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.rabbitmq.autoconfigure;

import java.nio.charset.StandardCharsets;

import org.jspecify.annotations.Nullable;

import org.springframework.lang.Contract;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Internal class used to parse an <a href="https://www.rabbitmq.com/docs/uri-spec">AMQP
 * URI</a>.
 *
 * @author Phillip Webb
 * @param secure {@link Boolean} indicating if the connection is secure or {@code null} if
 * no scheme is in the URI.
 * @param host the host or {@code null} if not specified
 * @param port the port or {@code null} if not specified
 * @param username the username or {@code null} if not specified
 * @param password the password or {@code null} if not specified
 * @param virtualHost the virtual host, {@code '/'} for the default virtual host or
 * {@code null} if not specified
 */
record RabbitUri(@Nullable Boolean secure, @Nullable String host, @Nullable Integer port, @Nullable String username,
		@Nullable String password, @Nullable String virtualHost) {

	private static final String PREFIX = "amqp://";

	private static final String PREFIX_SECURE = "amqps://";

	private static final String DEFAULT_VIRTUAL_HOST = "/";

	static final RabbitUri NONE = new RabbitUri(null, null, null, null, null, null);

	boolean determineSecure(@Nullable Boolean fallbackSecure) {
		return Boolean.TRUE.equals(valueOr(secure(), fallbackSecure));
	}

	String determineHost(@Nullable String fallbackHost) {
		String host = valueOr(host(), fallbackHost);
		Assert.state(StringUtils.hasText(host), "Unable to determine RabbitMQ host");
		return host;
	}

	@Nullable String determineUsername(@Nullable String fallbackUsername) {
		return valueOr(username(), fallbackUsername);
	}

	@Nullable String determinePassword(@Nullable String fallbackPassword) {
		return valueOr(password(), fallbackPassword);
	}

	int determineOrDefaultPort(boolean secure) {
		int defaultPort = (!valueOr(secure(), secure)) ? 5672 : 5671;
		return valueOr(determinePort(null), defaultPort);
	}

	@Nullable Integer determinePort(@Nullable Integer fallbackPort) {
		return valueOr(port(), fallbackPort);
	}

	public @Nullable String determineVirtualHost(@Nullable String fallbackVirtualHost) {
		return valueOr(virtualHost(), fallbackVirtualHost);
	}

	@Contract("_, null -> null; _, !null -> !null")
	private <T> @Nullable T valueOr(@Nullable T value, @Nullable T fallbackValue) {
		return (value != null) ? value : fallbackValue;
	}

	static RabbitUri of(@Nullable String value) {
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
			return new RabbitUri(secure, connection, null, decode(username), decode(password), decode(virtualHost));
		}
		String host = connection.substring(0, colonIndex);
		Integer port = Integer.parseInt(connection.substring(colonIndex + 1));
		Assert.state(StringUtils.hasText(host), "Missing RabbitMQ host");
		return new RabbitUri(secure, host, port, decode(username), decode(password), decode(virtualHost));
	}

	@Contract("null -> null; !null -> !null")
	private static @Nullable String decode(@Nullable String value) {
		return (value != null) ? StringUtils.uriDecode(value, StandardCharsets.UTF_8) : null;
	}

}
