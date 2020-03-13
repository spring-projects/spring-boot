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

package org.springframework.boot.actuate.jet;

import com.hazelcast.core.Endpoint;
import com.hazelcast.core.HazelcastException;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.Job;
import com.hazelcast.jet.core.DAG;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.when;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link HazelcastJetHealthIndicator}.
 *
 * @author Ali Gurbuz
 */
class HazelcastJetHealthIndicatorTests {

	private final JetInstance jet = mock(JetInstance.class);

	@Test
	void hazelcastUp() {
		HazelcastInstance hazelcast = mock(HazelcastInstance.class);
		Endpoint endpoint = mock(Endpoint.class);
		Job job = mock(Job.class);
		when(this.jet.getHazelcastInstance()).thenReturn(hazelcast);
		when(this.jet.getName()).thenReturn("jet0-instance");
		when(hazelcast.getLocalEndpoint()).thenReturn(endpoint);
		when(endpoint.getUuid()).thenReturn("7581bb2f-879f-413f-b574-0071d7519eb0");
		when(this.jet.newJob(any(DAG.class))).thenReturn(job);
		Health health = new HazelcastJetHealthIndicator(this.jet).health();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails()).containsOnlyKeys("name", "uuid").containsEntry("name", "jet0-instance")
				.containsEntry("uuid", "7581bb2f-879f-413f-b574-0071d7519eb0");
	}

	@Test
	void hazelcastDown() {
		HazelcastInstance hazelcast = mock(HazelcastInstance.class);
		when(this.jet.getHazelcastInstance()).thenReturn(hazelcast);
		when(hazelcast.executeTransaction(any())).thenThrow(new HazelcastException());
		Health health = new HazelcastJetHealthIndicator(this.jet).health();
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
	}

}
