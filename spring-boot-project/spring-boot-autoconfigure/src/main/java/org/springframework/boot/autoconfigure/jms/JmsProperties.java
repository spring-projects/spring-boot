/*
 * Copyright 2012-2024 the original author or authors.
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
@ConfigurationProperties(prefix = "spring.jms")
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

	/**
	 * Returns a boolean value indicating whether the domain is pub/sub.
	 * @return true if the domain is pub/sub, false otherwise
	 */
	public boolean isPubSubDomain() {
		return this.pubSubDomain;
	}

	/**
	 * Sets the value of the pubSubDomain property.
	 * @param pubSubDomain the new value for the pubSubDomain property
	 */
	public void setPubSubDomain(boolean pubSubDomain) {
		this.pubSubDomain = pubSubDomain;
	}

	/**
	 * Returns a boolean value indicating whether the subscription is durable.
	 * @return true if the subscription is durable, false otherwise
	 */
	public boolean isSubscriptionDurable() {
		return this.subscriptionDurable;
	}

	/**
	 * Sets the flag indicating whether the subscription is durable or not.
	 * @param subscriptionDurable true if the subscription is durable, false otherwise
	 */
	public void setSubscriptionDurable(boolean subscriptionDurable) {
		this.subscriptionDurable = subscriptionDurable;
	}

	/**
	 * Returns the client ID.
	 * @return the client ID
	 */
	public String getClientId() {
		return this.clientId;
	}

	/**
	 * Sets the client ID for the JMS properties.
	 * @param clientId the client ID to be set
	 */
	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	/**
	 * Returns the JNDI name.
	 * @return the JNDI name
	 */
	public String getJndiName() {
		return this.jndiName;
	}

	/**
	 * Sets the JNDI name for the JMS properties.
	 * @param jndiName the JNDI name to be set
	 */
	public void setJndiName(String jndiName) {
		this.jndiName = jndiName;
	}

	/**
	 * Returns the cache object.
	 * @return the cache object
	 */
	public Cache getCache() {
		return this.cache;
	}

	/**
	 * Returns the listener associated with this JmsProperties object.
	 * @return the listener associated with this JmsProperties object
	 */
	public Listener getListener() {
		return this.listener;
	}

	/**
	 * Returns the template associated with this JmsProperties object.
	 * @return the template associated with this JmsProperties object
	 */
	public Template getTemplate() {
		return this.template;
	}

	/**
	 * Cache class.
	 */
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

		/**
		 * Returns the current status of the cache.
		 * @return true if the cache is enabled, false otherwise.
		 */
		public boolean isEnabled() {
			return this.enabled;
		}

		/**
		 * Sets the enabled status of the cache.
		 * @param enabled the enabled status to be set
		 */
		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		/**
		 * Returns a boolean value indicating whether the cache is for consumers.
		 * @return true if the cache is for consumers, false otherwise
		 */
		public boolean isConsumers() {
			return this.consumers;
		}

		/**
		 * Sets the value of the consumers flag.
		 * @param consumers the new value for the consumers flag
		 */
		public void setConsumers(boolean consumers) {
			this.consumers = consumers;
		}

		/**
		 * Returns a boolean value indicating whether the cache is for producers.
		 * @return true if the cache is for producers, false otherwise
		 */
		public boolean isProducers() {
			return this.producers;
		}

		/**
		 * Sets the value of the "producers" flag in the Cache class.
		 * @param producers the new value for the "producers" flag
		 */
		public void setProducers(boolean producers) {
			this.producers = producers;
		}

		/**
		 * Returns the size of the session cache.
		 * @return the size of the session cache
		 */
		public int getSessionCacheSize() {
			return this.sessionCacheSize;
		}

		/**
		 * Sets the size of the session cache.
		 * @param sessionCacheSize the size of the session cache to be set
		 */
		public void setSessionCacheSize(int sessionCacheSize) {
			this.sessionCacheSize = sessionCacheSize;
		}

	}

	/**
	 * Listener class.
	 */
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

		private final Session session = new Session();

		/**
		 * Returns a boolean value indicating whether the listener is set to automatically
		 * start.
		 * @return true if the listener is set to automatically start, false otherwise
		 */
		public boolean isAutoStartup() {
			return this.autoStartup;
		}

		/**
		 * Sets the auto startup flag for the listener.
		 * @param autoStartup the auto startup flag to set
		 */
		public void setAutoStartup(boolean autoStartup) {
			this.autoStartup = autoStartup;
		}

		/**
		 * Returns the acknowledge mode for the listener session.
		 * @return the acknowledge mode for the listener session
		 * @deprecated This method is deprecated since version 3.2.0 and will be removed
		 * in a future release. Use {@link #getAcknowledgeMode()} instead.
		 * @see #getAcknowledgeMode()
		 * @since 3.2.0
		 */
		@Deprecated(since = "3.2.0", forRemoval = true)
		@DeprecatedConfigurationProperty(replacement = "spring.jms.listener.session.acknowledge-mode", since = "3.2.0")
		public AcknowledgeMode getAcknowledgeMode() {
			return this.session.getAcknowledgeMode();
		}

		/**
		 * Sets the acknowledge mode for the session.
		 * @param acknowledgeMode the acknowledge mode to be set
		 * @deprecated This method is deprecated since version 3.2.0 and will be removed
		 * in a future release.
		 *
		 * @see AcknowledgeMode
		 */
		@Deprecated(since = "3.2.0", forRemoval = true)
		public void setAcknowledgeMode(AcknowledgeMode acknowledgeMode) {
			this.session.setAcknowledgeMode(acknowledgeMode);
		}

		/**
		 * Retrieves the concurrency level for the listener.
		 * @return the concurrency level
		 * @deprecated This method has been deprecated since version 3.2.0 and will be
		 * removed in a future release. Use {@link #getMinConcurrency()} instead.
		 *
		 * @see #getMinConcurrency()
		 * @see DeprecatedConfigurationProperty
		 */
		@DeprecatedConfigurationProperty(replacement = "spring.jms.listener.min-concurrency", since = "3.2.0")
		@Deprecated(since = "3.2.0", forRemoval = true)
		public Integer getConcurrency() {
			return this.minConcurrency;
		}

		/**
		 * Sets the concurrency level for the listener.
		 * @param concurrency the concurrency level to be set
		 * @deprecated This method has been deprecated since version 3.2.0 and will be
		 * removed in a future release.
		 *
		 * @see Listener#getConcurrency()
		 * @see Listener#minConcurrency
		 */
		@Deprecated(since = "3.2.0", forRemoval = true)
		public void setConcurrency(Integer concurrency) {
			this.minConcurrency = concurrency;
		}

		/**
		 * Returns the minimum concurrency level for the listener.
		 * @return the minimum concurrency level
		 */
		public Integer getMinConcurrency() {
			return this.minConcurrency;
		}

		/**
		 * Sets the minimum concurrency level for the listener.
		 * @param minConcurrency the minimum concurrency level to be set
		 */
		public void setMinConcurrency(Integer minConcurrency) {
			this.minConcurrency = minConcurrency;
		}

		/**
		 * Returns the maximum concurrency value.
		 * @return the maximum concurrency value
		 */
		public Integer getMaxConcurrency() {
			return this.maxConcurrency;
		}

		/**
		 * Sets the maximum concurrency for the listener.
		 * @param maxConcurrency the maximum concurrency to be set
		 */
		public void setMaxConcurrency(Integer maxConcurrency) {
			this.maxConcurrency = maxConcurrency;
		}

		/**
		 * Formats the concurrency range.
		 * @return The formatted concurrency range as a string.
		 */
		public String formatConcurrency() {
			if (this.minConcurrency == null) {
				return (this.maxConcurrency != null) ? "1-" + this.maxConcurrency : null;
			}
			return this.minConcurrency + "-"
					+ ((this.maxConcurrency != null) ? this.maxConcurrency : this.minConcurrency);
		}

		/**
		 * Returns the receive timeout duration.
		 * @return the receive timeout duration
		 */
		public Duration getReceiveTimeout() {
			return this.receiveTimeout;
		}

		/**
		 * Sets the receive timeout for the listener.
		 * @param receiveTimeout the receive timeout duration to be set
		 */
		public void setReceiveTimeout(Duration receiveTimeout) {
			this.receiveTimeout = receiveTimeout;
		}

		/**
		 * Returns the current session.
		 * @return the current session
		 */
		public Session getSession() {
			return this.session;
		}

		/**
		 * Session class.
		 */
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

			/**
			 * Returns the acknowledge mode of the session.
			 * @return the acknowledge mode of the session
			 */
			public AcknowledgeMode getAcknowledgeMode() {
				return this.acknowledgeMode;
			}

			/**
			 * Sets the acknowledge mode for this session.
			 * @param acknowledgeMode the acknowledge mode to be set
			 */
			public void setAcknowledgeMode(AcknowledgeMode acknowledgeMode) {
				this.acknowledgeMode = acknowledgeMode;
			}

			/**
			 * Returns the value of the transacted property.
			 * @return true if the session is transacted, false otherwise
			 */
			public Boolean getTransacted() {
				return this.transacted;
			}

			/**
			 * Sets the transacted flag for the session.
			 * @param transacted the transacted flag to be set
			 */
			public void setTransacted(Boolean transacted) {
				this.transacted = transacted;
			}

		}

	}

	/**
	 * Template class.
	 */
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

		/**
		 * Returns the default destination.
		 * @return the default destination
		 */
		public String getDefaultDestination() {
			return this.defaultDestination;
		}

		/**
		 * Sets the default destination for the Template.
		 * @param defaultDestination the default destination to be set
		 */
		public void setDefaultDestination(String defaultDestination) {
			this.defaultDestination = defaultDestination;
		}

		/**
		 * Returns the delivery delay for this Template.
		 * @return the delivery delay for this Template
		 */
		public Duration getDeliveryDelay() {
			return this.deliveryDelay;
		}

		/**
		 * Sets the delivery delay for the template.
		 * @param deliveryDelay the duration of the delivery delay
		 */
		public void setDeliveryDelay(Duration deliveryDelay) {
			this.deliveryDelay = deliveryDelay;
		}

		/**
		 * Returns the delivery mode of the template.
		 * @return the delivery mode of the template
		 */
		public DeliveryMode getDeliveryMode() {
			return this.deliveryMode;
		}

		/**
		 * Sets the delivery mode for the template.
		 * @param deliveryMode the delivery mode to be set
		 */
		public void setDeliveryMode(DeliveryMode deliveryMode) {
			this.deliveryMode = deliveryMode;
		}

		/**
		 * Returns the priority of the Template.
		 * @return the priority of the Template
		 */
		public Integer getPriority() {
			return this.priority;
		}

		/**
		 * Sets the priority of the template.
		 * @param priority the priority to be set
		 */
		public void setPriority(Integer priority) {
			this.priority = priority;
		}

		/**
		 * Returns the time to live for the object.
		 * @return the time to live as a Duration object
		 */
		public Duration getTimeToLive() {
			return this.timeToLive;
		}

		/**
		 * Sets the time to live for the template.
		 * @param timeToLive the duration representing the time to live
		 */
		public void setTimeToLive(Duration timeToLive) {
			this.timeToLive = timeToLive;
		}

		/**
		 * Determines if QoS (Quality of Service) is enabled.
		 * @return true if QoS is enabled, false otherwise
		 */
		public boolean determineQosEnabled() {
			if (this.qosEnabled != null) {
				return this.qosEnabled;
			}
			return (getDeliveryMode() != null || getPriority() != null || getTimeToLive() != null);
		}

		/**
		 * Returns the value of the QoS enabled flag.
		 * @return true if QoS is enabled, false otherwise
		 */
		public Boolean getQosEnabled() {
			return this.qosEnabled;
		}

		/**
		 * Sets the QoS enabled flag.
		 * @param qosEnabled the QoS enabled flag to set
		 */
		public void setQosEnabled(Boolean qosEnabled) {
			this.qosEnabled = qosEnabled;
		}

		/**
		 * Returns the receive timeout duration.
		 * @return the receive timeout duration
		 */
		public Duration getReceiveTimeout() {
			return this.receiveTimeout;
		}

		/**
		 * Sets the receive timeout for the Template.
		 * @param receiveTimeout the receive timeout duration to be set
		 */
		public void setReceiveTimeout(Duration receiveTimeout) {
			this.receiveTimeout = receiveTimeout;
		}

		/**
		 * Returns the session object associated with this Template.
		 * @return the session object
		 */
		public Session getSession() {
			return this.session;
		}

		/**
		 * Session class.
		 */
		public static class Session {

			/**
			 * Acknowledge mode used when creating sessions.
			 */
			private AcknowledgeMode acknowledgeMode = AcknowledgeMode.AUTO;

			/**
			 * Whether to use transacted sessions.
			 */
			private boolean transacted = false;

			/**
			 * Returns the acknowledge mode of the session.
			 * @return the acknowledge mode of the session
			 */
			public AcknowledgeMode getAcknowledgeMode() {
				return this.acknowledgeMode;
			}

			/**
			 * Sets the acknowledge mode for this session.
			 * @param acknowledgeMode the acknowledge mode to be set
			 */
			public void setAcknowledgeMode(AcknowledgeMode acknowledgeMode) {
				this.acknowledgeMode = acknowledgeMode;
			}

			/**
			 * Returns a boolean value indicating whether the session is transacted.
			 * @return true if the session is transacted, false otherwise
			 */
			public boolean isTransacted() {
				return this.transacted;
			}

			/**
			 * Sets the transacted flag for the session.
			 * @param transacted the value to set for the transacted flag
			 */
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

		/**
		 * Sets the delivery mode for the JMS message.
		 * @param value the delivery mode value to be set
		 */
		DeliveryMode(int value) {
			this.value = value;
		}

		/**
		 * Returns the value of the JmsProperties object.
		 * @return the value of the JmsProperties object.
		 */
		public int getValue() {
			return this.value;
		}

	}

}
