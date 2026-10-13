/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.rabbitmq.autoconfigure;

import org.springframework.boot.retry.RetryPolicySettings;
import org.springframework.core.retry.RetryPolicy;

/**
 * Callback interface that can be used to customize {@linkplain RetryPolicySettings retry
 * settings} used by message listeners.
 *
 * @author Stephane Nicoll
 * @since 4.2.0
 * @see RetryPolicy
 */
@FunctionalInterface
public interface RabbitListenerRetrySettingsCustomizer {

	/**
	 * Callback to customize the {@link RetryPolicySettings} to create the
	 * {@link RetryPolicy} used by message listeners.
	 * @param retrySettings the settings to customize
	 */
	void customize(RetryPolicySettings retrySettings);

}
