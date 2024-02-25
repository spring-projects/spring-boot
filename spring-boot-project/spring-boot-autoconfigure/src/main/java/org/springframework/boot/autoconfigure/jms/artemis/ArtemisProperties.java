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
 * @author Justin Bertram
 * @since 1.3.0
 */
@ConfigurationProperties(prefix = "spring.artemis")
public class ArtemisProperties {

	/**
	 * Artemis deployment mode, auto-detected by default.
	 */
	private ArtemisMode mode;

	/**
	 * Artemis broker url.
	 */
	private String brokerUrl;

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

	/**
     * Returns the current mode of the ArtemisProperties.
     *
     * @return the current mode of the ArtemisProperties
     */
    public ArtemisMode getMode() {
		return this.mode;
	}

	/**
     * Sets the mode of the ArtemisProperties.
     * 
     * @param mode the ArtemisMode to set
     */
    public void setMode(ArtemisMode mode) {
		this.mode = mode;
	}

	/**
     * Returns the broker URL.
     *
     * @return the broker URL
     */
    public String getBrokerUrl() {
		return this.brokerUrl;
	}

	/**
     * Sets the broker URL for connecting to the Artemis message broker.
     * 
     * @param brokerUrl the URL of the Artemis message broker
     */
    public void setBrokerUrl(String brokerUrl) {
		this.brokerUrl = brokerUrl;
	}

	/**
     * Returns the user associated with the ArtemisProperties object.
     *
     * @return the user associated with the ArtemisProperties object
     */
    public String getUser() {
		return this.user;
	}

	/**
     * Sets the user for the ArtemisProperties.
     * 
     * @param user the user to be set
     */
    public void setUser(String user) {
		this.user = user;
	}

	/**
     * Returns the password value.
     *
     * @return the password value
     */
    public String getPassword() {
		return this.password;
	}

	/**
     * Sets the password for the ArtemisProperties object.
     * 
     * @param password the password to be set
     */
    public void setPassword(String password) {
		this.password = password;
	}

	/**
     * Returns the embedded object.
     *
     * @return the embedded object
     */
    public Embedded getEmbedded() {
		return this.embedded;
	}

	/**
     * Returns the JmsPoolConnectionFactoryProperties object representing the connection pool configuration.
     *
     * @return the JmsPoolConnectionFactoryProperties object representing the connection pool configuration
     */
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

		/**
         * Returns the server ID.
         * 
         * @return the server ID
         */
        public int getServerId() {
			return this.serverId;
		}

		/**
         * Sets the server ID for the Embedded class.
         * 
         * @param serverId the server ID to be set
         */
        public void setServerId(int serverId) {
			this.serverId = serverId;
		}

		/**
         * Returns the current state of the enabled flag.
         *
         * @return true if the enabled flag is set, false otherwise.
         */
        public boolean isEnabled() {
			return this.enabled;
		}

		/**
         * Sets the enabled status of the Embedded object.
         * 
         * @param enabled the enabled status to be set
         */
        public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		/**
         * Returns a boolean value indicating whether the object is persistent.
         * 
         * @return true if the object is persistent, false otherwise
         */
        public boolean isPersistent() {
			return this.persistent;
		}

		/**
         * Sets the persistent flag for the Embedded object.
         * 
         * @param persistent the value to set for the persistent flag
         */
        public void setPersistent(boolean persistent) {
			this.persistent = persistent;
		}

		/**
         * Returns the data directory.
         *
         * @return the data directory
         */
        public String getDataDirectory() {
			return this.dataDirectory;
		}

		/**
         * Sets the data directory for the Embedded class.
         * 
         * @param dataDirectory the path to the data directory
         */
        public void setDataDirectory(String dataDirectory) {
			this.dataDirectory = dataDirectory;
		}

		/**
         * Returns an array of queues.
         *
         * @return an array of queues
         */
        public String[] getQueues() {
			return this.queues;
		}

		/**
         * Sets the queues for the Embedded class.
         * 
         * @param queues an array of strings representing the queues
         */
        public void setQueues(String[] queues) {
			this.queues = queues;
		}

		/**
         * Returns an array of topics.
         *
         * @return an array of topics
         */
        public String[] getTopics() {
			return this.topics;
		}

		/**
         * Sets the topics for the Embedded class.
         * 
         * @param topics an array of strings representing the topics to be set
         */
        public void setTopics(String[] topics) {
			this.topics = topics;
		}

		/**
         * Returns the cluster password.
         * 
         * @return the cluster password
         */
        public String getClusterPassword() {
			return this.clusterPassword;
		}

		/**
         * Sets the password for the cluster.
         * 
         * @param clusterPassword the password to set for the cluster
         */
        public void setClusterPassword(String clusterPassword) {
			this.clusterPassword = clusterPassword;
			this.defaultClusterPassword = false;
		}

		/**
         * Returns true if the cluster password is set to the default value.
         * 
         * @return true if the cluster password is set to the default value, false otherwise
         */
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
