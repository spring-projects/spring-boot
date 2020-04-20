/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.autoconfigure.kafka;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.kafka.KafkaProperties.IsolationLevel;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties.Listener;
import org.springframework.kafka.listener.ContainerProperties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link KafkaProperties}.
 *
 * @author Stephane Nicoll
 */
class KafkaPropertiesTests {

	@Test
	void isolationLevelEnumConsistentWithKafkaVersion() {
		org.apache.kafka.common.IsolationLevel[] original = org.apache.kafka.common.IsolationLevel.values();
		assertThat(original).extracting("name").containsExactly(IsolationLevel.READ_UNCOMMITTED.name(),
				IsolationLevel.READ_COMMITTED.name());
		assertThat(original).extracting("id").containsExactly(IsolationLevel.READ_UNCOMMITTED.id(),
				IsolationLevel.READ_COMMITTED.id());
		assertThat(original).hasSize(IsolationLevel.values().length);
	}

	@Test
	void listenerDefaultValuesAreConsistent() {
		ContainerProperties container = new ContainerProperties("test");
		Listener listenerProperties = new KafkaProperties().getListener();
		assertThat(listenerProperties.isMissingTopicsFatal()).isEqualTo(container.isMissingTopicsFatal());
	}

}
