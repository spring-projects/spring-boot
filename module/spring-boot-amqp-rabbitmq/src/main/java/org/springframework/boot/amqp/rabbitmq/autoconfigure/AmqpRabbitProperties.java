/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.amqp.rabbitmq.autoconfigure;

import java.time.Duration;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.source.InvalidConfigurationPropertyValueException;
import org.springframework.util.StringUtils;

/**
 * Configuration properties for Rabbit AMQP.
 *
 * @author Eddú Meléndez
 * @since 4.2.0
 */
@ConfigurationProperties("spring.amqp.rabbitmq")
public class AmqpRabbitProperties {

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
	 * SSL configuration.
	 */
	private final Ssl ssl = new Ssl();

	/**
	 * Listener container configuration.
	 */
	private final Listener listener = new Listener();

	/**
	 * Template configuration.
	 */
	private final Template template = new Template();

	public String getHost() {
		return this.host;
	}

	public void setHost(String host) {
		if (host.contains(",")) {
			throw new InvalidConfigurationPropertyValueException("spring.amqp.rabbitmq.host", this.host,
					"Invalid character ','. Value must be a single host. For multiple hosts, use property 'spring.amqp.address' instead.");
		}
		this.host = host;
	}

	public @Nullable Integer getPort() {
		return this.port;
	}

	public void setPort(@Nullable Integer port) {
		this.port = port;
	}

	public @Nullable String getAddress() {
		return this.address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getUsername() {
		return this.username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return this.password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public @Nullable String getVirtualHost() {
		return this.virtualHost;
	}

	public void setVirtualHost(@Nullable String virtualHost) {
		this.virtualHost = StringUtils.hasText(virtualHost) ? virtualHost : "/";
	}

	public Ssl getSsl() {
		return this.ssl;
	}

	public Listener getListener() {
		return this.listener;
	}

	public Template getTemplate() {
		return this.template;
	}

	/**
	 * SSL configuration.
	 */
	public static class Ssl {

		/**
		 * SSL bundle name.
		 */
		private @Nullable String bundle;

		public @Nullable String getBundle() {
			return this.bundle;
		}

		public void setBundle(@Nullable String bundle) {
			this.bundle = bundle;
		}

	}

	public static class Listener {

		/**
		 * Whether to enable observation.
		 */
		private boolean observationEnabled;

		private final Batch batch = new Batch();

		public boolean isObservationEnabled() {
			return this.observationEnabled;
		}

		public void setObservationEnabled(boolean observationEnabled) {
			this.observationEnabled = observationEnabled;
		}

		public Batch getBatch() {
			return this.batch;
		}

		public static class Batch {

			/**
			 * Batch size, expressed as the number of physical messages, to be used by the
			 * container.
			 */
			private @Nullable Integer size;

			/**
			 * Timeout for gathering batch messages.
			 */
			private Duration receiveTimeout = Duration.ofSeconds(30);

			public @Nullable Integer getSize() {
				return this.size;
			}

			public void setSize(@Nullable Integer size) {
				this.size = size;
			}

			public Duration getReceiveTimeout() {
				return this.receiveTimeout;
			}

			public void setReceiveTimeout(Duration receiveTimeout) {
				this.receiveTimeout = receiveTimeout;
			}

		}

	}

	public static class Template {

		/**
		 * Name of the default exchange to use for send operations.
		 */
		private @Nullable String exchange;

		/**
		 * Value of a default routing key to use for send operations.
		 */
		private @Nullable String routingKey;

		/**
		 * Name of the default queue to receive messages from when none is specified
		 * explicitly.
		 */
		private @Nullable String defaultReceiveQueue;

		public @Nullable String getExchange() {
			return this.exchange;
		}

		public void setExchange(@Nullable String exchange) {
			this.exchange = exchange;
		}

		public @Nullable String getRoutingKey() {
			return this.routingKey;
		}

		public void setRoutingKey(@Nullable String routingKey) {
			this.routingKey = routingKey;
		}

		public @Nullable String getDefaultReceiveQueue() {
			return this.defaultReceiveQueue;
		}

		public void setDefaultReceiveQueue(@Nullable String defaultReceiveQueue) {
			this.defaultReceiveQueue = defaultReceiveQueue;
		}

	}

}
