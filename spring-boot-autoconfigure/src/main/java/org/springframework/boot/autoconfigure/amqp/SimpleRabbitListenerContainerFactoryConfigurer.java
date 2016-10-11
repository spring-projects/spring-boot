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

package org.springframework.boot.autoconfigure.amqp;

import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties.ListenerRetry;
import org.springframework.util.Assert;

/**
 * Configure {@link RabbitListenerContainerFactory} with sensible defaults.
 *
 * @author Stephane Nicoll
 * @author Gary Russell
 * @since 1.3.3
 */
public final class SimpleRabbitListenerContainerFactoryConfigurer {

	private MessageConverter messageConverter;

	private RabbitProperties rabbitProperties;

	/**
	 * Set the {@link MessageConverter} to use or {@code null} if the out-of-the-box
	 * converter should be used.
	 * @param messageConverter the {@link MessageConverter}
	 */
	void setMessageConverter(MessageConverter messageConverter) {
		this.messageConverter = messageConverter;
	}

	/**
	 * Set the {@link RabbitProperties} to use.
	 * @param rabbitProperties the {@link RabbitProperties}
	 */
	void setRabbitProperties(RabbitProperties rabbitProperties) {
		this.rabbitProperties = rabbitProperties;
	}

	/**
	 * Configure the specified rabbit listener container factory. The factory can be
	 * further tuned and default settings can be overridden.
	 * @param factory the {@link SimpleRabbitListenerContainerFactory} instance to
	 * configure
	 * @param connectionFactory the {@link ConnectionFactory} to use
	 */
	public void configure(SimpleRabbitListenerContainerFactory factory,
			ConnectionFactory connectionFactory) {
		Assert.notNull(factory, "Factory must not be null");
		Assert.notNull(connectionFactory, "ConnectionFactory must not be null");
		factory.setConnectionFactory(connectionFactory);
		if (this.messageConverter != null) {
			factory.setMessageConverter(this.messageConverter);
		}
		RabbitProperties.Listener listenerConfig = this.rabbitProperties.getListener();
		factory.setAutoStartup(listenerConfig.isAutoStartup());
		if (listenerConfig.getAcknowledgeMode() != null) {
			factory.setAcknowledgeMode(listenerConfig.getAcknowledgeMode());
		}
		if (listenerConfig.getConcurrency() != null) {
			factory.setConcurrentConsumers(listenerConfig.getConcurrency());
		}
		if (listenerConfig.getMaxConcurrency() != null) {
			factory.setMaxConcurrentConsumers(listenerConfig.getMaxConcurrency());
		}
		if (listenerConfig.getPrefetch() != null) {
			factory.setPrefetchCount(listenerConfig.getPrefetch());
		}
		if (listenerConfig.getTransactionSize() != null) {
			factory.setTxSize(listenerConfig.getTransactionSize());
		}
		if (listenerConfig.getDefaultRequeueRejected() != null) {
			factory.setDefaultRequeueRejected(listenerConfig.getDefaultRequeueRejected());
		}
		if (listenerConfig.getIdleEventInterval() != null) {
			factory.setIdleEventInterval(listenerConfig.getIdleEventInterval());
		}
		ListenerRetry retryConfig = listenerConfig.getRetry();
		if (retryConfig.isEnabled()) {
			RetryInterceptorBuilder<?> builder = (retryConfig.isStateless()
					? RetryInterceptorBuilder.stateless()
					: RetryInterceptorBuilder.stateful());
			builder.maxAttempts(retryConfig.getMaxAttempts());
			builder.backOffOptions(retryConfig.getInitialInterval(),
					retryConfig.getMultiplier(), retryConfig.getMaxInterval());
			builder.recoverer(new RejectAndDontRequeueRecoverer());
			factory.setAdviceChain(builder.build());
		}

	}

}
