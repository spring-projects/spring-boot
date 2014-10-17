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

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.util.StringUtils;

public class PahoMqttClientFactory {

	private final PahoProperties pahoProperties;

	private final PahoMqttConnectOptionsConfiguration connectOptionsConfiguration;

	public PahoMqttClientFactory(PahoProperties pahoProperties, PahoMqttConnectOptionsConfiguration connectOptionsConfiguration) {
		this.pahoProperties = pahoProperties;
		this.connectOptionsConfiguration = connectOptionsConfiguration;
	}

	public MqttClient createMqttClient() {
		try {
			MqttClient mqttClient = new MqttClient(pahoProperties.getBrokerUrl(), pahoProperties.getClientId(), pahoProperties.resolveClientPersistence());

			MqttConnectOptions connectOptions = new MqttConnectOptions();
			if (StringUtils.hasLength(pahoProperties.getUser()) && StringUtils.hasLength(pahoProperties.getPassword())) {
				connectOptions.setUserName(pahoProperties.getUser());
				connectOptions.setPassword(pahoProperties.getPassword().toCharArray());
			}
			if (connectOptionsConfiguration != null) {
				connectOptionsConfiguration.configure(connectOptions);
			}
			mqttClient.connect(connectOptions);

			return mqttClient;
		} catch (MqttException e) {
			throw new RuntimeException(e);
		}
	}

}
