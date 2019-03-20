/*
 * Copyright 2012-2018 the original author or authors.
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

import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

/**
 * Factory to create {@link RetryTemplate} instance from properties defined in
 * {@link RabbitProperties}.
 *
 * @author Stephane Nicoll
 */
class RetryTemplateFactory {

	private final List<RabbitRetryTemplateCustomizer> customizers;

	RetryTemplateFactory(List<RabbitRetryTemplateCustomizer> customizers) {
		this.customizers = customizers;
	}

	public RetryTemplate createRetryTemplate(RabbitProperties.Retry properties,
			RabbitRetryTemplateCustomizer.Target target) {
		PropertyMapper map = PropertyMapper.get();
		RetryTemplate template = new RetryTemplate();
		SimpleRetryPolicy policy = new SimpleRetryPolicy();
		map.from(properties::getMaxAttempts).to(policy::setMaxAttempts);
		template.setRetryPolicy(policy);
		ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
		map.from(properties::getInitialInterval).whenNonNull().as(Duration::toMillis)
				.to(backOffPolicy::setInitialInterval);
		map.from(properties::getMultiplier).to(backOffPolicy::setMultiplier);
		map.from(properties::getMaxInterval).whenNonNull().as(Duration::toMillis)
				.to(backOffPolicy::setMaxInterval);
		template.setBackOffPolicy(backOffPolicy);
		if (this.customizers != null) {
			for (RabbitRetryTemplateCustomizer customizer : this.customizers) {
				customizer.customize(target, template);
			}
		}
		return template;
	}

}
