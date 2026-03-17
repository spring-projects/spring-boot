/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.amqp.autoconfigure;

import java.util.List;
import java.util.concurrent.Executor;

import org.jspecify.annotations.Nullable;

import org.springframework.amqp.rabbit.config.AbstractRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.rabbit.support.micrometer.RabbitListenerObservationConvention;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.amqp.autoconfigure.RabbitProperties.ListenerRetry;
import org.springframework.boot.amqp.autoconfigure.RabbitProperties.Retry;
import org.springframework.boot.retry.RetryPolicySettings;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.util.Assert;

/**
 * Base class for configurers of sub-classes of
 * {@link AbstractRabbitListenerContainerFactory}.
 *
 * @param <T> the container factory type.
 * @author Gary Russell
 * @author Stephane Nicoll
 * @since 4.0.0
 */
public abstract class AbstractRabbitListenerContainerFactoryConfigurer<T extends AbstractRabbitListenerContainerFactory<?>> {

	private @Nullable MessageConverter messageConverter;

	private @Nullable MessageRecoverer messageRecoverer;

	private @Nullable List<RabbitListenerRetrySettingsCustomizer> retrySettingsCustomizers;

	private final RabbitProperties rabbitProperties;

	private @Nullable Executor taskExecutor;

	private @Nullable RabbitListenerObservationConvention observationConvention;

	/**
	 * Creates a new configurer that will use the given {@code rabbitProperties}.
	 * @param rabbitProperties properties to use
	 */
	protected AbstractRabbitListenerContainerFactoryConfigurer(RabbitProperties rabbitProperties) {
		this.rabbitProperties = rabbitProperties;
	}

	/**
	 * Set the {@link MessageConverter} to use or {@code null} if the out-of-the-box
	 * converter should be used.
	 * @param messageConverter the {@link MessageConverter}
	 */
	protected void setMessageConverter(@Nullable MessageConverter messageConverter) {
		this.messageConverter = messageConverter;
	}

	/**
	 * Set the {@link MessageRecoverer} to use or {@code null} to rely on the default.
	 * @param messageRecoverer the {@link MessageRecoverer}
	 */
	protected void setMessageRecoverer(@Nullable MessageRecoverer messageRecoverer) {
		this.messageRecoverer = messageRecoverer;
	}

	/**
	 * Set the {@link RabbitListenerRetrySettingsCustomizer} instances to use.
	 * @param retrySettingsCustomizers the retry settings customizers
	 */
	protected void setRetrySettingsCustomizers(
			@Nullable List<RabbitListenerRetrySettingsCustomizer> retrySettingsCustomizers) {
		this.retrySettingsCustomizers = retrySettingsCustomizers;
	}

	/**
	 * Set the task executor to use.
	 * @param taskExecutor the task executor
	 */
	public void setTaskExecutor(@Nullable Executor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	/**
	 * Sets the observation convention to use.
	 * @param observationConvention the observation convention to use
	 * @since 4.1.0
	 */
	protected void setObservationConvention(@Nullable RabbitListenerObservationConvention observationConvention) {
		this.observationConvention = observationConvention;
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
		Assert.notNull(factory, "'factory' must not be null");
		Assert.notNull(connectionFactory, "'connectionFactory' must not be null");
		Assert.notNull(configuration, "'configuration' must not be null");
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
		factory.setForceStop(configuration.isForceStop());
		if (this.taskExecutor != null) {
			factory.setTaskExecutor(this.taskExecutor);
		}
		factory.setObservationEnabled(configuration.isObservationEnabled());
		ListenerRetry retryConfig = configuration.getRetry();
		if (retryConfig.isEnabled()) {
			RetryInterceptorBuilder<?, ?> builder = (retryConfig.isStateless()) ? RetryInterceptorBuilder.stateless()
					: RetryInterceptorBuilder.stateful();
			builder.retryPolicy(createRetryPolicy(retryConfig));
			MessageRecoverer recoverer = (this.messageRecoverer != null) ? this.messageRecoverer
					: new RejectAndDontRequeueRecoverer();
			builder.recoverer(recoverer);
			factory.setAdviceChain(builder.build());
		}
		if (this.observationConvention != null) {
			factory.setObservationConvention(this.observationConvention);
		}
	}

	private RetryPolicy createRetryPolicy(Retry retryProperties) {
		RetryPolicySettings retrySettings = retryProperties.initializeRetryPolicySettings();
		if (this.retrySettingsCustomizers != null) {
			for (RabbitListenerRetrySettingsCustomizer customizer : this.retrySettingsCustomizers) {
				customizer.customize(retrySettings);
			}
		}
		return retrySettings.createRetryPolicy();
	}

}
