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

package org.springframework.boot.actuate.couchbase;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.couchbase.client.core.message.internal.DiagnosticsReport;
import com.couchbase.client.core.message.internal.EndpointHealth;
import com.couchbase.client.core.service.ServiceType;
import com.couchbase.client.core.state.LifecycleState;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.bucket.BucketInfo;
import com.couchbase.client.java.bucket.BucketManager;
import com.couchbase.client.java.cluster.ClusterInfo;
import com.couchbase.client.java.util.features.Version;
import org.junit.Test;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.data.couchbase.core.CouchbaseOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link CouchbaseHealthIndicator}
 *
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 */
public class CouchbaseHealthIndicatorTests {

	@Test
	public void couchbaseOperationsIsUp() throws UnknownHostException {
		BucketInfo bucketInfo = mock(BucketInfo.class);
		given(bucketInfo.nodeList()).willReturn(Collections.singletonList(InetAddress.getByName("127.0.0.1")));
		BucketManager bucketManager = mock(BucketManager.class);
		given(bucketManager.info(2000, TimeUnit.MILLISECONDS)).willReturn(bucketInfo);
		Bucket bucket = mock(Bucket.class);
		given(bucket.bucketManager()).willReturn(bucketManager);
		ClusterInfo clusterInfo = mock(ClusterInfo.class);
		given(clusterInfo.getAllVersions()).willReturn(Collections.singletonList(new Version(1, 2, 3)));
		CouchbaseOperations couchbaseOperations = mock(CouchbaseOperations.class);
		given(couchbaseOperations.getCouchbaseBucket()).willReturn(bucket);
		given(couchbaseOperations.getCouchbaseClusterInfo()).willReturn(clusterInfo);
		@SuppressWarnings("deprecation")
		CouchbaseHealthIndicator healthIndicator = new CouchbaseHealthIndicator(couchbaseOperations,
				Duration.ofSeconds(2));
		Health health = healthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails()).containsOnly(entry("versions", "1.2.3"), entry("nodes", "/127.0.0.1"));
		verify(clusterInfo).getAllVersions();
		verify(bucketInfo).nodeList();
	}

	@Test
	public void couchbaseOperationsTimeout() {
		BucketManager bucketManager = mock(BucketManager.class);
		given(bucketManager.info(1500, TimeUnit.MILLISECONDS))
				.willThrow(new RuntimeException(new TimeoutException("timeout, expected")));
		Bucket bucket = mock(Bucket.class);
		given(bucket.bucketManager()).willReturn(bucketManager);
		CouchbaseOperations couchbaseOperations = mock(CouchbaseOperations.class);
		given(couchbaseOperations.getCouchbaseBucket()).willReturn(bucket);
		@SuppressWarnings("deprecation")
		CouchbaseHealthIndicator healthIndicator = new CouchbaseHealthIndicator(couchbaseOperations,
				Duration.ofMillis(1500));
		Health health = healthIndicator.health();
		assertThat((String) health.getDetails().get("error")).contains("timeout, expected");
	}

	@Test
	public void couchbaseOperationsIsDown() {
		CouchbaseOperations couchbaseOperations = mock(CouchbaseOperations.class);
		given(couchbaseOperations.getCouchbaseClusterInfo()).willThrow(new IllegalStateException("test, expected"));
		@SuppressWarnings("deprecation")
		CouchbaseHealthIndicator healthIndicator = new CouchbaseHealthIndicator(couchbaseOperations,
				Duration.ofSeconds(1));
		Health health = healthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertThat((String) health.getDetails().get("error")).contains("test, expected");
		verify(couchbaseOperations).getCouchbaseClusterInfo();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void couchbaseClusterIsUp() {
		Cluster cluster = mock(Cluster.class);
		CouchbaseHealthIndicator healthIndicator = new CouchbaseHealthIndicator(cluster);
		List<EndpointHealth> endpoints = Arrays.asList(new EndpointHealth(ServiceType.BINARY, LifecycleState.CONNECTED,
				new InetSocketAddress(0), new InetSocketAddress(0), 1234, "endpoint-1"));
		DiagnosticsReport diagnostics = new DiagnosticsReport(endpoints, "test-sdk", "test-id");
		given(cluster.diagnostics()).willReturn(diagnostics);
		Health health = healthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails()).containsEntry("sdk", "test-sdk");
		assertThat(health.getDetails()).containsKey("endpoints");
		assertThat((List<Map<String, Object>>) health.getDetails().get("endpoints")).hasSize(1);
		verify(cluster).diagnostics();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void couchbaseClusterIsDown() {
		Cluster cluster = mock(Cluster.class);
		CouchbaseHealthIndicator healthIndicator = new CouchbaseHealthIndicator(cluster);
		List<EndpointHealth> endpoints = Arrays.asList(
				new EndpointHealth(ServiceType.BINARY, LifecycleState.CONNECTED, new InetSocketAddress(0),
						new InetSocketAddress(0), 1234, "endpoint-1"),
				new EndpointHealth(ServiceType.BINARY, LifecycleState.CONNECTING, new InetSocketAddress(0),
						new InetSocketAddress(0), 1234, "endpoint-2"));
		DiagnosticsReport diagnostics = new DiagnosticsReport(endpoints, "test-sdk", "test-id");
		given(cluster.diagnostics()).willReturn(diagnostics);
		Health health = healthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertThat(health.getDetails()).containsEntry("sdk", "test-sdk");
		assertThat(health.getDetails()).containsKey("endpoints");
		assertThat((List<Map<String, Object>>) health.getDetails().get("endpoints")).hasSize(2);
		verify(cluster).diagnostics();
	}

}
