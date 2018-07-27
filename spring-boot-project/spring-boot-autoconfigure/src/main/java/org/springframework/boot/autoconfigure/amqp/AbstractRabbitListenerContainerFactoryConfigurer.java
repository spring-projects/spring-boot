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

package org.springframework.boot.autoconfigure.amqp;

import org.springframework.amqp.rabbit.config.AbstractRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties.ListenerRetry;
import org.springframework.util.Assert;

/**
 * Configure {@link RabbitListenerContainerFactory} with sensible defaults.
 *
 * @param <T> the container factory type.
 * @author Gary Russell
 * @author Stephane Nicoll
 * @since 2.0.0
 */
public abstract class AbstractRabbitListenerContainerFactoryConfigurer<T extends AbstractRabbitListenerContainerFactory<?>> {

	private MessageConverter messageConverter;

	private MessageRecoverer messageRecoverer;

	private RabbitProperties rabbitProperties;

	/**
	 * Set the {@link MessageConverter} to use or {@code null} if the out-of-the-box
	 * converter should be used.
	 * @param messageConverter the {@link MessageConverter}
	 */
	protected void setMessageConverter(MessageConverter messageConverter) {
		this.messageConverter = messageConverter;
	}

	/**
	 * Set the {@link MessageRecoverer} to use or {@code null} to rely on the default.
	 * @param messageRecoverer the {@link MessageRecoverer}
	 */
	protected void setMessageRecoverer(MessageRecoverer messageRecoverer) {
		this.messageRecoverer = messageRecoverer;
	}

	/**
	 * Set the {@link RabbitProperties} to use.
	 * @param rabbitProperties the {@link RabbitProperties}
	 */
	protected void setRabbitProperties(RabbitProperties rabbitProperties) {
		this.rabbitProperties = rabbitProperties;
	}

	protected final RabbitProperties getRabbitProperties() {
		return this.rabbitProperties;
	}

	/**
	 * Configure the specified rabbit listener container factory. The factory can be
	 * further tuned and default settings can be overridden.
	 * @param factory the {@link AbstractRabbitListenerContainerFactory} instance to
	 * configure
	 * @param connectionFactory the {@link ConnectionFactory} to use
	 */
	public abstract void configure(T factory, ConnectionFactory connectionFactory);

	protected void configure(T factory, ConnectionFactory connectionFactory,
			RabbitProperties.AmqpContainer configuration) {
		Assert.notNull(factory, "Factory must not be null");
		Assert.notNull(connectionFactory, "ConnectionFactory must not be null");
		Assert.notNull(configuration, "Configuration must not be null");
		factory.setConnectionFactory(connectionFactory);
		if (this.messageConverter != null) {
			factory.setMessageConverter(this.messageConverter);
		}
		factory.setAutoStartup(configuration.isAutoStartup());
		if (configuration.getAcknowledgeMode() != null) {
			factory.setAcknowledgeMode(configuration.getAcknowledgeMode());
		}
		if (configuration.getPrefetch() != null) {
			factory.setPrefetchCount(configuration.getPrefetch());
		}
		if (configuration.getDefaultRequeueRejected() != null) {
			factory.setDefaultRequeueRejected(configuration.getDefaultRequeueRejected());
		}
		if (configuration.getIdleEventInterval() != null) {
			factory.setIdleEventInterval(configuration.getIdleEventInterval().toMillis());
		}
		ListenerRetry retryConfig = configuration.getRetry();
		if (retryConfig.isEnabled()) {
			RetryInterceptorBuilder<?> builder = (retryConfig.isStateless()
					? RetryInterceptorBuilder.stateless()
					: RetryInterceptorBuilder.stateful());
			builder.maxAttempts(retryConfig.getMaxAttempts());
			builder.backOffOptions(retryConfig.getInitialInterval().toMillis(),
					retryConfig.getMultiplier(), retryConfig.getMaxInterval().toMillis());
			MessageRecoverer recoverer = (this.messageRecoverer != null)
					? this.messageRecoverer : new RejectAndDontRequeueRecoverer();
			builder.recoverer(recoverer);
			factory.setAdviceChain(builder.build());
		}
	}

}
