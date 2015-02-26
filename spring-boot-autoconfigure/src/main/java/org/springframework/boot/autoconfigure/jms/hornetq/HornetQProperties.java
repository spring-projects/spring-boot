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

package org.springframework.boot.autoconfigure.jms.hornetq;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.hornetq.core.remoting.impl.invm.TransportConstants;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for HornetQ
 *
 * @author Stephane Nicoll
 * @since 1.1.0
 */
@ConfigurationProperties(prefix = "spring.hornetq")
public class HornetQProperties {

	/**
	 * HornetQ deployment mode, auto-detected by default. Can be explicitly set to
	 * "native" or "embedded".
	 */
	private HornetQMode mode;

	/**
	 * HornetQ broker host.
	 */
	private String host = "localhost";

	/**
	 * HornetQ broker port.
	 */
	private int port = 5445;

	private final Embedded embedded = new Embedded();

	public HornetQMode getMode() {
		return this.mode;
	}

	public void setMode(HornetQMode mode) {
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

	public Embedded getEmbedded() {
		return this.embedded;
	}

	/**
	 * Configuration for an embedded HornetQ server.
	 */
	public static class Embedded {

		private static final AtomicInteger serverIdCounter = new AtomicInteger();

		/**
		 * Server id. By default, an auto-incremented counter is used.
		 */
		private int serverId = serverIdCounter.getAndIncrement();

		/**
		 * Enable embedded mode if the HornetQ server APIs are available.
		 */
		private boolean enabled = true;

		/**
		 * Enable persistent store.
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
			Map<String, Object> parameters = new HashMap<String, Object>();
			parameters.put(TransportConstants.SERVER_ID_PROP_NAME, getServerId());
			return parameters;
		}

	}

}
