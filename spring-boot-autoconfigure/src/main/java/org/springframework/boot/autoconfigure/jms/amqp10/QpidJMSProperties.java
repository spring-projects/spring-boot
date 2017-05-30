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

package org.springframework.boot.autoconfigure.jms.amqp10;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Qpid JMS client.
 *
 * @author Timothy Bish
 */
@ConfigurationProperties(prefix = "spring.qpidjms")
public class QpidJMSProperties {

	private String remoteURL;
	private String username;
	private String password;
	private String clientId;

	private Boolean receiveLocalOnly;
	private Boolean receiveNoWaitLocalOnly;

	private final DeserializationPolicy deserializationPolicy = new DeserializationPolicy();

	public String getRemoteURL() {
		return this.remoteURL;
	}

	public void setRemoteURL(String remoteURL) {
		this.remoteURL = remoteURL;
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

	public String getClientId() {
		return this.clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public Boolean isReceiveLocalOnly() {
		return this.receiveLocalOnly;
	}

	public void setReceiveLocalOnly(Boolean receiveLocalOnly) {
		this.receiveLocalOnly = receiveLocalOnly;
	}

	public Boolean isReceiveNoWaitLocalOnly() {
		return this.receiveNoWaitLocalOnly;
	}

	public void setReceiveNoWaitLocalOnly(Boolean receiveNoWaitLocalOnly) {
		this.receiveNoWaitLocalOnly = receiveNoWaitLocalOnly;
	}

	public DeserializationPolicy getDeserializationPolicy() {
		return this.deserializationPolicy;
	}

	public static class DeserializationPolicy {

		private String whiteList;
		private String blackList;

		public String getWhiteList() {
			return this.whiteList;
		}

		public void setWhiteList(String whiteList) {
			this.whiteList = whiteList;
		}

		public String getBlackList() {
			return this.blackList;
		}

		public void setBlackList(String blackList) {
			this.blackList = blackList;
		}
	}
}
