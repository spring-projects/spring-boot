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

import org.elasticsearch.action.main.MainResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;

import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

/**
 * {@link HealthIndicator} for an Elasticsearch cluster by REST.
 *
 * @author Artsiom Yudovin
 * @since 2.1.0
 */
public class ElasticsearchRestHealthIndicator extends AbstractHealthIndicator {

	private final RestHighLevelClient client;

	public ElasticsearchRestHealthIndicator(RestHighLevelClient client) {
		super("Elasticsearch health check failed");
		this.client = client;
	}

	@Override
	protected void doHealthCheck(Health.Builder builder) throws Exception {
		MainResponse info = this.client.info(RequestOptions.DEFAULT);

		if (info.isAvailable()) {
			builder.up();
		}
		else {
			builder.down();
		}

		builder.withDetail("clusterName", info.getClusterName());
		builder.withDetail("nodeName", info.getNodeName());
		builder.withDetail("clusterUuid", info.getClusterUuid());
		builder.withDetail("version", info.getVersion());
	}

}
