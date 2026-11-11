/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.pulsar.autoconfigure;

import org.apache.pulsar.client.api.DeadLetterPolicy;
import org.apache.pulsar.client.api.DeadLetterPolicy.DeadLetterPolicyBuilder;

import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.util.Assert;

/**
 * Helper class used to map {@link PulsarProperties.Consumer.DeadLetterPolicy dead letter
 * policy properties}.
 *
 * @author Chris Bono
 * @author Phillip Webb
 */
final class DeadLetterPolicyMapper {

	private DeadLetterPolicyMapper() {
	}

	static DeadLetterPolicy map(PulsarProperties.Consumer.DeadLetterPolicy policy) {
		Assert.state(policy.getMaxRedeliverCount() > 0,
				"Pulsar DeadLetterPolicy must have a positive 'max-redelivery-count' property value");
		PropertyMapper map = PropertyMapper.get();
		DeadLetterPolicyBuilder builder = DeadLetterPolicy.builder();
		map.from(policy::getMaxRedeliverCount).to(builder::maxRedeliverCount);
		map.from(policy::getRetryLetterTopic).to(builder::retryLetterTopic);
		map.from(policy::getDeadLetterTopic).to(builder::deadLetterTopic);
		map.from(policy::getInitialSubscriptionName).to(builder::initialSubscriptionName);
		return builder.build();
	}

}
