/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.MetricsCollector;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.metrics.CounterService;
import org.springframework.boot.actuate.metrics.amqp.CounterServiceRabbitMetricsCollector;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.ConnectionFactoryCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for RabbitMQ Java Client metrics.
 *
 * @author Arnaud Cogoluègnes
 * @since 2.0.0
 */
@Configuration
@ConditionalOnBean(CounterService.class)
@ConditionalOnClass({ RabbitTemplate.class, Channel.class })
public class RabbitMetricsAutoConfiguration {

	@Autowired
	private CounterService counterService;

	@ConditionalOnClass(MetricsCollector.class)
	@ConditionalOnMissingClass("com.codahale.metrics.MetricRegistry")
	@ConditionalOnProperty(prefix = "spring.rabbitmq", name = "metrics", matchIfMissing = true)
	@Bean
	public ConnectionFactoryCustomizer counterServiceRabbitMetricsCollectorConnectionFactoryCustomizer() {
		return connectionFactory -> {
			connectionFactory.setMetricsCollector(new CounterServiceRabbitMetricsCollector(this.counterService));
		};
	}

}
