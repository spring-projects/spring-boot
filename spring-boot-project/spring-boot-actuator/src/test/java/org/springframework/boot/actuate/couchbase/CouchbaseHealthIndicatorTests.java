/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.actuate.couchbase;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.couchbase.client.core.diagnostics.DiagnosticsResult;
import com.couchbase.client.core.diagnostics.EndpointDiagnostics;
import com.couchbase.client.core.endpoint.CircuitBreaker;
import com.couchbase.client.core.endpoint.EndpointState;
import com.couchbase.client.core.service.ServiceType;
import com.couchbase.client.java.Cluster;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link CouchbaseHealthIndicator}
 *
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 */
class CouchbaseHealthIndicatorTests {

	@Test
	@SuppressWarnings("unchecked")
	void couchbaseClusterIsUp() {
		Cluster cluster = mock(Cluster.class);
		CouchbaseHealthIndicator healthIndicator = new CouchbaseHealthIndicator(cluster);
		Map<ServiceType, List<EndpointDiagnostics>> endpoints = Collections.singletonMap(ServiceType.KV,
				Collections.singletonList(new EndpointDiagnostics(ServiceType.KV, EndpointState.CONNECTED,
						CircuitBreaker.State.DISABLED, "127.0.0.1", "127.0.0.1", Optional.empty(), Optional.of(1234L),
						Optional.of("endpoint-1"), Optional.empty())));

		DiagnosticsResult diagnostics = new DiagnosticsResult(endpoints, "test-sdk", "test-id");
		given(cluster.diagnostics()).willReturn(diagnostics);
		Health health = healthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails()).containsEntry("sdk", "test-sdk");
		assertThat(health.getDetails()).containsKey("endpoints");
		assertThat((List<Map<String, Object>>) health.getDetails().get("endpoints")).hasSize(1);
		then(cluster).should().diagnostics();
	}

	@Test
	@SuppressWarnings("unchecked")
	void couchbaseClusterIsDown() {
		Cluster cluster = mock(Cluster.class);
		CouchbaseHealthIndicator healthIndicator = new CouchbaseHealthIndicator(cluster);
		Map<ServiceType, List<EndpointDiagnostics>> endpoints = Collections.singletonMap(ServiceType.KV,
				Arrays.asList(
						new EndpointDiagnostics(ServiceType.KV, EndpointState.CONNECTED, CircuitBreaker.State.DISABLED,
								"127.0.0.1", "127.0.0.1", Optional.empty(), Optional.of(1234L),
								Optional.of("endpoint-1"), Optional.empty()),
						new EndpointDiagnostics(ServiceType.KV, EndpointState.CONNECTING, CircuitBreaker.State.DISABLED,
								"127.0.0.1", "127.0.0.1", Optional.empty(), Optional.of(1234L),
								Optional.of("endpoint-2"), Optional.empty())));
		DiagnosticsResult diagnostics = new DiagnosticsResult(endpoints, "test-sdk", "test-id");
		given(cluster.diagnostics()).willReturn(diagnostics);
		Health health = healthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertThat(health.getDetails()).containsEntry("sdk", "test-sdk");
		assertThat(health.getDetails()).containsKey("endpoints");
		assertThat((List<Map<String, Object>>) health.getDetails().get("endpoints")).hasSize(2);
		then(cluster).should().diagnostics();
	}

}
