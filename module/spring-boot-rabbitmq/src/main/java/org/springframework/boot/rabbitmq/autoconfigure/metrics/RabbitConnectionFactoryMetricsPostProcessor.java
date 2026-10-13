/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.rabbitmq.autoconfigure.metrics;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MetricsCollector;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.jspecify.annotations.Nullable;

import org.springframework.amqp.rabbit.connection.AbstractConnectionFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.rabbitmq.metrics.RabbitMetrics;
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

	private volatile @Nullable MeterRegistry meterRegistry;

	RabbitConnectionFactoryMetricsPostProcessor(ApplicationContext context) {
		this.context = context;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) {
		if (bean instanceof AbstractConnectionFactory connectionFactory) {
			bindConnectionFactoryToRegistry(getMeterRegistry(), beanName, connectionFactory);
		}
		return bean;
	}

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

	private MeterRegistry getMeterRegistry() {
		MeterRegistry meterRegistry = this.meterRegistry;
		if (meterRegistry == null) {
			meterRegistry = this.context.getBean(MeterRegistry.class);
			this.meterRegistry = meterRegistry;
		}
		return meterRegistry;
	}

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}

}
