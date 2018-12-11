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

import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;

import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;

/**
 * {@link HealthIndicator} for Elasticsearch using a {@link JestClient}.
 *
 * @author Stephane Nicoll
 * @author Julian Devia Serna
 * @author Brian Clozel
 * @since 2.0.0
 */
public class ElasticsearchJestHealthIndicator extends AbstractHealthIndicator {

	private final JestClient jestClient;

	private final JsonParser jsonParser = JsonParserFactory.getJsonParser();

	public ElasticsearchJestHealthIndicator(JestClient jestClient) {
		super("Elasticsearch health check failed");
		this.jestClient = jestClient;
	}

	@Override
	protected void doHealthCheck(Health.Builder builder) throws Exception {
		JestResult healthResult = this.jestClient
				.execute(new io.searchbox.cluster.Health.Builder().build());
		if (healthResult.getResponseCode() != 200 || !healthResult.isSucceeded()) {
			builder.down();
			builder.withDetail("statusCode", healthResult.getResponseCode());
		}
		else {
			Map<String, Object> response = this.jsonParser
					.parseMap(healthResult.getJsonString());
			String status = (String) response.get("status");
			if (status.equals(io.searchbox.cluster.Health.Status.RED.getKey())) {
				builder.outOfService();
			}
			else {
				builder.up();
			}
			builder.withDetails(response);
		}
	}

}
