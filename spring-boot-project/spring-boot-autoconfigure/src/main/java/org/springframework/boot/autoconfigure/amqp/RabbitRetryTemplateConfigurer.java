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

import org.springframework.boot.autoconfigure.amqp.RabbitProperties.Retry;
import org.springframework.boot.autoconfigure.retry.RetryTemplateConfigurer;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

/**
 * A {@link RetryTemplateConfigurer} for RabbitMQ.
 *
 * @author Gary Russell
 * @since 2.0
 *
 */
public class RabbitRetryTemplateConfigurer implements RetryTemplateConfigurer<RabbitProperties.Retry> {

	@Override
	public void configure(RetryTemplate template, Retry properties) {
		PropertyMapper map = PropertyMapper.get();
		SimpleRetryPolicy policy = new SimpleRetryPolicy();
		map.from(properties::getMaxAttempts).to(policy::setMaxAttempts);
		template.setRetryPolicy(policy);
		ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
		map.from(properties::getInitialInterval)
				.to(backOffPolicy::setInitialInterval);
		map.from(properties::getMultiplier).to(backOffPolicy::setMultiplier);
		map.from(properties::getMaxInterval).to(backOffPolicy::setMaxInterval);
		template.setBackOffPolicy(backOffPolicy);
	}

}
