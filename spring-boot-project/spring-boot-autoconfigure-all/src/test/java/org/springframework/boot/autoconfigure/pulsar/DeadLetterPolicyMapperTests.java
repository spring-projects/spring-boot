/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.pulsar;

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
