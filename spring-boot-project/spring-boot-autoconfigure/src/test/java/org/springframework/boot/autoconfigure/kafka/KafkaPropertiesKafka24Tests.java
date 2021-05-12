/*
 * Copyright 2012-2021 the original author or authors.
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
import org.springframework.boot.testsupport.classpath.ClassPathOverrides;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link KafkaProperties} with Kafka 2.4.
 *
 * @author Stephane Nicoll
 */
@ClassPathOverrides("org.apache.kafka:kafka-clients:2.4.1")
class KafkaPropertiesKafka24Tests {

	@SuppressWarnings("rawtypes")
	@Test
	void isolationLevelEnumConsistentWithKafkaVersion() throws ClassNotFoundException {
		Class<?> isolationLevelClass = Class.forName("org.apache.kafka.common.requests.IsolationLevel");
		Enum[] original = ReflectionTestUtils.invokeMethod(isolationLevelClass, "values");
		assertThat(original).extracting(Enum::name).containsExactly(IsolationLevel.READ_UNCOMMITTED.name(),
				IsolationLevel.READ_COMMITTED.name());
		assertThat(original).extracting("id").containsExactly(IsolationLevel.READ_UNCOMMITTED.id(),
				IsolationLevel.READ_COMMITTED.id());
		assertThat(original).hasSize(IsolationLevel.values().length);
	}

}
