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

package org.springframework.boot.actuate.kafka;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterOptions;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.common.KafkaFuture;
import org.assertj.core.data.MapEntry;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link KafkaHealthIndicator}
 *
 * @author Juan Rada
 */
public class KafkaHealthIndicatorTests {

	private static final Long RESPONSE_TIME = 10L;
	private static final String CLUSTER_ID = "abc_123";

	@Mock
	private AdminClient adminClient;

	@Mock
	private DescribeClusterResult describeClusterResult;

	@Mock
	private KafkaFuture<String> clusterIdFuture;

	@Captor
	private ArgumentCaptor<DescribeClusterOptions> describeOptionsCaptor;

	private KafkaHealthIndicator healthIndicator;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		this.healthIndicator = new KafkaHealthIndicator(this.adminClient, RESPONSE_TIME);
		given(this.describeClusterResult.clusterId()).willReturn(this.clusterIdFuture);
	}

	@Test
	public void kafkaIsUp() throws Exception {
		given(this.adminClient.describeCluster(any(DescribeClusterOptions.class)))
				.willReturn(this.describeClusterResult);
		given(this.clusterIdFuture.get()).willReturn(CLUSTER_ID);
		Health health = this.healthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails()).containsOnly(MapEntry.entry("clusterId", CLUSTER_ID));
		verify(this.adminClient).describeCluster(this.describeOptionsCaptor.capture());
		assertThat(this.describeOptionsCaptor.getValue().timeoutMs()).isEqualTo(RESPONSE_TIME.intValue());
	}

	@Test
	public void kafkaIsDown() {
		given(this.adminClient.describeCluster(any(DescribeClusterOptions.class)))
				.willThrow(new IllegalStateException("test, expected"));
		Health health = this.healthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertThat((String) health.getDetails().get("error")).contains("test, expected");
	}
}
