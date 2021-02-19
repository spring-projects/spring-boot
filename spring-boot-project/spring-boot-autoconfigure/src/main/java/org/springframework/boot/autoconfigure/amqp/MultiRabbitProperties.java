/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.autoconfigure.amqp;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.amqp.rabbit.config.RabbitListenerConfigUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.lang.Nullable;

/**
 * Configuration properties for multiple Rabbit.
 *
 * @author Wander Costa
 * @see RabbitProperties
 * @since 2.4
 */
@ConfigurationProperties(prefix = "spring.multirabbitmq")
public class MultiRabbitProperties {

	/**
	 * A flag to enable/disable MultiRabbit processing.
	 */
	@Value("${" + RabbitListenerConfigUtils.MULTI_RABBIT_ENABLED_PROPERTY + "}")
	private boolean enabled;

	/**
	 * A map representing the RabbitProperties of all available brokers.
	 */
	private Map<String, RabbitProperties> connections = new HashMap<>();

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public Map<String, RabbitProperties> getConnections() {
		return this.connections;
	}

	public void setConnections(@Nullable final Map<String, RabbitProperties> connections) {
		this.connections = Optional.ofNullable(connections).orElse(new HashMap<>());
	}

}
