/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.autoconfigure.jms.artemis;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.activemq.artemis.core.remoting.impl.invm.TransportConstants;

import org.springframework.boot.autoconfigure.jms.JmsPoolConnectionFactoryProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Configuration properties for Artemis.
 *
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @since 1.3.0
 */
@ConfigurationProperties(prefix = "spring.artemis")
public class ArtemisProperties {

	/**
	 * Artemis deployment mode, auto-detected by default.
	 */
	private ArtemisMode mode;

	/**
	 * Artemis broker host.
	 */
	private String host = "localhost";

	/**
	 * Artemis broker port.
	 */
	private int port = 61616;

	/**
	 * Login user of the broker.
	 */
	private String user;

	/**
	 * Login password of the broker.
	 */
	private String password;

	private final Embedded embedded = new Embedded();

	@NestedConfigurationProperty
	private final JmsPoolConnectionFactoryProperties pool = new JmsPoolConnectionFactoryProperties();

	public ArtemisMode getMode() {
		return this.mode;
	}

	public void setMode(ArtemisMode mode) {
		this.mode = mode;
	}

	public String getHost() {
		return this.host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return this.port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getUser() {
		return this.user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPassword() {
		return this.password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Embedded getEmbedded() {
		return this.embedded;
	}

	public JmsPoolConnectionFactoryProperties getPool() {
		return this.pool;
	}

	/**
	 * Configuration for an embedded Artemis server.
	 */
	public static class Embedded {

		private static final AtomicInteger serverIdCounter = new AtomicInteger();

		/**
		 * Server ID. By default, an auto-incremented counter is used.
		 */
		private int serverId = serverIdCounter.getAndIncrement();

		/**
		 * Whether to enable embedded mode if the Artemis server APIs are available.
		 */
		private boolean enabled = true;

		/**
		 * Whether to enable persistent store.
		 */
		private boolean persistent;

		/**
		 * Journal file directory. Not necessary if persistence is turned off.
		 */
		private String dataDirectory;

		/**
		 * Comma-separated list of queues to create on startup.
		 */
		private String[] queues = new String[0];

		/**
		 * Comma-separated list of topics to create on startup.
		 */
		private String[] topics = new String[0];

		/**
		 * Cluster password. Randomly generated on startup by default.
		 */
		private String clusterPassword = UUID.randomUUID().toString();

		private boolean defaultClusterPassword = true;

		public int getServerId() {
			return this.serverId;
		}

		public void setServerId(int serverId) {
			this.serverId = serverId;
		}

		public boolean isEnabled() {
			return this.enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public boolean isPersistent() {
			return this.persistent;
		}

		public void setPersistent(boolean persistent) {
			this.persistent = persistent;
		}

		public String getDataDirectory() {
			return this.dataDirectory;
		}

		public void setDataDirectory(String dataDirectory) {
			this.dataDirectory = dataDirectory;
		}

		public String[] getQueues() {
			return this.queues;
		}

		public void setQueues(String[] queues) {
			this.queues = queues;
		}

		public String[] getTopics() {
			return this.topics;
		}

		public void setTopics(String[] topics) {
			this.topics = topics;
		}

		public String getClusterPassword() {
			return this.clusterPassword;
		}

		public void setClusterPassword(String clusterPassword) {
			this.clusterPassword = clusterPassword;
			this.defaultClusterPassword = false;
		}

		public boolean isDefaultClusterPassword() {
			return this.defaultClusterPassword;
		}

		/**
		 * Creates the minimal transport parameters for an embedded transport
		 * configuration.
		 * @return the transport parameters
		 * @see TransportConstants#SERVER_ID_PROP_NAME
		 */
		public Map<String, Object> generateTransportParameters() {
			Map<String, Object> parameters = new HashMap<>();
			parameters.put(TransportConstants.SERVER_ID_PROP_NAME, getServerId());
			return parameters;
		}

	}

}
