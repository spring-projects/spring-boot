/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.actuate.health;

import java.util.Arrays;
import java.util.Map;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.ElasticsearchTimeoutException;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthStatus;
import org.elasticsearch.action.support.PlainActionFuture;
import org.elasticsearch.client.AdminClient;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.ClusterAdminClient;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.block.ClusterBlocks;
import org.elasticsearch.cluster.node.DiscoveryNodes;
import org.elasticsearch.cluster.routing.RoutingTable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;

/**
 * Test for {@link ElasticsearchHealthIndicator}.
 *
 * @author Andy Wilkinson
 */
@RunWith(MockitoJUnitRunner.class)
public class ElasticsearchHealthIndicatorTests {

	@Mock
	private Client client;

	@Mock
	private AdminClient admin;

	@Mock
	private ClusterAdminClient cluster;

	private ElasticsearchHealthIndicator indicator;

	private ElasticsearchHealthIndicatorProperties properties = new ElasticsearchHealthIndicatorProperties();

	@Before
	public void setUp() throws Exception {
		given(this.client.admin()).willReturn(this.admin);
		given(this.admin.cluster()).willReturn(this.cluster);

		this.indicator = new ElasticsearchHealthIndicator(this.client, this.properties);
	}

	@Test
	public void defaultConfigurationQueriesAllIndicesWith100msTimeout() {
		TestActionFuture responseFuture = new TestActionFuture();
		responseFuture.onResponse(new StubClusterHealthResponse());
		ArgumentCaptor<ClusterHealthRequest> requestCaptor = ArgumentCaptor
				.forClass(ClusterHealthRequest.class);
		given(this.cluster.health(requestCaptor.capture())).willReturn(responseFuture);
		Health health = this.indicator.health();
		assertThat(responseFuture.getTimeout, is(100L));
		assertThat(requestCaptor.getValue().indices(), is(arrayContaining("_all")));
		assertThat(health.getStatus(), is(Status.UP));
	}

	@Test
	public void certainIndices() {
		PlainActionFuture<ClusterHealthResponse> responseFuture = new PlainActionFuture<ClusterHealthResponse>();
		responseFuture.onResponse(new StubClusterHealthResponse());
		ArgumentCaptor<ClusterHealthRequest> requestCaptor = ArgumentCaptor
				.forClass(ClusterHealthRequest.class);
		given(this.cluster.health(requestCaptor.capture())).willReturn(responseFuture);
		this.properties.getIndices()
				.addAll(Arrays.asList("test-index-1", "test-index-2"));
		Health health = this.indicator.health();
		assertThat(requestCaptor.getValue().indices(),
				is(arrayContaining("test-index-1", "test-index-2")));
		assertThat(health.getStatus(), is(Status.UP));
	}

	@Test
	public void customTimeout() {
		TestActionFuture responseFuture = new TestActionFuture();
		responseFuture.onResponse(new StubClusterHealthResponse());
		ArgumentCaptor<ClusterHealthRequest> requestCaptor = ArgumentCaptor
				.forClass(ClusterHealthRequest.class);
		given(this.cluster.health(requestCaptor.capture())).willReturn(responseFuture);
		this.properties.setResponseTimeout(1000L);
		this.indicator.health();
		assertThat(responseFuture.getTimeout, is(1000L));
	}

	@Test
	public void healthDetails() {
		PlainActionFuture<ClusterHealthResponse> responseFuture = new PlainActionFuture<ClusterHealthResponse>();
		responseFuture.onResponse(new StubClusterHealthResponse());
		given(this.cluster.health(any(ClusterHealthRequest.class))).willReturn(
				responseFuture);
		Health health = this.indicator.health();
		assertThat(health.getStatus(), is(Status.UP));
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
		PlainActionFuture<ClusterHealthResponse> responseFuture = new PlainActionFuture<ClusterHealthResponse>();
		responseFuture.onResponse(new StubClusterHealthResponse(ClusterHealthStatus.RED));
		given(this.cluster.health(any(ClusterHealthRequest.class))).willReturn(
				responseFuture);
		assertThat(this.indicator.health().getStatus(), is(Status.DOWN));
	}

	@Test
	public void yellowResponseMapsToUp() {
		PlainActionFuture<ClusterHealthResponse> responseFuture = new PlainActionFuture<ClusterHealthResponse>();
		responseFuture.onResponse(new StubClusterHealthResponse(
				ClusterHealthStatus.YELLOW));
		given(this.cluster.health(any(ClusterHealthRequest.class))).willReturn(
				responseFuture);
		assertThat(this.indicator.health().getStatus(), is(Status.UP));
	}

	@Test
	public void responseTimeout() {
		PlainActionFuture<ClusterHealthResponse> responseFuture = new PlainActionFuture<ClusterHealthResponse>();
		given(this.cluster.health(any(ClusterHealthRequest.class))).willReturn(
				responseFuture);
		Health health = this.indicator.health();
		assertThat(health.getStatus(), is(Status.DOWN));
		assertThat((String) health.getDetails().get("error"),
				containsString(ElasticsearchTimeoutException.class.getName()));
	}

	@SuppressWarnings("unchecked")
	private <T> void assertDetail(Map<String, Object> details, String detail, T value) {
		assertThat((T) details.get(detail), is(equalTo(value)));
	}

	private static class StubClusterHealthResponse extends ClusterHealthResponse {

		private final ClusterHealthStatus status;

		private StubClusterHealthResponse() {
			this(ClusterHealthStatus.GREEN);
		}

		private StubClusterHealthResponse(ClusterHealthStatus status) {
			super("test-cluster", new String[0], new ClusterState(null, 0, null,
					RoutingTable.builder().build(), DiscoveryNodes.builder().build(),
					ClusterBlocks.builder().build(), null));
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

	private static class TestActionFuture extends
			PlainActionFuture<ClusterHealthResponse> {

		private long getTimeout = -1L;

		@Override
		public ClusterHealthResponse actionGet(long timeoutMillis)
				throws ElasticsearchException {
			this.getTimeout = timeoutMillis;
			return super.actionGet(timeoutMillis);
		}

	}
}
