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

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.DescribeClusterOptions;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.config.ConfigResource.Type;
import org.apache.kafka.common.errors.UnsupportedVersionException;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health.Builder;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.util.Assert;

/**
 * {@link HealthIndicator} for Kafka cluster.
 *
 * @author Juan Rada
 * @author Gary Russell
 * @since 2.0.0
 */
public class KafkaHealthIndicator extends AbstractHealthIndicator implements DisposableBean {

	private static final Log logger = LogFactory.getLog(KafkaHealthIndicator.class);

	static final String REPLICATION_PROPERTY = "transaction.state.log.replication.factor";

	private static final long CLOSE_TIMEOUT = 30L;

	private final AdminClient adminClient;

	private final DescribeClusterOptions describeOptions;

	/**
	 * Create a new {@link KafkaHealthIndicator} instance.
	 *
	 * @param kafkaAdmin the kafka admin
	 * @param requestTimeout the request timeout in milliseconds
	 */
	public KafkaHealthIndicator(KafkaAdmin kafkaAdmin, long requestTimeout) {
		Assert.notNull(kafkaAdmin, "KafkaAdmin must not be null");
		this.adminClient = AdminClient.create(kafkaAdmin.getConfig());
		this.describeOptions = new DescribeClusterOptions()
				.timeoutMs((int) requestTimeout);
	}

	@Override
	protected void doHealthCheck(Builder builder) throws Exception {
		DescribeClusterResult result = this.adminClient.describeCluster(this.describeOptions);
		String brokerId = result.controller().get().idString();
		int replicationFactor = getReplicationFactor(brokerId, adminClient);
		int nodes = result.nodes().get().size();
		Status status = nodes >= replicationFactor ? Status.UP : Status.DOWN;
		builder.status(status).withDetail("clusterId", result.clusterId().get())
				.withDetail("brokerId", brokerId).withDetail("nodes", nodes);
	}

	private int getReplicationFactor(String brokerId, AdminClient adminClient) throws Exception {
		try {
			ConfigResource configResource = new ConfigResource(Type.BROKER, brokerId);
			Map<ConfigResource, Config> kafkaConfig = adminClient
					.describeConfigs(Collections.singletonList(configResource)).all().get();
			Config brokerConfig = kafkaConfig.get(configResource);
			return Integer.parseInt(brokerConfig.get(REPLICATION_PROPERTY).value());
		}
		catch (ExecutionException e) {
			if (e.getCause() instanceof UnsupportedVersionException) {
				if (logger.isDebugEnabled()) {
					logger.debug("Broker does not support obtaining replication factor, assuming 1");
				}
				return 1;
			}
			throw e;
		}
	}

	@Override
	public void destroy() throws Exception {
		this.adminClient.close(CLOSE_TIMEOUT, TimeUnit.SECONDS);
	}

}
