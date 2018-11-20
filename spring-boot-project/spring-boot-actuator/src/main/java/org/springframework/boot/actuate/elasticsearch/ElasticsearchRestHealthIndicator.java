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

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.apache.http.HttpStatus;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;

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

	private final RestClient client;

	private final JsonParser jsonParser = new JsonParser();

	public ElasticsearchRestHealthIndicator(RestClient client) {
		super("Elasticsearch health check failed");
		this.client = client;
	}

	@Override
	protected void doHealthCheck(Health.Builder builder) throws Exception {
		Response response = this.client
				.performRequest(new Request("GET", "/_cluster/health/"));

		if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
			builder.down();
		}
		else {
			try (InputStreamReader reader = new InputStreamReader(
					response.getEntity().getContent(), StandardCharsets.UTF_8)) {
				JsonElement root = this.jsonParser.parse(reader);
				JsonElement status = root.getAsJsonObject().get("status");
				if (status.getAsString()
						.equals(io.searchbox.cluster.Health.Status.RED.getKey())) {
					builder.outOfService();
				}
				else {
					builder.up();
				}
			}
		}
	}

}
