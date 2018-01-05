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

import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health.Builder;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.util.Assert;

/**
 * {@link HealthIndicator} for Kafka cluster.
 *
 * @author Juan Rada
 */
public class KafkaHealthIndicator extends AbstractHealthIndicator {

	private final AdminClient adminClient;
	private final DescribeClusterOptions describeOptions;

	/**
	 * Create a new {@link KafkaHealthIndicator} instance.
	 *
	 * @param adminClient the kafka admin client
	 * @param responseTimeout the describe cluster request timeout in milliseconds
	 */
	public KafkaHealthIndicator(AdminClient adminClient, long responseTimeout) {
		Assert.notNull(adminClient, "KafkaAdmin must not be null");

		this.adminClient = adminClient;
		this.describeOptions = new DescribeClusterOptions().timeoutMs((int) responseTimeout);
	}

	@Override
	protected void doHealthCheck(Builder builder) throws Exception {
		DescribeClusterResult result = this.adminClient.describeCluster(this.describeOptions);
		builder.up().withDetail("clusterId", result.clusterId().get());
	}
}

