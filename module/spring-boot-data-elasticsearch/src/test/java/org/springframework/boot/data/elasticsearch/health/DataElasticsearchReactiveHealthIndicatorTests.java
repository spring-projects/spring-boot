/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.data.elasticsearch.health;

import java.time.Duration;
import java.util.Map;

import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest5_client.Rest5ClientTransport;
import co.elastic.clients.transport.rest5_client.low_level.ResponseException;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.hc.core5.http.HttpHost;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;
import org.springframework.boot.testsupport.junit.EnabledOnLocale;
import org.springframework.data.elasticsearch.client.elc.ReactiveElasticsearchClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * Tests for {@link DataElasticsearchReactiveHealthIndicator}
 *
 * @author Brian Clozel
 * @author Scott Frederick
 */
class DataElasticsearchReactiveHealthIndicatorTests {

	private static final Duration TIMEOUT = Duration.ofSeconds(5);

	private MockWebServer server;

	private DataElasticsearchReactiveHealthIndicator healthIndicator;

	@BeforeEach
	void setup() throws Exception {
		this.server = new MockWebServer();
		this.server.start();
		ReactiveElasticsearchClient client = new ReactiveElasticsearchClient(new Rest5ClientTransport(
				Rest5Client.builder(HttpHost.create(this.server.getHostName() + ":" + this.server.getPort())).build(),
				new JacksonJsonpMapper()));
		this.healthIndicator = new DataElasticsearchReactiveHealthIndicator(client);
	}

	@AfterEach
	void shutdown() throws Exception {
		this.server.shutdown();
	}

	@Test
	void elasticsearchIsUp() {
		setupMockResponse("green");
		Health health = this.healthIndicator.health().block(TIMEOUT);
		assertThat(health).isNotNull();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertHealthDetailsWithStatus(health.getDetails(), "green");
	}

	@Test
	void elasticsearchWithYellowStatusIsUp() {
		setupMockResponse("yellow");
		Health health = this.healthIndicator.health().block(TIMEOUT);
		assertThat(health).isNotNull();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertHealthDetailsWithStatus(health.getDetails(), "yellow");
	}

	@Test
	@EnabledOnLocale(language = "en")
	void elasticsearchIsDown() throws Exception {
		this.server.shutdown();
		Health health = this.healthIndicator.health().block(TIMEOUT);
		assertThat(health).isNotNull();
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertThat(health.getDetails().get("error")).asString().contains("Connection refused");
	}

	@Test
	void elasticsearchIsDownByResponseCode() {
		this.server.enqueue(new MockResponse().setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.value()));
		Health health = this.healthIndicator.health().block(TIMEOUT);
		assertThat(health).isNotNull();
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertThat(health.getDetails().get("error")).asString().startsWith(ResponseException.class.getName());
	}

	@Test
	void elasticsearchIsOutOfServiceByStatus() {
		setupMockResponse("red");
		Health health = this.healthIndicator.health().block(TIMEOUT);
		assertThat(health).isNotNull();
		assertThat(health.getStatus()).isEqualTo(Status.OUT_OF_SERVICE);
		assertHealthDetailsWithStatus(health.getDetails(), "red");
	}

	private void assertHealthDetailsWithStatus(Map<String, Object> details, String status) {
		assertThat(details).contains(entry("cluster_name", "elasticsearch"), entry("status", status),
				entry("timed_out", false), entry("number_of_nodes", 1), entry("number_of_data_nodes", 1),
				entry("active_primary_shards", 0), entry("active_shards", 0), entry("relocating_shards", 0),
				entry("initializing_shards", 0), entry("unassigned_shards", 0), entry("delayed_unassigned_shards", 0),
				entry("number_of_pending_tasks", 0), entry("number_of_in_flight_fetch", 0),
				entry("task_max_waiting_in_queue_millis", 0L), entry("active_shards_percent_as_number", 100.0),
				entry("unassigned_primary_shards", 10));
	}

	private void setupMockResponse(String status) {
		MockResponse mockResponse = new MockResponse().setBody(createJsonResult(status))
			.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
			.setHeader("X-Elastic-Product", "Elasticsearch");
		this.server.enqueue(mockResponse);
	}

	private String createJsonResult(String status) {
		return String.format(
				"{\"cluster_name\":\"elasticsearch\"," + "\"status\":\"%s\",\"timed_out\":false,\"number_of_nodes\":1,"
						+ "\"number_of_data_nodes\":1,\"active_primary_shards\":0,"
						+ "\"active_shards\":0,\"relocating_shards\":0,\"initializing_shards\":0,"
						+ "\"unassigned_shards\":0,\"delayed_unassigned_shards\":0,"
						+ "\"number_of_pending_tasks\":0,\"number_of_in_flight_fetch\":0,"
						+ "\"task_max_waiting_in_queue_millis\":0,\"active_shards_percent_as_number\":100.0,"
						+ "\"unassigned_primary_shards\": 10 }",
				status);
	}

}
