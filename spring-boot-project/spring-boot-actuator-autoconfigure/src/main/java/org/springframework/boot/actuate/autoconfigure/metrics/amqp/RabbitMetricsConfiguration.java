/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics.amqp;

import java.util.Map;

import com.rabbitmq.client.ConnectionFactory;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;

import org.springframework.amqp.rabbit.connection.AbstractConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.metrics.amqp.RabbitMetrics;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Configure metrics for all available {@link ConnectionFactory connection factories}.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 */
@Configuration
@ConditionalOnClass({ ConnectionFactory.class, AbstractConnectionFactory.class })
@ConditionalOnBean(AbstractConnectionFactory.class)
@ConditionalOnProperty(value = "management.metrics.rabbitmq.instrument", matchIfMissing = true)
@EnableConfigurationProperties(RabbitMetricsProperties.class)
public class RabbitMetricsConfiguration {

	private static final String CONNECTION_FACTORY_SUFFIX = "connectionFactory";

	private final MeterRegistry registry;

	private final String metricName;

	public RabbitMetricsConfiguration(MeterRegistry registry,
			RabbitMetricsProperties rabbitMetricsProperties) {
		this.registry = registry;
		this.metricName = rabbitMetricsProperties.getMetricName();
	}

	@Autowired
	public void bindConnectionFactoriesToRegistry(
			Map<String, AbstractConnectionFactory> connectionFactories) {
		connectionFactories.forEach(this::bindConnectionFactoryToRegistry);
	}

	private void bindConnectionFactoryToRegistry(String beanName,
			AbstractConnectionFactory connectionFactory) {
		ConnectionFactory rabbitConnectionFactory = connectionFactory
				.getRabbitConnectionFactory();
		String connectionFactoryName = getConnectionFactoryName(beanName);
		new RabbitMetrics(rabbitConnectionFactory, this.metricName,
				Tags.of("name", connectionFactoryName)).bindTo(this.registry);
	}

	/**
	 * Get the name of a ConnectionFactory based on its {@code beanName}.
	 * @param beanName the name of the connection factory bean
	 * @return a name for the given connection factory
	 */
	private String getConnectionFactoryName(String beanName) {
		if (beanName.length() > CONNECTION_FACTORY_SUFFIX.length()
				&& StringUtils.endsWithIgnoreCase(beanName, CONNECTION_FACTORY_SUFFIX)) {
			return beanName.substring(0,
					beanName.length() - CONNECTION_FACTORY_SUFFIX.length());
		}
		return beanName;
	}

}
