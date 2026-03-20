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

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.retry.RetryTemplate;

/**
 * Callback interface that can be used to customize
 * {@linkplain RabbitRetryTemplateSettings retry settings} used by {@link RabbitTemplate}.
 *
 * @author Stephane Nicoll
 * @since 4.0.0
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
