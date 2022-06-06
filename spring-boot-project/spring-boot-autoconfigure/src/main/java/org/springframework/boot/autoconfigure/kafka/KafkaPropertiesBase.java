/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.autoconfigure.kafka;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.CommonClientConfigs;

import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.util.CollectionUtils;

/**
 * Common Spring Kafka configuration properties.
 * <p>
 * Users should refer to Kafka documentation for complete descriptions of these
 * properties.
 *
 * @author Chris Bono
 * @since 2.7.0
 */
class KafkaPropertiesBase {

	/**
	 * ID to pass to the server when making requests. Used for server-side logging.
	 */
	private String clientId;

	/**
	 * Additional properties, common to all clients, used to configure the client.
	 */
	private final Map<String, String> properties = new HashMap<>();

	@NestedConfigurationProperty
	private final Ssl ssl = new Ssl();

	@NestedConfigurationProperty
	private final Security security = new Security();

	public String getClientId() {
		return this.clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public Map<String, String> getProperties() {
		return this.properties;
	}

	public Ssl getSsl() {
		return this.ssl;
	}

	public Security getSecurity() {
		return this.security;
	}

	public Map<String, Object> buildProperties() {
		Map<String, Object> properties = new HashMap<>();
		if (this.clientId != null) {
			properties.put(CommonClientConfigs.CLIENT_ID_CONFIG, this.clientId);
		}
		properties.putAll(this.ssl.buildProperties());
		properties.putAll(this.security.buildProperties());
		if (!CollectionUtils.isEmpty(this.properties)) {
			properties.putAll(this.properties);
		}
		return properties;
	}

}
