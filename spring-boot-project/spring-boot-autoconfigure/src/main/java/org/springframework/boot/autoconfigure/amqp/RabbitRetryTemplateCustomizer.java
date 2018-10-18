/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.amqp;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.AbstractMessageListenerContainer;
import org.springframework.retry.support.RetryTemplate;

/**
 * Callback interface that can be used to customize a {@link RetryTemplate} used as part
 * of the Rabbit infrastructure.
 *
 * @author Stephane Nicoll
 * @since 2.1.0
 */
@FunctionalInterface
public interface RabbitRetryTemplateCustomizer {

	/**
	 * Callback to customize a {@link RetryTemplate} instance used in the context of the
	 * specified {@link Target}.
	 * @param target the {@link Target} of the retry template
	 * @param retryTemplate the template to customize
	 */
	void customize(Target target, RetryTemplate retryTemplate);

	/**
	 * Define the available target for a {@link RetryTemplate}.
	 */
	enum Target {

		/**
		 * {@link RabbitTemplate} target.
		 */
		SENDER,

		/**
		 * {@link AbstractMessageListenerContainer} target.
		 */
		LISTENER

	}

}
