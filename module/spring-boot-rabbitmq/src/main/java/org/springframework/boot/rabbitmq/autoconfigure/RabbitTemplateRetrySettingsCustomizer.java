/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.rabbitmq.autoconfigure;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.retry.RetryTemplate;

/**
 * Callback interface that can be used to customize
 * {@linkplain RabbitRetryTemplateSettings retry settings} used by {@link RabbitTemplate}.
 *
 * @author Stephane Nicoll
 * @since 4.2.0
 * @see RabbitTemplateConfigurer
 * @see RetryTemplate
 */
@FunctionalInterface
public interface RabbitTemplateRetrySettingsCustomizer {

	/**
	 * Callback to customize the {@link RabbitRetryTemplateSettings} to create the
	 * {@link RetryTemplate} used as part of {@link RabbitTemplate} operations.
	 * @param retrySettings the settings to customize
	 */
	void customize(RabbitRetryTemplateSettings retrySettings);

}
