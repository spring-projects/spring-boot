/*
 * Copyright 2012-2025 the original author or authors.
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
import org.springframework.amqp.support.converter.AllowedListDeserializingMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.context.properties.source.InvalidConfigurationPropertyValueException;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * Configure {@link RabbitTemplate} with sensible defaults.
 *
 * @author Stephane Nicoll
 * @author Yanming Zhou
 * @since 2.3.0
 */
public class RabbitTemplateConfigurer {

	private MessageConverter messageConverter;

	private List<RabbitRetryTemplateCustomizer> retryTemplateCustomizers;

	private final RabbitProperties rabbitProperties;

	/**
	 * Creates a new configurer that will use the given {@code rabbitProperties}.
	 * @param rabbitProperties properties to use
	 * @since 2.6.0
	 */
	public RabbitTemplateConfigurer(RabbitProperties rabbitProperties) {
		Assert.notNull(rabbitProperties, "'rabbitProperties' must not be null");
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
		map.from(templateProperties::getReceiveTimeout)
			.whenNonNull()
			.as(Duration::toMillis)
			.to(template::setReceiveTimeout);
		map.from(templateProperties::getReplyTimeout)
			.whenNonNull()
			.as(Duration::toMillis)
			.to(template::setReplyTimeout);
		map.from(templateProperties::getExchange).to(template::setExchange);
		map.from(templateProperties::getRoutingKey).to(template::setRoutingKey);
		map.from(templateProperties::getDefaultReceiveQueue).whenNonNull().to(template::setDefaultReceiveQueue);
		map.from(templateProperties::isObservationEnabled).to(template::setObservationEnabled);
		map.from(templateProperties::getAllowedListPatterns)
			.whenNot(CollectionUtils::isEmpty)
			.to((allowedListPatterns) -> setAllowedListPatterns(template.getMessageConverter(), allowedListPatterns));
	}

	private void setAllowedListPatterns(MessageConverter messageConverter, List<String> allowedListPatterns) {
		if (messageConverter instanceof AllowedListDeserializingMessageConverter allowedListDeserializingMessageConverter) {
			allowedListDeserializingMessageConverter.setAllowedListPatterns(allowedListPatterns);
			return;
		}
		throw new InvalidConfigurationPropertyValueException("spring.rabbitmq.template.allowed-list-patterns",
				allowedListPatterns,
				"Allowed list patterns can only be applied to an AllowedListDeserializingMessageConverter");
	}

	private boolean determineMandatoryFlag() {
		Boolean mandatory = this.rabbitProperties.getTemplate().getMandatory();
		return (mandatory != null) ? mandatory : this.rabbitProperties.isPublisherReturns();
	}

}
