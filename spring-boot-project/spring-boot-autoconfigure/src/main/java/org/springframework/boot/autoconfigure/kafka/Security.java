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

import java.util.Map;

import org.apache.kafka.clients.CommonClientConfigs;

import org.springframework.boot.context.properties.PropertyMapper;

/**
 * Spring Kafka security related configuration properties for Kafka clients.
 *
 * @author Chris Bono
 */
public class Security {

	/**
	 * Security protocol used to communicate with brokers.
	 */
	private String protocol;

	public String getProtocol() {
		return this.protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public Map<String, Object> buildProperties() {
		PropertiesMap properties = new PropertiesMap();
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(this::getProtocol).to(properties.in(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG));
		return properties;
	}

}
