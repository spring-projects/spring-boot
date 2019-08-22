/*
 * Copyright 2012-2019 the original author or authors.
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;

import org.apache.http.StatusLine;
import org.apache.http.entity.BasicHttpEntity;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ElasticsearchRestHealthIndicator}.
 *
 * @author Artsiom Yudovin
 * @author Filip Hrisafov
 */
class ElasticsearchRestHealthIndicatorTest {

	private final RestClient restClient = mock(RestClient.class);

	private final ElasticsearchRestHealthIndicator elasticsearchRestHealthIndicator = new ElasticsearchRestHealthIndicator(
			this.restClient);

	@Test
	void elasticsearchIsUp() throws IOException {
		BasicHttpEntity httpEntity = new BasicHttpEntity();
		httpEntity.setContent(new ByteArrayInputStream(createJsonResult(200, "green").getBytes()));
		Response response = mock(Response.class);
		StatusLine statusLine = mock(StatusLine.class);
		given(statusLine.getStatusCode()).willReturn(200);
		given(response.getStatusLine()).willReturn(statusLine);
		given(response.getEntity()).willReturn(httpEntity);
		given(this.restClient.performRequest(any(Request.class))).willReturn(response);
		Health health = this.elasticsearchRestHealthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertHealthDetailsWithStatus(health.getDetails(), "green");
	}

	@Test
	void elasticsearchWithYellowStatusIsUp() throws IOException {
		BasicHttpEntity httpEntity = new BasicHttpEntity();
		httpEntity.setContent(new ByteArrayInputStream(createJsonResult(200, "yellow").getBytes()));
		Response response = mock(Response.class);
		StatusLine statusLine = mock(StatusLine.class);
		given(statusLine.getStatusCode()).willReturn(200);
		given(response.getStatusLine()).willReturn(statusLine);
		given(response.getEntity()).willReturn(httpEntity);
		given(this.restClient.performRequest(any(Request.class))).willReturn(response);
		Health health = this.elasticsearchRestHealthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertHealthDetailsWithStatus(health.getDetails(), "yellow");
	}

	@Test
	void elasticsearchIsDown() throws IOException {
		given(this.restClient.performRequest(any(Request.class))).willThrow(new IOException("Couldn't connect"));
		Health health = this.elasticsearchRestHealthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertThat(health.getDetails()).contains(entry("error", "java.io.IOException: Couldn't connect"));
	}

	@Test
	void elasticsearchIsDownByResponseCode() throws IOException {
		Response response = mock(Response.class);
		StatusLine statusLine = mock(StatusLine.class);
		given(statusLine.getStatusCode()).willReturn(500);
		given(statusLine.getReasonPhrase()).willReturn("Internal server error");
		given(response.getStatusLine()).willReturn(statusLine);
		given(this.restClient.performRequest(any(Request.class))).willReturn(response);
		Health health = this.elasticsearchRestHealthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertThat(health.getDetails()).contains(entry("statusCode", 500),
				entry("reasonPhrase", "Internal server error"));
	}

	@Test
	void elasticsearchIsOutOfServiceByStatus() throws IOException {
		BasicHttpEntity httpEntity = new BasicHttpEntity();
		httpEntity.setContent(new ByteArrayInputStream(createJsonResult(200, "red").getBytes()));
		Response response = mock(Response.class);
		StatusLine statusLine = mock(StatusLine.class);
		given(statusLine.getStatusCode()).willReturn(200);
		given(response.getStatusLine()).willReturn(statusLine);
		given(response.getEntity()).willReturn(httpEntity);
		given(this.restClient.performRequest(any(Request.class))).willReturn(response);
		Health health = this.elasticsearchRestHealthIndicator.health();
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
