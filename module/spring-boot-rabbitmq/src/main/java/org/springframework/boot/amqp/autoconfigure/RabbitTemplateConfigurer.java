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

import java.time.Duration;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.support.micrometer.RabbitTemplateObservationConvention;
import org.springframework.amqp.support.converter.AllowedListDeserializingMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.context.properties.source.InvalidConfigurationPropertyValueException;
import org.springframework.core.retry.RetryListener;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.core.retry.support.CompositeRetryListener;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * Configure {@link RabbitTemplate} with sensible defaults tuned using configuration
 * properties.
 * <p>
 * Can be injected into application code and used to define a custom
 * {@code RabbitTemplate} whose configuration is based upon that produced by
 * auto-configuration.
 *
 * @author Stephane Nicoll
 * @author Yanming Zhou
 * @since 4.0.0
 */
public class RabbitTemplateConfigurer {

	private @Nullable MessageConverter messageConverter;

	private @Nullable List<RabbitTemplateRetrySettingsCustomizer> retrySettingsCustomizers;

	private @Nullable RabbitTemplateObservationConvention observationConvention;

	private final RabbitProperties rabbitProperties;

	/**
	 * Creates a new configurer that will use the given {@code rabbitProperties}.
	 * @param rabbitProperties properties to use
	 */
	public RabbitTemplateConfigurer(RabbitProperties rabbitProperties) {
		Assert.notNull(rabbitProperties, "'rabbitProperties' must not be null");
		this.rabbitProperties = rabbitProperties;
	}

	/**
	 * Set the {@link MessageConverter} to use or {@code null} if the out-of-the-box
	 * converter should be used.
	 * @param messageConverter the {@link MessageConverter}
	 */
	public void setMessageConverter(@Nullable MessageConverter messageConverter) {
		this.messageConverter = messageConverter;
	}

	/**
	 * Set the {@link RabbitTemplateRetrySettingsCustomizer} instances to use.
	 * @param retrySettingsCustomizers the retry settings customizers
	 */
	public void setRetrySettingsCustomizers(
			@Nullable List<RabbitTemplateRetrySettingsCustomizer> retrySettingsCustomizers) {
		this.retrySettingsCustomizers = retrySettingsCustomizers;
	}

	/**
	 * Sets the observation convention to use.
	 * @param observationConvention the observation convention to use
	 * @since 4.1.0
	 */
	public void setObservationConvention(@Nullable RabbitTemplateObservationConvention observationConvention) {
		this.observationConvention = observationConvention;
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
			template.setRetryTemplate(createRetryTemplate(templateProperties.getRetry()));
		}
		map.from(templateProperties::getReceiveTimeout).as(Duration::toMillis).to(template::setReceiveTimeout);
		map.from(templateProperties::getReplyTimeout).as(Duration::toMillis).to(template::setReplyTimeout);
		map.from(templateProperties::getExchange).to(template::setExchange);
		map.from(templateProperties::getRoutingKey).to(template::setRoutingKey);
		map.from(templateProperties::getDefaultReceiveQueue).to(template::setDefaultReceiveQueue);
		map.from(templateProperties::isObservationEnabled).to(template::setObservationEnabled);
		map.from(templateProperties::getAllowedListPatterns)
			.whenNot(CollectionUtils::isEmpty)
			.to((allowedListPatterns) -> setAllowedListPatterns(template.getMessageConverter(), allowedListPatterns));
		if (this.observationConvention != null) {
			template.setObservationConvention(this.observationConvention);
		}
	}

	protected RetryTemplate createRetryTemplate(RabbitProperties.Retry properties) {
		RabbitRetryTemplateSettings retrySettings = new RabbitRetryTemplateSettings(
				properties.initializeRetryPolicySettings());
		if (this.retrySettingsCustomizers != null) {
			for (RabbitTemplateRetrySettingsCustomizer customizer : this.retrySettingsCustomizers) {
				customizer.customize(retrySettings);
			}
		}
		RetryPolicy retryPolicy = retrySettings.getRetryPolicySettings().createRetryPolicy();
		RetryListener retryListener = createRetryListener(retrySettings.getRetryListeners());
		RetryTemplate retryTemplate = new RetryTemplate(retryPolicy);
		if (retryListener != null) {
			retryTemplate.setRetryListener(retryListener);
		}
		return retryTemplate;
	}

	private @Nullable RetryListener createRetryListener(List<RetryListener> configuredListeners) {
		if (configuredListeners.size() > 1) {
			return new CompositeRetryListener(configuredListeners);
		}
		if (configuredListeners.size() == 1) {
			return configuredListeners.get(0);
		}
		return null;
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
