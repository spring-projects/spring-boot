/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.autoconfigure.jms;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for JMS.
 *
 * @author Greg Turnquist
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
@ConfigurationProperties(prefix = "spring.jms")
public class JmsProperties {

	/**
	 * Specify if the default destination type is topic.
	 */
	private boolean pubSubDomain = false;

	/**
	 * Connection factory JNDI name. When set, takes precedence to others connection
	 * factory auto-configurations.
	 */
	private String jndiName;

	private final Listener listener = new Listener();

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

	public Listener getListener() {
		return this.listener;
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
				return (this.maxConcurrency != null ? "1-" + this.maxConcurrency : null);
			}
			return (this.maxConcurrency != null
					? this.concurrency + "-" + this.maxConcurrency
					: String.valueOf(this.concurrency));
		}
	}

	/**
	 * Translate the acknowledge modes defined on the {@link javax.jms.Session}.
	 *
	 * <p>
	 * {@link javax.jms.Session#SESSION_TRANSACTED} is not defined as we take care of this
	 * already via a call to {@code setSessionTransacted}.
	 */
	public enum AcknowledgeMode {

		/**
		 * Messages sent or received from the session are automatically acknowledged. This
		 * is the simplest mode and enables once-only message delivery guarantee.
		 */
		AUTO(1),

		/**
		 * Messages are acknowledged once the message listener implementation has called
		 * {@link javax.jms.Message#acknowledge()}. This mode gives the application
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

}
