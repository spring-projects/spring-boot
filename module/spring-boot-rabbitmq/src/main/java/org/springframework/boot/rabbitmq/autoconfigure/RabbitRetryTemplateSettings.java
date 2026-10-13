/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.rabbitmq.autoconfigure;

import java.util.LinkedList;

import org.springframework.boot.retry.RetryPolicySettings;
import org.springframework.core.retry.RetryListener;
import org.springframework.core.retry.RetryTemplate;

/**
 * Define the settings of a {@link RetryTemplate}.
 *
 * @author Stephane Nicoll
 * @since 4.2.0
 */
public class RabbitRetryTemplateSettings {

	private final RetryPolicySettings retryPolicySettings;

	private final LinkedList<RetryListener> retryListeners;

	RabbitRetryTemplateSettings(RetryPolicySettings retryPolicySettings) {
		this.retryPolicySettings = retryPolicySettings;
		this.retryListeners = new LinkedList<>();
	}

	/**
	 * Return the {@link RetryPolicySettings} to use to customize the retry policy.
	 * @return the retry policy settings
	 */
	public RetryPolicySettings getRetryPolicySettings() {
		return this.retryPolicySettings;
	}

	/**
	 * Return the list of {@link RetryListener} instances. The returned list is mutable
	 * and can be used to add listeners.
	 * @return the retry listeners
	 */
	public LinkedList<RetryListener> getRetryListeners() {
		return this.retryListeners;
	}

}
