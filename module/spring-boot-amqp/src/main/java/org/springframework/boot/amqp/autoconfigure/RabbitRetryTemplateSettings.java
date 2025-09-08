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

import java.util.LinkedList;

import org.springframework.boot.retry.RetryPolicySettings;
import org.springframework.core.retry.RetryListener;
import org.springframework.core.retry.RetryTemplate;

/**
 * Define the settings of a {@link RetryTemplate}.
 *
 * @author Stephane Nicoll
 * @since 4.0.0
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
