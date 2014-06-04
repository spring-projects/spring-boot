/*
 * Copyright 2012-2014 the original author or authors.
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

import java.util.UUID;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for HornetQ
 * 
 * @author Stephane Nicoll
 * @since 1.1.0
 */
@ConfigurationProperties(prefix = "spring.hornetq")
public class HornetQProperties {

	private HornetQMode mode;

	private String host = "localhost";

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

		private boolean enabled = true;

		private boolean persistent;

		private String dataDirectory;

		private String[] queues = new String[0];

		private String[] topics = new String[0];

		private String clusterPassword = UUID.randomUUID().toString();

		private boolean defaultClusterPassword = true;

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

	}

}
