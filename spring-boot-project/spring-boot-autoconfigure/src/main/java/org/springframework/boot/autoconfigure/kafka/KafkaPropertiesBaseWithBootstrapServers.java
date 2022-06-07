/*
 * Copyright 2022-2022 the original author or authors.
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

import java.util.List;
import java.util.Map;

import org.apache.kafka.clients.CommonClientConfigs;

/**
 * Common Spring Kafka configuration properties - including boostrap server information.
 * <p>
 * Users should refer to Kafka documentation for complete descriptions of these
 * properties.
 *
 * @author Chris Bono
 * @since 2.7.0
 */
class KafkaPropertiesBaseWithBootstrapServers extends KafkaPropertiesBase {

	/**
	 * Comma-delimited list of host:port pairs to use for establishing the initial
	 * connections to the Kafka cluster. Applies to all components unless overridden.
	 */
	private List<String> bootstrapServers;

	KafkaPropertiesBaseWithBootstrapServers() {
		this(null);
	}

	KafkaPropertiesBaseWithBootstrapServers(List<String> defaultBootstrapServers) {
		super();
		this.bootstrapServers = defaultBootstrapServers;
	}

	public List<String> getBootstrapServers() {
		return this.bootstrapServers;
	}

	public void setBootstrapServers(List<String> bootstrapServers) {
		this.bootstrapServers = bootstrapServers;
	}

	public Map<String, Object> buildProperties() {
		Map<String, Object> properties = super.buildProperties();
		if (this.bootstrapServers != null) {
			properties.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, this.bootstrapServers);
		}
		return properties;
	}

}
