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

import java.util.List;

import org.springframework.amqp.rabbit.config.AbstractRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties.ListenerRetry;
import org.springframework.retry.support.RetryTemplate;
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

	private List<RabbitRetryTemplateCustomizer> retryTemplateCustomizers;

	private RabbitProperties rabbitProperties;

	/**
	 * Creates a new configurer.
	 * @deprecated since 2.6.0 for removal in 2.8.0 in favor of
	 * {@link #AbstractRabbitListenerContainerFactoryConfigurer(RabbitProperties)}
	 */
	@Deprecated
	protected AbstractRabbitListenerContainerFactoryConfigurer() {

	}

	/**
	 * Creates a new configurer that will use the given {@code rabbitProperties}.
	 * @param rabbitProperties properties to use
	 * @since 2.6.0
	 */
	protected AbstractRabbitListenerContainerFactoryConfigurer(RabbitProperties rabbitProperties) {
		this.rabbitProperties = rabbitProperties;
	}

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
	 * Set the {@link RabbitRetryTemplateCustomizer} instances to use.
	 * @param retryTemplateCustomizers the retry template customizers
	 */
	protected void setRetryTemplateCustomizers(List<RabbitRetryTemplateCustomizer> retryTemplateCustomizers) {
		this.retryTemplateCustomizers = retryTemplateCustomizers;
	}

	/**
	 * Set the {@link RabbitProperties} to use.
	 * @param rabbitProperties the {@link RabbitProperties}
	 * @deprecated since 2.6.0 for removal in 2.8.0 in favor of
	 * {@link #AbstractRabbitListenerContainerFactoryConfigurer(RabbitProperties)}
	 */
	@Deprecated
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
		factory.setMissingQueuesFatal(configuration.isMissingQueuesFatal());
		factory.setDeBatchingEnabled(configuration.isDeBatchingEnabled());
		ListenerRetry retryConfig = configuration.getRetry();
		if (retryConfig.isEnabled()) {
			RetryInterceptorBuilder<?, ?> builder = (retryConfig.isStateless()) ? RetryInterceptorBuilder.stateless()
					: RetryInterceptorBuilder.stateful();
			RetryTemplate retryTemplate = new RetryTemplateFactory(this.retryTemplateCustomizers)
					.createRetryTemplate(retryConfig, RabbitRetryTemplateCustomizer.Target.LISTENER);
			builder.retryOperations(retryTemplate);
			MessageRecoverer recoverer = (this.messageRecoverer != null) ? this.messageRecoverer
					: new RejectAndDontRequeueRecoverer();
			builder.recoverer(recoverer);
			factory.setAdviceChain(builder.build());
		}
	}

}
