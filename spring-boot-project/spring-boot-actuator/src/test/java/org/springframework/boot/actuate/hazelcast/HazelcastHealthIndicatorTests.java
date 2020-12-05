/*
 * Copyright 2012-2019 the original author or authors.
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

import com.hazelcast.core.Endpoint;
import com.hazelcast.core.HazelcastException;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.transaction.TransactionalTask;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link HazelcastHealthIndicator}.
 *
 * @author Dmytro Nosan
 * @author Stephane Nicoll
 */
class HazelcastHealthIndicatorTests {

	private final HazelcastInstance hazelcast = mock(HazelcastInstance.class);

	@Test
	void hazelcastUp() {
		Endpoint endpoint = mock(Endpoint.class);
		given(this.hazelcast.getName()).willReturn("hz0-instance");
		given(this.hazelcast.getLocalEndpoint()).willReturn(endpoint);
		given(endpoint.getUuid()).willReturn("7581bb2f-879f-413f-b574-0071d7519eb0");
		given(this.hazelcast.executeTransaction(any())).willAnswer((invocation) -> {
			TransactionalTask<?> task = invocation.getArgument(0);
			return task.execute(null);
		});
		Health health = new HazelcastHealthIndicator(this.hazelcast).health();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails()).containsOnlyKeys("name", "uuid").containsEntry("name", "hz0-instance")
				.containsEntry("uuid", "7581bb2f-879f-413f-b574-0071d7519eb0");
	}

	@Test
	void hazelcastDown() {
		given(this.hazelcast.executeTransaction(any())).willThrow(new HazelcastException());
		Health health = new HazelcastHealthIndicator(this.hazelcast).health();
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
	}

}
