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

package org.springframework.boot.actuate.autoconfigure.metrics.amqp;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MetricsCollector;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;

import org.springframework.amqp.rabbit.connection.AbstractConnectionFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.actuate.metrics.amqp.RabbitMetrics;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.util.StringUtils;

/**
 * {@link BeanPostProcessor} that configures RabbitMQ metrics. Such arrangement is
 * necessary because a connection can be eagerly created and cached without a reference to
 * a proper {@link MetricsCollector}.
 *
 * @author Stephane Nicoll
 */
class RabbitConnectionFactoryMetricsPostProcessor implements BeanPostProcessor, Ordered {

	private static final String CONNECTION_FACTORY_SUFFIX = "connectionFactory";

	private final ApplicationContext context;

	private volatile MeterRegistry meterRegistry;

	/**
	 * Constructs a new RabbitConnectionFactoryMetricsPostProcessor with the specified
	 * ApplicationContext.
	 * @param context the ApplicationContext to be used by the
	 * RabbitConnectionFactoryMetricsPostProcessor
	 */
	RabbitConnectionFactoryMetricsPostProcessor(ApplicationContext context) {
		this.context = context;
	}

	/**
	 * Post-processes the bean after initialization. Binds the connection factory to the
	 * meter registry if it is an instance of AbstractConnectionFactory.
	 * @param bean the initialized bean
	 * @param beanName the name of the bean
	 * @return the processed bean
	 */
	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) {
		if (bean instanceof AbstractConnectionFactory connectionFactory) {
			bindConnectionFactoryToRegistry(getMeterRegistry(), beanName, connectionFactory);
		}
		return bean;
	}

	/**
	 * Binds the given {@link AbstractConnectionFactory} to the provided
	 * {@link MeterRegistry} by creating a new instance of {@link RabbitMetrics} and
	 * binding it to the registry. The {@link RabbitMetrics} instance is created using the
	 * RabbitMQ connection factory obtained from the provided
	 * {@link AbstractConnectionFactory}. The connection factory name is derived from the
	 * given bean name by calling {@link #getConnectionFactoryName(String)}. The created
	 * {@link RabbitMetrics} instance is then bound to the registry using the connection
	 * factory name as a tag.
	 * @param registry The {@link MeterRegistry} to bind the metrics to.
	 * @param beanName The name of the bean associated with the connection factory.
	 * @param connectionFactory The {@link AbstractConnectionFactory} to bind to the
	 * registry.
	 */
	private void bindConnectionFactoryToRegistry(MeterRegistry registry, String beanName,
			AbstractConnectionFactory connectionFactory) {
		ConnectionFactory rabbitConnectionFactory = connectionFactory.getRabbitConnectionFactory();
		String connectionFactoryName = getConnectionFactoryName(beanName);
		new RabbitMetrics(rabbitConnectionFactory, Tags.of("name", connectionFactoryName)).bindTo(registry);
	}

	/**
	 * Get the name of a ConnectionFactory based on its {@code beanName}.
	 * @param beanName the name of the connection factory bean
	 * @return a name for the given connection factory
	 */
	private String getConnectionFactoryName(String beanName) {
		if (beanName.length() > CONNECTION_FACTORY_SUFFIX.length()
				&& StringUtils.endsWithIgnoreCase(beanName, CONNECTION_FACTORY_SUFFIX)) {
			return beanName.substring(0, beanName.length() - CONNECTION_FACTORY_SUFFIX.length());
		}
		return beanName;
	}

	/**
	 * Returns the MeterRegistry instance.
	 * @return the MeterRegistry instance
	 */
	private MeterRegistry getMeterRegistry() {
		if (this.meterRegistry == null) {
			this.meterRegistry = this.context.getBean(MeterRegistry.class);
		}
		return this.meterRegistry;
	}

	/**
	 * Returns the order of this RabbitConnectionFactoryMetricsPostProcessor bean. The
	 * order is set to the highest precedence using the Ordered.HIGHEST_PRECEDENCE
	 * constant.
	 * @return the order of this RabbitConnectionFactoryMetricsPostProcessor bean
	 */
	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}

}
