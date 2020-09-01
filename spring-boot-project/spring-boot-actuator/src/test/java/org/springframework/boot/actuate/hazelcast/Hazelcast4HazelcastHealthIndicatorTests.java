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

package org.springframework.boot.actuate.hazelcast;

import java.io.IOException;

import com.hazelcast.core.HazelcastException;
import com.hazelcast.core.HazelcastInstance;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.autoconfigure.hazelcast.HazelcastInstanceFactory;
import org.springframework.boot.testsupport.classpath.ClassPathExclusions;
import org.springframework.boot.testsupport.classpath.ClassPathOverrides;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link HazelcastHealthIndicator} with Hazelcast 4.
 *
 * @author Dmytro Nosan
 * @author Stephane Nicoll
 */
@ClassPathExclusions("hazelcast*.jar")
@ClassPathOverrides("com.hazelcast:hazelcast:4.0")
class Hazelcast4HazelcastHealthIndicatorTests {

	@Test
	void hazelcastUp() throws IOException {
		HazelcastInstance hazelcast = new HazelcastInstanceFactory(new ClassPathResource("hazelcast-4.xml"))
				.getHazelcastInstance();
		try {
			Health health = new HazelcastHealthIndicator(hazelcast).health();
			assertThat(health.getStatus()).isEqualTo(Status.UP);
			assertThat(health.getDetails()).containsOnlyKeys("name", "uuid").containsEntry("name",
					"actuator-hazelcast-4");
			assertThat(health.getDetails().get("uuid")).asString().isNotEmpty();
		}
		finally {
			hazelcast.shutdown();
		}
	}

	@Test
	void hazelcastDown() {
		HazelcastInstance hazelcast = mock(HazelcastInstance.class);
		given(hazelcast.executeTransaction(any())).willThrow(new HazelcastException());
		Health health = new HazelcastHealthIndicator(hazelcast).health();
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
	}

}
