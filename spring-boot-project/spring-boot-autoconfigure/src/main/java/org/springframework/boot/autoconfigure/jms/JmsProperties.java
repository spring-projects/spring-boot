/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.autoconfigure.jms;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.DeprecatedConfigurationProperty;

/**
 * Configuration properties for JMS.
 *
 * @author Greg Turnquist
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Lasse Wulff
 * @author Vedran Pavic
 * @since 1.0.0
 */
@ConfigurationProperties("spring.jms")
public class JmsProperties {

	/**
	 * Whether the default destination type is topic.
	 */
	private boolean pubSubDomain = false;

	/**
	 * Connection factory JNDI name. When set, takes precedence to others connection
	 * factory auto-configurations.
	 */
	private String jndiName;

	/**
	 * Whether the subscription is durable.
	 */
	private boolean subscriptionDurable = false;

	/**
	 * Client id of the connection.
	 */
	private String clientId;

	private final Cache cache = new Cache();

	private final Listener listener = new Listener();

	private final Template template = new Template();

	public boolean isPubSubDomain() {
		return this.pubSubDomain;
	}

	public void setPubSubDomain(boolean pubSubDomain) {
		this.pubSubDomain = pubSubDomain;
	}

	public boolean isSubscriptionDurable() {
		return this.subscriptionDurable;
	}

	public void setSubscriptionDurable(boolean subscriptionDurable) {
		this.subscriptionDurable = subscriptionDurable;
	}

	public String getClientId() {
		return this.clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getJndiName() {
		return this.jndiName;
	}

	public void setJndiName(String jndiName) {
		this.jndiName = jndiName;
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

	public static class Cache {

		/**
		 * Whether to cache sessions.
		 */
		private boolean enabled = true;

		/**
		 * Whether to cache message consumers.
		 */
		private boolean consumers = false;

		/**
		 * Whether to cache message producers.
		 */
		private boolean producers = true;

		/**
		 * Size of the session cache (per JMS Session type).
		 */
		private int sessionCacheSize = 1;

		public boolean isEnabled() {
			return this.enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public boolean isConsumers() {
			return this.consumers;
		}

		public void setConsumers(boolean consumers) {
			this.consumers = consumers;
		}

		public boolean isProducers() {
			return this.producers;
		}

		public void setProducers(boolean producers) {
			this.producers = producers;
		}

		public int getSessionCacheSize() {
			return this.sessionCacheSize;
		}

		public void setSessionCacheSize(int sessionCacheSize) {
			this.sessionCacheSize = sessionCacheSize;
		}

	}

	public static class Listener {

		/**
		 * Start the container automatically on startup.
		 */
		private boolean autoStartup = true;

		/**
		 * Minimum number of concurrent consumers. When max-concurrency is not specified
		 * the minimum will also be used as the maximum.
		 */
		private Integer minConcurrency;

		/**
		 * Maximum number of concurrent consumers.
		 */
		private Integer maxConcurrency;

		/**
		 * Timeout to use for receive calls. Use -1 for a no-wait receive or 0 for no
		 * timeout at all. The latter is only feasible if not running within a transaction
		 * manager and is generally discouraged since it prevents clean shutdown.
		 */
		private Duration receiveTimeout = Duration.ofSeconds(1);

		/**
		 * Maximum number of messages to process in one task. By default, unlimited unless
		 * a SchedulingTaskExecutor is configured on the listener (10 messages), as it
		 * indicates a preference for short-lived tasks.
		 */
		private Integer maxMessagesPerTask;

		private final Session session = new Session();

		public boolean isAutoStartup() {
			return this.autoStartup;
		}

		public void setAutoStartup(boolean autoStartup) {
			this.autoStartup = autoStartup;
		}

		@Deprecated(since = "3.2.0", forRemoval = true)
		@DeprecatedConfigurationProperty(replacement = "spring.jms.listener.session.acknowledge-mode", since = "3.2.0")
		public AcknowledgeMode getAcknowledgeMode() {
			return this.session.getAcknowledgeMode();
		}

		@Deprecated(since = "3.2.0", forRemoval = true)
		public void setAcknowledgeMode(AcknowledgeMode acknowledgeMode) {
			this.session.setAcknowledgeMode(acknowledgeMode);
		}

		@DeprecatedConfigurationProperty(replacement = "spring.jms.listener.min-concurrency", since = "3.2.0")
		@Deprecated(since = "3.2.0", forRemoval = true)
		public Integer getConcurrency() {
			return this.minConcurrency;
		}

		@Deprecated(since = "3.2.0", forRemoval = true)
		public void setConcurrency(Integer concurrency) {
			this.minConcurrency = concurrency;
		}

		public Integer getMinConcurrency() {
			return this.minConcurrency;
		}

		public void setMinConcurrency(Integer minConcurrency) {
			this.minConcurrency = minConcurrency;
		}

		public Integer getMaxConcurrency() {
			return this.maxConcurrency;
		}

		public void setMaxConcurrency(Integer maxConcurrency) {
			this.maxConcurrency = maxConcurrency;
		}

		public String formatConcurrency() {
			if (this.minConcurrency == null) {
				return (this.maxConcurrency != null) ? "1-" + this.maxConcurrency : null;
			}
			return this.minConcurrency + "-"
					+ ((this.maxConcurrency != null) ? this.maxConcurrency : this.minConcurrency);
		}

		public Duration getReceiveTimeout() {
			return this.receiveTimeout;
		}

		public void setReceiveTimeout(Duration receiveTimeout) {
			this.receiveTimeout = receiveTimeout;
		}

		public Integer getMaxMessagesPerTask() {
			return this.maxMessagesPerTask;
		}

		public void setMaxMessagesPerTask(Integer maxMessagesPerTask) {
			this.maxMessagesPerTask = maxMessagesPerTask;
		}

		public Session getSession() {
			return this.session;
		}

		public static class Session {

			/**
			 * Acknowledge mode of the listener container.
			 */
			private AcknowledgeMode acknowledgeMode = AcknowledgeMode.AUTO;

			/**
			 * Whether the listener container should use transacted JMS sessions. Defaults
			 * to false in the presence of a JtaTransactionManager and true otherwise.
			 */
			private Boolean transacted;

			public AcknowledgeMode getAcknowledgeMode() {
				return this.acknowledgeMode;
			}

			public void setAcknowledgeMode(AcknowledgeMode acknowledgeMode) {
				this.acknowledgeMode = acknowledgeMode;
			}

			public Boolean getTransacted() {
				return this.transacted;
			}

			public void setTransacted(Boolean transacted) {
				this.transacted = transacted;
			}

		}

	}

	public static class Template {

		/**
		 * Default destination to use on send and receive operations that do not have a
		 * destination parameter.
		 */
		private String defaultDestination;

		/**
		 * Delivery delay to use for send calls.
		 */
		private Duration deliveryDelay;

		/**
		 * Delivery mode. Enables QoS (Quality of Service) when set.
		 */
		private DeliveryMode deliveryMode;

		/**
		 * Priority of a message when sending. Enables QoS (Quality of Service) when set.
		 */
		private Integer priority;

		/**
		 * Time-to-live of a message when sending. Enables QoS (Quality of Service) when
		 * set.
		 */
		private Duration timeToLive;

		/**
		 * Whether to enable explicit QoS (Quality of Service) when sending a message.
		 * When enabled, the delivery mode, priority and time-to-live properties will be
		 * used when sending a message. QoS is automatically enabled when at least one of
		 * those settings is customized.
		 */
		private Boolean qosEnabled;

		/**
		 * Timeout to use for receive calls.
		 */
		private Duration receiveTimeout;

		private final Session session = new Session();

		public String getDefaultDestination() {
			return this.defaultDestination;
		}

		public void setDefaultDestination(String defaultDestination) {
			this.defaultDestination = defaultDestination;
		}

		public Duration getDeliveryDelay() {
			return this.deliveryDelay;
		}

		public void setDeliveryDelay(Duration deliveryDelay) {
			this.deliveryDelay = deliveryDelay;
		}

		public DeliveryMode getDeliveryMode() {
			return this.deliveryMode;
		}

		public void setDeliveryMode(DeliveryMode deliveryMode) {
			this.deliveryMode = deliveryMode;
		}

		public Integer getPriority() {
			return this.priority;
		}

		public void setPriority(Integer priority) {
			this.priority = priority;
		}

		public Duration getTimeToLive() {
			return this.timeToLive;
		}

		public void setTimeToLive(Duration timeToLive) {
			this.timeToLive = timeToLive;
		}

		public boolean determineQosEnabled() {
			if (this.qosEnabled != null) {
				return this.qosEnabled;
			}
			return (getDeliveryMode() != null || getPriority() != null || getTimeToLive() != null);
		}

		public Boolean getQosEnabled() {
			return this.qosEnabled;
		}

		public void setQosEnabled(Boolean qosEnabled) {
			this.qosEnabled = qosEnabled;
		}

		public Duration getReceiveTimeout() {
			return this.receiveTimeout;
		}

		public void setReceiveTimeout(Duration receiveTimeout) {
			this.receiveTimeout = receiveTimeout;
		}

		public Session getSession() {
			return this.session;
		}

		public static class Session {

			/**
			 * Acknowledge mode used when creating sessions.
			 */
			private AcknowledgeMode acknowledgeMode = AcknowledgeMode.AUTO;

			/**
			 * Whether to use transacted sessions.
			 */
			private boolean transacted = false;

			public AcknowledgeMode getAcknowledgeMode() {
				return this.acknowledgeMode;
			}

			public void setAcknowledgeMode(AcknowledgeMode acknowledgeMode) {
				this.acknowledgeMode = acknowledgeMode;
			}

			public boolean isTransacted() {
				return this.transacted;
			}

			public void setTransacted(boolean transacted) {
				this.transacted = transacted;
			}

		}

	}

	public enum DeliveryMode {

		/**
		 * Does not require that the message be logged to stable storage. This is the
		 * lowest-overhead delivery mode but can lead to lost of message if the broker
		 * goes down.
		 */
		NON_PERSISTENT(1),

		/*
		 * Instructs the JMS provider to log the message to stable storage as part of the
		 * client's send operation.
		 */
		PERSISTENT(2);

		private final int value;

		DeliveryMode(int value) {
			this.value = value;
		}

		public int getValue() {
			return this.value;
		}

	}

}
