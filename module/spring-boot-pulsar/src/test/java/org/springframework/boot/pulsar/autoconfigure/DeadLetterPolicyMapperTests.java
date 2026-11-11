/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.pulsar.autoconfigure;

import org.apache.pulsar.client.api.DeadLetterPolicy;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link DeadLetterPolicyMapper}.
 *
 * @author Chris Bono
 * @author Phillip Webb
 */
class DeadLetterPolicyMapperTests {

	@Test
	void map() {
		PulsarProperties.Consumer.DeadLetterPolicy properties = new PulsarProperties.Consumer.DeadLetterPolicy();
		properties.setMaxRedeliverCount(100);
		properties.setRetryLetterTopic("my-retry-topic");
		properties.setDeadLetterTopic("my-dlt-topic");
		properties.setInitialSubscriptionName("my-initial-subscription");
		DeadLetterPolicy policy = DeadLetterPolicyMapper.map(properties);
		assertThat(policy.getMaxRedeliverCount()).isEqualTo(100);
		assertThat(policy.getRetryLetterTopic()).isEqualTo("my-retry-topic");
		assertThat(policy.getDeadLetterTopic()).isEqualTo("my-dlt-topic");
		assertThat(policy.getInitialSubscriptionName()).isEqualTo("my-initial-subscription");
	}

	@Test
	void mapWhenMaxRedeliverCountIsNotPositiveThrowsException() {
		PulsarProperties.Consumer.DeadLetterPolicy properties = new PulsarProperties.Consumer.DeadLetterPolicy();
		properties.setMaxRedeliverCount(0);
		assertThatIllegalStateException().isThrownBy(() -> DeadLetterPolicyMapper.map(properties))
			.withMessage("Pulsar DeadLetterPolicy must have a positive 'max-redelivery-count' property value");
	}

}
