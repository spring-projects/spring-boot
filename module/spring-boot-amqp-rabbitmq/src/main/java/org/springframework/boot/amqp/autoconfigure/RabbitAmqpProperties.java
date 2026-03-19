/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.amqp.autoconfigure;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.source.InvalidConfigurationPropertyValueException;
import org.springframework.util.StringUtils;

/**
 * Configuration properties for Rabbit AMQP.
 *
 * @author Eddú Meléndez
 * @since 4.1.0
 */
@ConfigurationProperties("spring.amqp.rabbitmq")
public class RabbitAmqpProperties {

	private static final int DEFAULT_PORT = 5672;

	/**
	 * RabbitMQ host. Ignored if an address is set.
	 */
	private String host = "localhost";

	/**
	 * RabbitMQ port. Ignored if an address is set. Default to 5672, or 5671 if SSL is
	 * enabled.
	 */
	private @Nullable Integer port;

	/**
	 * Login user to authenticate to the broker.
	 */
	private String username = "guest";

	/**
	 * Login to authenticate against the broker.
	 */
	private String password = "guest";

	/**
	 * Virtual host to use when connecting to the broker.
	 */
	private @Nullable String virtualHost;

	/**
	 * The address to which the client should connect. When set, the host and port are
	 * ignored.
	 */
	private @Nullable String address;

	/**
	 * Listener container configuration.
	 */
	private final Listener listener = new Listener();

	private final Template template = new Template();

	private @Nullable Address parsedAddress;

	public String getHost() {
		return this.host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public @Nullable Integer getPort() {
		return this.port;
	}

	/**
	 * Returns the port from the address, or the configured port if no address have been
	 * set.
	 * @return the port
	 * @see #setAddress(String)
	 * @see #getPort()
	 */
	public int determinePort() {
		if (this.parsedAddress == null) {
			Integer port = getPort();
			if (port != null) {
				return port;
			}
			return DEFAULT_PORT;
		}
		return this.parsedAddress.port;
	}

	public void setPort(@Nullable Integer port) {
		this.port = port;
	}

	public @Nullable String getAddress() {
		return this.address;
	}

	/**
	 * Returns the configured address ({@code host:port}) created from the configured host
	 * and port.
	 * @return the address
	 */
	public String determineAddress() {
		if (this.parsedAddress == null) {
			if (this.host.contains(",")) {
				throw new InvalidConfigurationPropertyValueException("spring.amqp.host", this.host,
						"Invalid character ','. Value must be a single host. For multiple hosts, use property 'spring.amqp.address' instead.");
			}
			return this.host + ":" + determinePort();
		}
		return this.parsedAddress.host + ":" + this.parsedAddress.port;
	}

	public void setAddress(String address) {
		this.address = address;
		this.parsedAddress = parseAddress(address);
	}

	private Address parseAddress(String address) {
		return new Address(address);
	}

	public String getUsername() {
		return this.username;
	}

	/**
	 * If address has been set and has a username it is returned. Otherwise returns the
	 * result of calling {@code getUsername()}.
	 * @return the username
	 * @see #setAddress(String)
	 * @see #getUsername()
	 */
	public String determineUsername() {
		if (this.parsedAddress == null) {
			return this.username;
		}
		Address address = this.parsedAddress;
		return (address.username != null) ? address.username : this.username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return this.password;
	}

	/**
	 * If address has been set and has a password it is returned. Otherwise returns the
	 * result of calling {@code getPassword()}.
	 * @return the password or {@code null}
	 * @see #setAddress(String)
	 * @see #getPassword()
	 */
	public @Nullable String determinePassword() {
		if (this.parsedAddress == null) {
			return getPassword();
		}
		Address address = this.parsedAddress;
		return (address.password != null) ? address.password : getPassword();
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public @Nullable String getVirtualHost() {
		return this.virtualHost;
	}

	/**
	 * If address has been set and has a virtual host it is returned. Otherwise returns
	 * the result of calling {@code getVirtualHost()}.
	 * @return the virtual host or {@code null}
	 * @see #setAddress(String)
	 * @see #getVirtualHost()
	 */
	public @Nullable String determineVirtualHost() {
		if (this.parsedAddress == null) {
			return getVirtualHost();
		}
		Address address = this.parsedAddress;
		return (address.virtualHost != null) ? address.virtualHost : getVirtualHost();
	}

	public void setVirtualHost(@Nullable String virtualHost) {
		this.virtualHost = StringUtils.hasText(virtualHost) ? virtualHost : "/";
	}

	public Listener getListener() {
		return this.listener;
	}

	public Template getTemplate() {
		return this.template;
	}

	public static class Listener {

		private final AmqpContainer amqp = new AmqpContainer();

		public AmqpContainer getAmqp() {
			return this.amqp;
		}

	}

	/**
	 * Configuration properties for {@code RabbitAmqpListenerContainer}.
	 */
	public static class AmqpContainer {

		/**
		 * Whether to enable observation.
		 */
		private boolean observationEnabled;

		/**
		 * Batch size, expressed as the number of physical messages, to be used by the
		 * container.
		 */
		private @Nullable Integer batchSize;

		public boolean isObservationEnabled() {
			return this.observationEnabled;
		}

		public void setObservationEnabled(boolean observationEnabled) {
			this.observationEnabled = observationEnabled;
		}

		public @Nullable Integer getBatchSize() {
			return this.batchSize;
		}

		public void setBatchSize(@Nullable Integer batchSize) {
			this.batchSize = batchSize;
		}

	}

	public static class Template {

		/**
		 * Name of the default exchange to use for send operations.
		 */
		private String exchange = "";

		/**
		 * Value of a default routing key to use for send operations.
		 */
		private String routingKey = "";

		/**
		 * Name of the default queue to receive messages from when none is specified
		 * explicitly.
		 */
		private @Nullable String defaultReceiveQueue;

		public String getExchange() {
			return this.exchange;
		}

		public void setExchange(String exchange) {
			this.exchange = exchange;
		}

		public String getRoutingKey() {
			return this.routingKey;
		}

		public void setRoutingKey(String routingKey) {
			this.routingKey = routingKey;
		}

		public @Nullable String getDefaultReceiveQueue() {
			return this.defaultReceiveQueue;
		}

		public void setDefaultReceiveQueue(@Nullable String defaultReceiveQueue) {
			this.defaultReceiveQueue = defaultReceiveQueue;
		}

	}

	private static final class Address {

		private static final String PREFIX_AMQP = "amqp://";

		private static final String PREFIX_AMQP_SECURE = "amqps://";

		private String host;

		private int port;

		private @Nullable String username;

		private @Nullable String password;

		private @Nullable String virtualHost;

		private Address(String input) {
			input = input.trim();
			input = trimPrefix(input);
			input = parseUsernameAndPassword(input);
			input = parseVirtualHost(input);
			parseHostAndPort(input);
		}

		private String trimPrefix(String input) {
			if (input.startsWith(PREFIX_AMQP_SECURE)) {
				return input.substring(PREFIX_AMQP_SECURE.length());
			}
			if (input.startsWith(PREFIX_AMQP)) {
				return input.substring(PREFIX_AMQP.length());
			}
			return input;
		}

		private String parseUsernameAndPassword(String input) {
			String[] splitInput = StringUtils.split(input, "@");
			if (splitInput == null) {
				return input;
			}
			String credentials = splitInput[0];
			String[] splitCredentials = StringUtils.split(credentials, ":");
			if (splitCredentials == null) {
				this.username = credentials;
			}
			else {
				this.username = splitCredentials[0];
				this.password = splitCredentials[1];
			}
			return splitInput[1];
		}

		private String parseVirtualHost(String input) {
			int hostIndex = input.indexOf('/');
			if (hostIndex >= 0) {
				this.virtualHost = input.substring(hostIndex + 1);
				if (this.virtualHost.isEmpty()) {
					this.virtualHost = "/";
				}
				input = input.substring(0, hostIndex);
			}
			return input;
		}

		private void parseHostAndPort(String input) {
			int bracketIndex = input.lastIndexOf(']');
			int colonIndex = input.lastIndexOf(':');
			if (colonIndex == -1 || colonIndex < bracketIndex) {
				this.host = input;
				this.port = DEFAULT_PORT;
			}
			else {
				this.host = input.substring(0, colonIndex);
				this.port = Integer.parseInt(input.substring(colonIndex + 1));
			}
		}

	}

}
