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

package org.springframework.boot.actuate.health;

import java.util.HashMap;
import java.util.Map;

import org.springframework.amqp.rabbit.core.ChannelCallback;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.util.Assert;

import com.rabbitmq.client.Channel;

/**
 * Simple implementation of a {@link HealthIndicator} returning status information for the
 * RabbitMQ messaging system.
 *
 * @author Christian Dupuis
 * @since 1.1.0
 */
public class RabbitHealthIndicator implements HealthIndicator<Map<String, Object>> {

	private final RabbitTemplate rabbitTemplate;

	public RabbitHealthIndicator(RabbitTemplate rabbitTemplate) {
		Assert.notNull(rabbitTemplate, "RabbitTemplate must not be null.");
		this.rabbitTemplate = rabbitTemplate;
	}

	@Override
	public Map<String, Object> health() {
		Map<String, Object> health = new HashMap<String, Object>();
		try {
			health.put("version",
					this.rabbitTemplate.execute(new ChannelCallback<String>() {

						@Override
						public String doInRabbit(Channel channel) throws Exception {
							Map<String, Object> serverProperties = channel
									.getConnection().getServerProperties();
							return serverProperties.get("version").toString();
						}
					}));

			health.put("status", "ok");
		}
		catch (Exception ex) {
			health.put("status", "error");
			health.put("error", ex.getClass().getName() + ": " + ex.getMessage());
		}
		return health;
	}
}
