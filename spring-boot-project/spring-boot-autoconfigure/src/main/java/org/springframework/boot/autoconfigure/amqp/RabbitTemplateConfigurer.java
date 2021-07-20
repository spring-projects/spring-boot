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

import java.time.Duration;
import java.util.List;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.util.Assert;

/**
 * Configure {@link RabbitTemplate} with sensible defaults.
 *
 * @author Stephane Nicoll
 * @since 2.3.0
 */
public class RabbitTemplateConfigurer {

	private MessageConverter messageConverter;

	private List<RabbitRetryTemplateCustomizer> retryTemplateCustomizers;

	private RabbitProperties rabbitProperties;

	/**
	 * Creates a new configurer.
	 * @deprecated since 2.6.0 for removal in 2.8.0 in favor of
	 * {@link #RabbitTemplateConfigurer(RabbitProperties)}
	 */
	@Deprecated
	public RabbitTemplateConfigurer() {

	}

	/**
	 * Creates a new configurer that will use the given {@code rabbitProperties}.
	 * @param rabbitProperties properties to use
	 * @since 2.6.0
	 */
	public RabbitTemplateConfigurer(RabbitProperties rabbitProperties) {
		Assert.notNull(rabbitProperties, "RabbitProperties must not be null");
		this.rabbitProperties = rabbitProperties;
	}

	/**
	 * Set the {@link MessageConverter} to use or {@code null} if the out-of-the-box
	 * converter should be used.
	 * @param messageConverter the {@link MessageConverter}
	 * @since 2.6.0
	 */
	public void setMessageConverter(MessageConverter messageConverter) {
		this.messageConverter = messageConverter;
	}

	/**
	 * Set the {@link RabbitRetryTemplateCustomizer} instances to use.
	 * @param retryTemplateCustomizers the retry template customizers
	 * @since 2.6.0
	 */
	public void setRetryTemplateCustomizers(List<RabbitRetryTemplateCustomizer> retryTemplateCustomizers) {
		this.retryTemplateCustomizers = retryTemplateCustomizers;
	}

	/**
	 * Set the {@link RabbitProperties} to use.
	 * @param rabbitProperties the {@link RabbitProperties}
	 * @deprecated since 2.6.0 for removal in 2.8.0 in favor of
	 * {@link #RabbitTemplateConfigurer(RabbitProperties)}
	 */
	@Deprecated
	protected void setRabbitProperties(RabbitProperties rabbitProperties) {
		this.rabbitProperties = rabbitProperties;
	}

	protected final RabbitProperties getRabbitProperties() {
		return this.rabbitProperties;
	}

	/**
	 * Configure the specified {@link RabbitTemplate}. The template can be further tuned
	 * and default settings can be overridden.
	 * @param template the {@link RabbitTemplate} instance to configure
	 * @param connectionFactory the {@link ConnectionFactory} to use
	 */
	public void configure(RabbitTemplate template, ConnectionFactory connectionFactory) {
		PropertyMapper map = PropertyMapper.get();
		template.setConnectionFactory(connectionFactory);
		if (this.messageConverter != null) {
			template.setMessageConverter(this.messageConverter);
		}
		template.setMandatory(determineMandatoryFlag());
		RabbitProperties.Template templateProperties = this.rabbitProperties.getTemplate();
		if (templateProperties.getRetry().isEnabled()) {
			template.setRetryTemplate(new RetryTemplateFactory(this.retryTemplateCustomizers)
					.createRetryTemplate(templateProperties.getRetry(), RabbitRetryTemplateCustomizer.Target.SENDER));
		}
		map.from(templateProperties::getReceiveTimeout).whenNonNull().as(Duration::toMillis)
				.to(template::setReceiveTimeout);
		map.from(templateProperties::getReplyTimeout).whenNonNull().as(Duration::toMillis)
				.to(template::setReplyTimeout);
		map.from(templateProperties::getExchange).to(template::setExchange);
		map.from(templateProperties::getRoutingKey).to(template::setRoutingKey);
		map.from(templateProperties::getDefaultReceiveQueue).whenNonNull().to(template::setDefaultReceiveQueue);
	}

	private boolean determineMandatoryFlag() {
		Boolean mandatory = this.rabbitProperties.getTemplate().getMandatory();
		return (mandatory != null) ? mandatory : this.rabbitProperties.isPublisherReturns();
	}

}
