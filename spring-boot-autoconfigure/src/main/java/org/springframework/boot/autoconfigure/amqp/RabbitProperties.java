/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.amqp;

import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory.CacheMode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.util.StringUtils;

/**
 * Configuration properties for Rabbit.
 *
 * @author Greg Turnquist
 * @author Dave Syer
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author Josh Thornhill
 * @author Gary Russell
 */
@ConfigurationProperties(prefix = "spring.rabbitmq")
public class RabbitProperties {

	/**
	 * RabbitMQ host.
	 */
	private String host = "localhost";

	/**
	 * RabbitMQ port.
	 */
	private int port = 5672;

	/**
	 * Login user to authenticate to the broker.
	 */
	private String username;

	/**
	 * Login to authenticate against the broker.
	 */
	private String password;

	/**
	 * SSL configuration.
	 */
	private final Ssl ssl = new Ssl();

	/**
	 * Virtual host to use when connecting to the broker.
	 */
	private String virtualHost;

	/**
	 * Comma-separated list of addresses to which the client should connect.
	 */
	private String addresses;

	/**
	 * Requested heartbeat timeout, in seconds; zero for none.
	 */
	private Integer requestedHeartbeat;

	/**
	 * Enable publisher confirms.
	 */
	private boolean publisherConfirms;

	/**
	 * Enable publisher returns.
	 */
	private boolean publisherReturns;

	/**
	 * Connection timeout, in milliseconds; zero for infinite.
	 */
	private Integer connectionTimeout;

	/**
	 * Cache configuration.
	 */
	private final Cache cache = new Cache();

	/**
	 * Listener container configuration.
	 */
	private final Listener listener = new Listener();

	private final Template template = new Template();

	public String getHost() {
		if (this.addresses == null) {
			return this.host;
		}
		String[] hosts = StringUtils.delimitedListToStringArray(this.addresses, ":");
		if (hosts.length == 2) {
			return hosts[0];
		}
		return null;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		if (this.addresses == null) {
			return this.port;
		}
		String[] hosts = StringUtils.delimitedListToStringArray(this.addresses, ":");
		if (hosts.length >= 2) {
			return Integer
					.valueOf(StringUtils.commaDelimitedListToStringArray(hosts[1])[0]);
		}
		return this.port;
	}

	public void setAddresses(String addresses) {
		this.addresses = parseAddresses(addresses);
	}

	public String getAddresses() {
		return (this.addresses == null ? this.host + ":" + this.port : this.addresses);
	}

	private String parseAddresses(String addresses) {
		Set<String> result = new LinkedHashSet<String>();
		for (String address : StringUtils.commaDelimitedListToStringArray(addresses)) {
			address = address.trim();
			if (address.startsWith("amqp://")) {
				address = address.substring("amqp://".length());
			}
			if (address.contains("@")) {
				String[] split = StringUtils.split(address, "@");
				String creds = split[0];
				address = split[1];
				split = StringUtils.split(creds, ":");
				this.username = split[0];
				if (split.length > 0) {
					this.password = split[1];
				}
			}
			int index = address.indexOf("/");
			if (index >= 0 && index < address.length()) {
				setVirtualHost(address.substring(index + 1));
				address = address.substring(0, index);
			}
			if (!address.contains(":")) {
				address = address + ":" + this.port;
			}
			result.add(address);
		}
		return (result.isEmpty() ? null
				: StringUtils.collectionToCommaDelimitedString(result));
	}

	public void setPort(int port) {
		this.port = port;
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

	public Ssl getSsl() {
		return this.ssl;
	}

	public String getVirtualHost() {
		return this.virtualHost;
	}

	public void setVirtualHost(String virtualHost) {
		this.virtualHost = ("".equals(virtualHost) ? "/" : virtualHost);
	}

	public Integer getRequestedHeartbeat() {
		return this.requestedHeartbeat;
	}

	public void setRequestedHeartbeat(Integer requestedHeartbeat) {
		this.requestedHeartbeat = requestedHeartbeat;
	}

	public boolean isPublisherConfirms() {
		return this.publisherConfirms;
	}

	public void setPublisherConfirms(boolean publisherConfirms) {
		this.publisherConfirms = publisherConfirms;
	}

	public boolean isPublisherReturns() {
		return this.publisherReturns;
	}

	public void setPublisherReturns(boolean publisherReturns) {
		this.publisherReturns = publisherReturns;
	}

	public Integer getConnectionTimeout() {
		return this.connectionTimeout;
	}

	public void setConnectionTimeout(Integer connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}

	public Cache getCache() {
		return this.cache;
	}

	public Listener getListener() {
		return this.listener;
	}

	public Template getTemplate() {
		return this.template;
	}

	public static class Ssl {

		/**
		 * Enable SSL support.
		 */
		private boolean enabled;

		/**
		 * Path to the key store that holds the SSL certificate.
		 */
		private String keyStore;

		/**
		 * Password used to access the key store.
		 */
		private String keyStorePassword;

		/**
		 * Trust store that holds SSL certificates.
		 */
		private String trustStore;

		/**
		 * Password used to access the trust store.
		 */
		private String trustStorePassword;

		/**
		 * SSL algorithm to use (e.g. TLSv1.1). Default is set automatically by the rabbit
		 * client library.
		 */
		private String algorithm;

		public boolean isEnabled() {
			return this.enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public String getKeyStore() {
			return this.keyStore;
		}

		public void setKeyStore(String keyStore) {
			this.keyStore = keyStore;
		}

		public String getKeyStorePassword() {
			return this.keyStorePassword;
		}

		public void setKeyStorePassword(String keyStorePassword) {
			this.keyStorePassword = keyStorePassword;
		}

		public String getTrustStore() {
			return this.trustStore;
		}

		public void setTrustStore(String trustStore) {
			this.trustStore = trustStore;
		}

		public String getTrustStorePassword() {
			return this.trustStorePassword;
		}

		public void setTrustStorePassword(String trustStorePassword) {
			this.trustStorePassword = trustStorePassword;
		}

		public String getAlgorithm() {
			return this.algorithm;
		}

		public void setAlgorithm(String sslAlgorithm) {
			this.algorithm = sslAlgorithm;
		}

	}

	public static class Cache {

		private final Channel channel = new Channel();

		private final Connection connection = new Connection();

		public Channel getChannel() {
			return this.channel;
		}

		public Connection getConnection() {
			return this.connection;
		}

		public static class Channel {

			/**
			 * Number of channels to retain in the cache. When "check-timeout" > 0, max
			 * channels per connection.
			 */
			private Integer size;

			/**
			 * Number of milliseconds to wait to obtain a channel if the cache size has
			 * been reached. If 0, always create a new channel.
			 */
			private Long checkoutTimeout;

			public Integer getSize() {
				return this.size;
			}

			public void setSize(Integer size) {
				this.size = size;
			}

			public Long getCheckoutTimeout() {
				return this.checkoutTimeout;
			}

			public void setCheckoutTimeout(Long checkoutTimeout) {
				this.checkoutTimeout = checkoutTimeout;
			}

		}

		public static class Connection {

			/**
			 * Connection factory cache mode.
			 */
			private CacheMode mode = CacheMode.CHANNEL;

			/**
			 * Number of connections to cache. Only applies when mode is CONNECTION.
			 */
			private Integer size;

			public CacheMode getMode() {
				return this.mode;
			}

			public void setMode(CacheMode mode) {
				this.mode = mode;
			}

			public Integer getSize() {
				return this.size;
			}

			public void setSize(Integer size) {
				this.size = size;
			}

		}

	}

	public static class Listener {

		/**
		 * Start the container automatically on startup.
		 */
		private boolean autoStartup = true;

		/**
		 * Acknowledge mode of container.
		 */
		private AcknowledgeMode acknowledgeMode;

		/**
		 * Minimum number of consumers.
		 */
		private Integer concurrency;

		/**
		 * Maximum number of consumers.
		 */
		private Integer maxConcurrency;

		/**
		 * Number of messages to be handled in a single request. It should be greater than
		 * or equal to the transaction size (if used).
		 */
		private Integer prefetch;

		/**
		 * Number of messages to be processed in a transaction. For best results it should
		 * be less than or equal to the prefetch count.
		 */
		private Integer transactionSize;

		/**
		 * Whether rejected deliveries are requeued by default; default true.
		 */
		private Boolean defaultRequeueRejected;

		/**
		 * Optional properties for a retry interceptor.
		 */
		@NestedConfigurationProperty
		private final ListenerRetry retry = new ListenerRetry();

		public boolean isAutoStartup() {
			return this.autoStartup;
		}

		public void setAutoStartup(boolean autoStartup) {
			this.autoStartup = autoStartup;
		}

		public AcknowledgeMode getAcknowledgeMode() {
			return this.acknowledgeMode;
		}

		public void setAcknowledgeMode(AcknowledgeMode acknowledgeMode) {
			this.acknowledgeMode = acknowledgeMode;
		}

		public Integer getConcurrency() {
			return this.concurrency;
		}

		public void setConcurrency(Integer concurrency) {
			this.concurrency = concurrency;
		}

		public Integer getMaxConcurrency() {
			return this.maxConcurrency;
		}

		public void setMaxConcurrency(Integer maxConcurrency) {
			this.maxConcurrency = maxConcurrency;
		}

		public Integer getPrefetch() {
			return this.prefetch;
		}

		public void setPrefetch(Integer prefetch) {
			this.prefetch = prefetch;
		}

		public Integer getTransactionSize() {
			return this.transactionSize;
		}

		public void setTransactionSize(Integer transactionSize) {
			this.transactionSize = transactionSize;
		}

		public Boolean getDefaultRequeueRejected() {
			return this.defaultRequeueRejected;
		}

		public void setDefaultRequeueRejected(Boolean defaultRequeueRejected) {
			this.defaultRequeueRejected = defaultRequeueRejected;
		}

		public ListenerRetry getRetry() {
			return this.retry;
		}

	}

	public static class Template {

		@NestedConfigurationProperty
		private final Retry retry = new Retry();

		/**
		 * Enable mandatory messages. If a mandatory message cannot be routed to a queue
		 * by the server, it will return an unroutable message with a Return method.
		 */
		private Boolean mandatory;

		/**
		 * Timeout for receive() operations.
		 */
		private Long receiveTimeout;

		/**
		 * Timeout for sendAndReceive() operations.
		 */
		private Long replyTimeout;

		public Retry getRetry() {
			return this.retry;
		}

		public Boolean getMandatory() {
			return this.mandatory;
		}

		public void setMandatory(Boolean mandatory) {
			this.mandatory = mandatory;
		}

		public Long getReceiveTimeout() {
			return this.receiveTimeout;
		}

		public void setReceiveTimeout(Long receiveTimeout) {
			this.receiveTimeout = receiveTimeout;
		}

		public Long getReplyTimeout() {
			return this.replyTimeout;
		}

		public void setReplyTimeout(Long replyTimeout) {
			this.replyTimeout = replyTimeout;
		}

	}

	public static class Retry {

		/**
		 * Whether or not publishing retries are enabled.
		 */
		private boolean enabled;

		/**
		 * Maximum number of attempts to publish or deliver a message.
		 */
		private int maxAttempts = 3;

		/**
		 * Interval between the first and second attempt to publish or deliver a message.
		 */
		private long initialInterval = 1000L;

		/**
		 * A multiplier to apply to the previous retry interval.
		 */
		private double multiplier = 1.0;

		/**
		 * Maximum interval between attempts.
		 */
		private long maxInterval = 10000L;

		public boolean isEnabled() {
			return this.enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public int getMaxAttempts() {
			return this.maxAttempts;
		}

		public void setMaxAttempts(int maxAttempts) {
			this.maxAttempts = maxAttempts;
		}

		public long getInitialInterval() {
			return this.initialInterval;
		}

		public void setInitialInterval(long initialInterval) {
			this.initialInterval = initialInterval;
		}

		public double getMultiplier() {
			return this.multiplier;
		}

		public void setMultiplier(double multiplier) {
			this.multiplier = multiplier;
		}

		public long getMaxInterval() {
			return this.maxInterval;
		}

		public void setMaxInterval(long maxInterval) {
			this.maxInterval = maxInterval;
		}

	}

	public static class ListenerRetry extends Retry {

		/**
		 * Whether or not retries are stateless or stateful.
		 */
		private boolean stateless = true;

		public boolean isStateless() {
			return this.stateless;
		}

		public void setStateless(boolean stateless) {
			this.stateless = stateless;
		}

	}

}
