/*
 * Copyright 2012-2019 the original author or authors.
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

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;

import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.util.StreamUtils;

/**
 * {@link HealthIndicator} for an Elasticsearch cluster using a {@link RestClient}.
 *
 * @author Artsiom Yudovin
 * @author Brian Clozel
 * @author Filip Hrisafov
 * @since 2.1.1
 */
public class ElasticsearchRestHealthIndicator extends AbstractHealthIndicator {

	private static final String RED_STATUS = "red";

	private final RestClient client;

	private final JsonParser jsonParser;

	public ElasticsearchRestHealthIndicator(RestClient client) {
		super("Elasticsearch health check failed");
		this.client = client;
		this.jsonParser = JsonParserFactory.getJsonParser();
	}

	@Override
	protected void doHealthCheck(Health.Builder builder) throws Exception {
		Response response = this.client
				.performRequest(new Request("GET", "/_cluster/health/"));
		StatusLine statusLine = response.getStatusLine();
		if (statusLine.getStatusCode() != HttpStatus.SC_OK) {
			builder.down();
			builder.withDetail("statusCode", statusLine.getStatusCode());
			builder.withDetail("reasonPhrase", statusLine.getReasonPhrase());
			return;
		}
		try (InputStream inputStream = response.getEntity().getContent()) {
			doHealthCheck(builder,
					StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8));
		}
	}

	private void doHealthCheck(Health.Builder builder, String json) {
		Map<String, Object> response = this.jsonParser.parseMap(json);
		String status = (String) response.get("status");
		if (RED_STATUS.equals(status)) {
			builder.outOfService();
		}
		else {
			builder.up();
		}
		builder.withDetails(response);
	}

}
