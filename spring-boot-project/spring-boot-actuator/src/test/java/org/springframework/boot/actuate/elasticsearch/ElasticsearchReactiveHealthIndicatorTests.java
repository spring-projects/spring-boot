/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.actuate.elasticsearch;

import java.util.Map;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.reactive.DefaultReactiveElasticsearchClient;
import org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * Tests for {@link ElasticsearchReactiveHealthIndicator}
 *
 * @author Brian Clozel
 */
class ElasticsearchReactiveHealthIndicatorTests {

	private MockWebServer server;

	private WebClient.Builder builder;

	private ElasticsearchReactiveHealthIndicator healthIndicator;

	@BeforeEach
	void setup() throws Exception {
		this.server = new MockWebServer();
		this.server.start();
		this.builder = WebClient.builder().baseUrl(this.server.url("/").toString());
		ReactiveElasticsearchClient client = DefaultReactiveElasticsearchClient
				.create(ClientConfiguration.create(this.server.getHostName() + ":" + this.server.getPort()));
		this.healthIndicator = new ElasticsearchReactiveHealthIndicator(client);
	}

	@AfterEach
	void shutdown() throws Exception {
		this.server.shutdown();
	}

	@Test
	void elasticsearchIsUp() {
		setupMockResponse(200, "green");
		Health health = this.healthIndicator.health().block();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertHealthDetailsWithStatus(health.getDetails(), "green");
	}

	@Test
	void elasticsearchWithYellowStatusIsUp() {
		setupMockResponse(200, "yellow");
		Health health = this.healthIndicator.health().block();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertHealthDetailsWithStatus(health.getDetails(), "yellow");
	}

	@Test
	void elasticsearchIsDown() throws Exception {
		this.server.shutdown();
		Health health = this.healthIndicator.health().block();
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertThat(health.getDetails().get("error")).asString()
				.contains("org.springframework.data.elasticsearch.client.NoReachableHostException");
	}

	@Test
	void elasticsearchIsDownByResponseCode() {
		// first enqueue an OK response since the HostChecker first sends a HEAD request
		// to "/"
		this.server.enqueue(new MockResponse().setResponseCode(HttpStatus.OK.value()));
		this.server.enqueue(new MockResponse().setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.value()));
		Health health = this.healthIndicator.health().block();
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertThat(health.getDetails().get("statusCode")).asString().isEqualTo("500");
		assertThat(health.getDetails().get("reasonPhrase")).asString().isEqualTo("Internal Server Error");
	}

	@Test
	void elasticsearchIsOutOfServiceByStatus() {
		setupMockResponse(200, "red");
		Health health = this.healthIndicator.health().block();
		assertThat(health.getStatus()).isEqualTo(Status.OUT_OF_SERVICE);
		assertHealthDetailsWithStatus(health.getDetails(), "red");
	}

	private void assertHealthDetailsWithStatus(Map<String, Object> details, String status) {
		assertThat(details).contains(entry("cluster_name", "elasticsearch"), entry("status", status),
				entry("timed_out", false), entry("number_of_nodes", 1), entry("number_of_data_nodes", 1),
				entry("active_primary_shards", 0), entry("active_shards", 0), entry("relocating_shards", 0),
				entry("initializing_shards", 0), entry("unassigned_shards", 0), entry("delayed_unassigned_shards", 0),
				entry("number_of_pending_tasks", 0), entry("number_of_in_flight_fetch", 0),
				entry("task_max_waiting_in_queue_millis", 0), entry("active_shards_percent_as_number", 100.0));
	}

	private void setupMockResponse(int responseCode, String status) {
		// first enqueue an OK response since the HostChecker first sends a HEAD request
		// to "/"
		this.server.enqueue(new MockResponse());
		MockResponse mockResponse = new MockResponse().setResponseCode(HttpStatus.valueOf(responseCode).value())
				.setBody(createJsonResult(responseCode, status))
				.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
		this.server.enqueue(mockResponse);
	}

	private String createJsonResult(int responseCode, String status) {
		if (responseCode == 200) {
			return String.format(
					"{\"cluster_name\":\"elasticsearch\","
							+ "\"status\":\"%s\",\"timed_out\":false,\"number_of_nodes\":1,"
							+ "\"number_of_data_nodes\":1,\"active_primary_shards\":0,"
							+ "\"active_shards\":0,\"relocating_shards\":0,\"initializing_shards\":0,"
							+ "\"unassigned_shards\":0,\"delayed_unassigned_shards\":0,"
							+ "\"number_of_pending_tasks\":0,\"number_of_in_flight_fetch\":0,"
							+ "\"task_max_waiting_in_queue_millis\":0,\"active_shards_percent_as_number\":100.0}",
					status);
		}
		return "{\n  \"error\": \"Server Error\",\n  \"status\": " + responseCode + "\n}";
	}

}
