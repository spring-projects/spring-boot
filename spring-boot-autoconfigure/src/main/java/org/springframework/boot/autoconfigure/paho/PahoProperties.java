/*
 * Copyright 2012-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.paho;

import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import java.util.UUID;

/**
 * Configuration properties for Paho MQTT client.
 *
 * @author Henryk Konsek
 */
@ConfigurationProperties(prefix = "spring.paho")
public class PahoProperties {

	private static final int MAX_CLIENT_ID_LENGTH = 23;

	private String brokerUrl = "tcp://localhost:1883";

	private String user;

	private String password;

	private String clientId = UUID.randomUUID().toString().substring(0, MAX_CLIENT_ID_LENGTH);

	private String clientPersistenceDirectory;

	public String getBrokerUrl() {
		return this.brokerUrl;
	}

	public void setBrokerUrl(String brokerUrl) {
		this.brokerUrl = brokerUrl;
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

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getClientPersistenceDirectory() {
		return clientPersistenceDirectory;
	}

	public void setClientPersistenceDirectory(String clientPersistenceDirectory) {
		this.clientPersistenceDirectory = clientPersistenceDirectory;
	}

	public MqttClientPersistence resolveClientPersistence() {
		if (StringUtils.hasLength(clientPersistenceDirectory)) {
			return new MqttDefaultFilePersistence(clientPersistenceDirectory);
		} else {
			return new MemoryPersistence();
		}
	}

}
