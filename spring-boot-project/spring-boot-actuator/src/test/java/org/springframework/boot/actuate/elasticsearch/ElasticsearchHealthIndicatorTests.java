/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.elasticsearch;

import java.util.Map;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.ElasticsearchTimeoutException;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.support.PlainActionFuture;
import org.elasticsearch.client.AdminClient;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.ClusterAdminClient;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.block.ClusterBlocks;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.cluster.node.DiscoveryNodes;
import org.elasticsearch.cluster.routing.RoutingTable;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

/**
 * Test for {@link ElasticsearchHealthIndicator}.
 *
 * @author Andy Wilkinson
 */
public class ElasticsearchHealthIndicatorTests {

	@Mock
	private Client client;

	@Mock
	private AdminClient admin;

	@Mock
	private ClusterAdminClient cluster;

	private ElasticsearchHealthIndicator indicator;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		given(this.client.admin()).willReturn(this.admin);
		given(this.admin.cluster()).willReturn(this.cluster);
		this.indicator = new ElasticsearchHealthIndicator(this.client, 100L);
	}

	@Test
	public void defaultConfigurationQueriesAllIndicesWith100msTimeout() {
		TestActionFuture responseFuture = new TestActionFuture();
		responseFuture.onResponse(new StubClusterHealthResponse());
		ArgumentCaptor<ClusterHealthRequest> requestCaptor = ArgumentCaptor
				.forClass(ClusterHealthRequest.class);
		given(this.cluster.health(requestCaptor.capture())).willReturn(responseFuture);
		Health health = this.indicator.health();
		assertThat(responseFuture.getTimeout).isEqualTo(100L);
		assertThat(requestCaptor.getValue().indices()).contains("_all");
		assertThat(health.getStatus()).isEqualTo(Status.UP);
	}

	@Test
	public void certainIndices() {
		this.indicator = new ElasticsearchHealthIndicator(this.client, 100L,
				"test-index-1", "test-index-2");
		PlainActionFuture<ClusterHealthResponse> responseFuture = new PlainActionFuture<>();
		responseFuture.onResponse(new StubClusterHealthResponse());
		ArgumentCaptor<ClusterHealthRequest> requestCaptor = ArgumentCaptor
				.forClass(ClusterHealthRequest.class);
		given(this.cluster.health(requestCaptor.capture())).willReturn(responseFuture);
		Health health = this.indicator.health();
		assertThat(requestCaptor.getValue().indices()).contains("test-index-1",
				"test-index-2");
		assertThat(health.getStatus()).isEqualTo(Status.UP);
	}

	@Test
	public void customTimeout() {
		this.indicator = new ElasticsearchHealthIndicator(this.client, 1000L);
		TestActionFuture responseFuture = new TestActionFuture();
		responseFuture.onResponse(new StubClusterHealthResponse());
		ArgumentCaptor<ClusterHealthRequest> requestCaptor = ArgumentCaptor
				.forClass(ClusterHealthRequest.class);
		given(this.cluster.health(requestCaptor.capture())).willReturn(responseFuture);
		this.indicator.health();
		assertThat(responseFuture.getTimeout).isEqualTo(1000L);
	}

	@Test
	public void healthDetails() {
		PlainActionFuture<ClusterHealthResponse> responseFuture = new PlainActionFuture<>();
		responseFuture.onResponse(new StubClusterHealthResponse());
		given(this.cluster.health(any(ClusterHealthRequest.class)))
				.willReturn(responseFuture);
		Health health = this.indicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		Map<String, Object> details = health.getDetails();
		assertDetail(details, "clusterName", "test-cluster");
		assertDetail(details, "activeShards", 1);
		assertDetail(details, "relocatingShards", 2);
		assertDetail(details, "activePrimaryShards", 3);
		assertDetail(details, "initializingShards", 4);
		assertDetail(details, "unassignedShards", 5);
		assertDetail(details, "numberOfNodes", 6);
		assertDetail(details, "numberOfDataNodes", 7);
	}

	@Test
	public void redResponseMapsToDown() {
		PlainActionFuture<ClusterHealthResponse> responseFuture = new PlainActionFuture<>();
		responseFuture.onResponse(new StubClusterHealthResponse(ClusterHealthStatus.RED));
		given(this.cluster.health(any(ClusterHealthRequest.class)))
				.willReturn(responseFuture);
		assertThat(this.indicator.health().getStatus()).isEqualTo(Status.DOWN);
	}

	@Test
	public void yellowResponseMapsToUp() {
		PlainActionFuture<ClusterHealthResponse> responseFuture = new PlainActionFuture<>();
		responseFuture
				.onResponse(new StubClusterHealthResponse(ClusterHealthStatus.YELLOW));
		given(this.cluster.health(any(ClusterHealthRequest.class)))
				.willReturn(responseFuture);
		assertThat(this.indicator.health().getStatus()).isEqualTo(Status.UP);
	}

	@Test
	public void responseTimeout() {
		PlainActionFuture<ClusterHealthResponse> responseFuture = new PlainActionFuture<>();
		given(this.cluster.health(any(ClusterHealthRequest.class)))
				.willReturn(responseFuture);
		Health health = this.indicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertThat((String) health.getDetails().get("error"))
				.contains(ElasticsearchTimeoutException.class.getName());
	}

	@SuppressWarnings("unchecked")
	private <T> void assertDetail(Map<String, Object> details, String detail, T value) {
		assertThat((T) details.get(detail)).isEqualTo(value);
	}

	private static final class StubClusterHealthResponse extends ClusterHealthResponse {

		private final ClusterHealthStatus status;

		private StubClusterHealthResponse() {
			this(ClusterHealthStatus.GREEN);
		}

		private StubClusterHealthResponse(ClusterHealthStatus status) {
			super("test-cluster", new String[0],
					new ClusterState(null, 0, null, null, RoutingTable.builder().build(),
							DiscoveryNodes.builder().build(),
							ClusterBlocks.builder().build(), null, false));
			this.status = status;
		}

		@Override
		public int getActiveShards() {
			return 1;
		}

		@Override
		public int getRelocatingShards() {
			return 2;
		}

		@Override
		public int getActivePrimaryShards() {
			return 3;
		}

		@Override
		public int getInitializingShards() {
			return 4;
		}

		@Override
		public int getUnassignedShards() {
			return 5;
		}

		@Override
		public int getNumberOfNodes() {
			return 6;
		}

		@Override
		public int getNumberOfDataNodes() {
			return 7;
		}

		@Override
		public ClusterHealthStatus getStatus() {
			return this.status;
		}

	}

	private static class TestActionFuture
			extends PlainActionFuture<ClusterHealthResponse> {

		private long getTimeout = -1L;

		@Override
		public ClusterHealthResponse actionGet(long timeoutMillis)
				throws ElasticsearchException {
			this.getTimeout = timeoutMillis;
			return super.actionGet(timeoutMillis);
		}

	}

}
