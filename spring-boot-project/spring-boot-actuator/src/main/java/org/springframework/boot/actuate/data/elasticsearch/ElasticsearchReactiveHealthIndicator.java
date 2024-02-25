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

package org.springframework.boot.actuate.data.elasticsearch;

import co.elastic.clients.elasticsearch._types.HealthStatus;
import co.elastic.clients.elasticsearch.cluster.HealthResponse;
import reactor.core.publisher.Mono;

import org.springframework.boot.actuate.health.AbstractReactiveHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.data.elasticsearch.client.elc.ReactiveElasticsearchClient;

/**
 * {@link HealthIndicator} for an Elasticsearch cluster using a
 * {@link ReactiveElasticsearchClient}.
 *
 * @author Brian Clozel
 * @author Aleksander Lech
 * @author Scott Frederick
 * @since 2.3.2
 */
public class ElasticsearchReactiveHealthIndicator extends AbstractReactiveHealthIndicator {

	private final ReactiveElasticsearchClient client;

	/**
	 * Constructs a new ElasticsearchReactiveHealthIndicator with the specified
	 * ReactiveElasticsearchClient.
	 * @param client the ReactiveElasticsearchClient used to check the health of
	 * Elasticsearch
	 */
	public ElasticsearchReactiveHealthIndicator(ReactiveElasticsearchClient client) {
		super("Elasticsearch health check failed");
		this.client = client;
	}

	/**
	 * Performs a health check on the Elasticsearch cluster.
	 * @param builder the Health.Builder object used to build the health status
	 * @return a Mono object representing the health status of the Elasticsearch cluster
	 */
	@Override
	protected Mono<Health> doHealthCheck(Health.Builder builder) {
		return this.client.cluster().health((b) -> b).map((response) -> processResponse(builder, response));
	}

	/**
	 * Processes the response from the health check and builds a Health object.
	 * @param builder the Health.Builder object used to build the Health object
	 * @param response the HealthResponse object containing the health check response
	 * @return the processed Health object
	 */
	private Health processResponse(Health.Builder builder, HealthResponse response) {
		if (!response.timedOut()) {
			HealthStatus status = response.status();
			builder.status((HealthStatus.Red == status) ? Status.OUT_OF_SERVICE : Status.UP);
			builder.withDetail("cluster_name", response.clusterName());
			builder.withDetail("status", response.status().jsonValue());
			builder.withDetail("timed_out", response.timedOut());
			builder.withDetail("number_of_nodes", response.numberOfNodes());
			builder.withDetail("number_of_data_nodes", response.numberOfDataNodes());
			builder.withDetail("active_primary_shards", response.activePrimaryShards());
			builder.withDetail("active_shards", response.activeShards());
			builder.withDetail("relocating_shards", response.relocatingShards());
			builder.withDetail("initializing_shards", response.initializingShards());
			builder.withDetail("unassigned_shards", response.unassignedShards());
			builder.withDetail("delayed_unassigned_shards", response.delayedUnassignedShards());
			builder.withDetail("number_of_pending_tasks", response.numberOfPendingTasks());
			builder.withDetail("number_of_in_flight_fetch", response.numberOfInFlightFetch());
			builder.withDetail("task_max_waiting_in_queue_millis", response.taskMaxWaitingInQueueMillis());
			builder.withDetail("active_shards_percent_as_number",
					Double.parseDouble(response.activeShardsPercentAsNumber()));
			return builder.build();
		}
		return builder.down().build();
	}

}
