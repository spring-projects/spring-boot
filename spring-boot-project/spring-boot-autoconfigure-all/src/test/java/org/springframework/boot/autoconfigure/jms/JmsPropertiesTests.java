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

package org.springframework.boot.autoconfigure.jms;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import org.springframework.jms.listener.AbstractPollingMessageListenerContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JmsProperties}.
 *
 * @author Stephane Nicoll
 */
class JmsPropertiesTests {

	@Test
	void formatConcurrencyNull() {
		JmsProperties properties = new JmsProperties();
		assertThat(properties.getListener().formatConcurrency()).isNull();
	}

	@Test
	void formatConcurrencyOnlyLowerBound() {
		JmsProperties properties = new JmsProperties();
		properties.getListener().setMinConcurrency(2);
		assertThat(properties.getListener().formatConcurrency()).isEqualTo("2-2");
	}

	@Test
	void formatConcurrencyOnlyHigherBound() {
		JmsProperties properties = new JmsProperties();
		properties.getListener().setMaxConcurrency(5);
		assertThat(properties.getListener().formatConcurrency()).isEqualTo("1-5");
	}

	@Test
	void formatConcurrencyBothBounds() {
		JmsProperties properties = new JmsProperties();
		properties.getListener().setMinConcurrency(2);
		properties.getListener().setMaxConcurrency(10);
		assertThat(properties.getListener().formatConcurrency()).isEqualTo("2-10");
	}

	@Test
	void setDeliveryModeEnablesQoS() {
		JmsProperties properties = new JmsProperties();
		properties.getTemplate().setDeliveryMode(JmsProperties.DeliveryMode.PERSISTENT);
		assertThat(properties.getTemplate().determineQosEnabled()).isTrue();
	}

	@Test
	void setPriorityEnablesQoS() {
		JmsProperties properties = new JmsProperties();
		properties.getTemplate().setPriority(6);
		assertThat(properties.getTemplate().determineQosEnabled()).isTrue();
	}

	@Test
	void setTimeToLiveEnablesQoS() {
		JmsProperties properties = new JmsProperties();
		properties.getTemplate().setTimeToLive(Duration.ofSeconds(5));
		assertThat(properties.getTemplate().determineQosEnabled()).isTrue();
	}

	@Test
	void defaultReceiveTimeoutMatchesListenerContainersDefault() {
		assertThat(new JmsProperties().getListener().getReceiveTimeout())
			.hasMillis(AbstractPollingMessageListenerContainer.DEFAULT_RECEIVE_TIMEOUT);
	}

}
