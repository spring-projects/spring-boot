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

package org.springframework.boot.autoconfigure.jms;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for JMS.
 *
 * @author Greg Turnquist
 * @author Phillip Webb
 * @author Stephane Nicoll
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

	private final Cache cache = new Cache();

	private final Listener listener = new Listener();

	private final Template template = new Template();

	public boolean isPubSubDomain() {
		return this.pubSubDomain;
	}

	public void setPubSubDomain(boolean pubSubDomain) {
		this.pubSubDomain = pubSubDomain;
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
		 * Acknowledge mode of the container. By default, the listener is transacted with
		 * automatic acknowledgment.
		 */
		private AcknowledgeMode acknowledgeMode;

		/**
		 * Minimum number of concurrent consumers.
		 */
		private Integer concurrency;

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

		public String formatConcurrency() {
			if (this.concurrency == null) {
				return (this.maxConcurrency != null) ? "1-" + this.maxConcurrency : null;
			}
			return ((this.maxConcurrency != null) ? this.concurrency + "-" + this.maxConcurrency
					: String.valueOf(this.concurrency));
		}

		public Duration getReceiveTimeout() {
			return this.receiveTimeout;
		}

		public void setReceiveTimeout(Duration receiveTimeout) {
			this.receiveTimeout = receiveTimeout;
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

	}

	/**
	 * Translate the acknowledge modes defined on the {@link jakarta.jms.Session}.
	 *
	 * <p>
	 * {@link jakarta.jms.Session#SESSION_TRANSACTED} is not defined as we take care of
	 * this already through a call to {@code setSessionTransacted}.
	 */
	public enum AcknowledgeMode {

		/**
		 * Messages sent or received from the session are automatically acknowledged. This
		 * is the simplest mode and enables once-only message delivery guarantee.
		 */
		AUTO(1),

		/**
		 * Messages are acknowledged once the message listener implementation has called
		 * {@link jakarta.jms.Message#acknowledge()}. This mode gives the application
		 * (rather than the JMS provider) complete control over message acknowledgement.
		 */
		CLIENT(2),

		/**
		 * Similar to auto acknowledgment except that said acknowledgment is lazy. As a
		 * consequence, the messages might be delivered more than once. This mode enables
		 * at-least-once message delivery guarantee.
		 */
		DUPS_OK(3);

		private final int mode;

		AcknowledgeMode(int mode) {
			this.mode = mode;
		}

		public int getMode() {
			return this.mode;
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
