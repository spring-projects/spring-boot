/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.actuate.health;

import java.util.List;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.Requests;

/**
 * {@link HealthIndicator} for an Elasticsearch cluster.
 *
 * @author Binwei Yang
 * @author Andy Wilkinson
 * @since 1.3.0
 */
public class ElasticsearchHealthIndicator extends AbstractHealthIndicator {

	private static final String[] allIndices = { "_all" };

	private final Client client;

	private final ElasticsearchHealthIndicatorProperties properties;

	public ElasticsearchHealthIndicator(Client client,
			ElasticsearchHealthIndicatorProperties properties) {
		this.client = client;
		this.properties = properties;
	}

	@Override
	protected void doHealthCheck(Health.Builder builder) throws Exception {
		List<String> indices = this.properties.getIndices();
		ClusterHealthResponse response = this.client.admin().cluster()
				.health(Requests.clusterHealthRequest(indices.isEmpty() ? allIndices
						: indices.toArray(new String[indices.size()])))
				.actionGet(this.properties.getResponseTimeout());

		switch (response.getStatus()) {
		case GREEN:
		case YELLOW:
			builder.up();
			break;
		case RED:
		default:
			builder.down();
			break;
		}
		builder.withDetail("clusterName", response.getClusterName());
		builder.withDetail("numberOfNodes", response.getNumberOfNodes());
		builder.withDetail("numberOfDataNodes", response.getNumberOfDataNodes());
		builder.withDetail("activePrimaryShards", response.getActivePrimaryShards());
		builder.withDetail("activeShards", response.getActiveShards());
		builder.withDetail("relocatingShards", response.getRelocatingShards());
		builder.withDetail("initializingShards", response.getInitializingShards());
		builder.withDetail("unassignedShards", response.getUnassignedShards());
	}

}
