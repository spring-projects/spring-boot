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

import java.io.IOException;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import io.searchbox.action.Action;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.client.config.exception.CouldNotConnectException;
import io.searchbox.core.SearchResult;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ElasticsearchJestHealthIndicator}.
 *
 * @author Stephane Nicoll
 * @author Julian Devia Serna
 * @author Brian Clozel
 */
class ElasticsearchJestHealthIndicatorTests {

	private final JestClient jestClient = mock(JestClient.class);

	private final ElasticsearchJestHealthIndicator healthIndicator = new ElasticsearchJestHealthIndicator(
			this.jestClient);

	@SuppressWarnings("unchecked")
	@Test
	void elasticsearchIsUp() throws IOException {
		given(this.jestClient.execute(any(Action.class))).willReturn(createJestResult(200, true, "green"));
		Health health = this.healthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertHealthDetailsWithStatus(health.getDetails(), "green");
	}

	@Test
	@SuppressWarnings("unchecked")
	void elasticsearchWithYellowStatusIsUp() throws IOException {
		given(this.jestClient.execute(any(Action.class))).willReturn(createJestResult(200, true, "yellow"));
		Health health = this.healthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertHealthDetailsWithStatus(health.getDetails(), "yellow");
	}

	@SuppressWarnings("unchecked")
	@Test
	void elasticsearchIsDown() throws IOException {
		given(this.jestClient.execute(any(Action.class)))
				.willThrow(new CouldNotConnectException("http://localhost:9200", new IOException()));
		Health health = this.healthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
	}

	@SuppressWarnings("unchecked")
	@Test
	void elasticsearchIsDownWhenQueryDidNotSucceed() throws IOException {
		given(this.jestClient.execute(any(Action.class))).willReturn(createJestResult(200, false, ""));
		Health health = this.healthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
	}

	@SuppressWarnings("unchecked")
	@Test
	void elasticsearchIsDownByResponseCode() throws IOException {
		given(this.jestClient.execute(any(Action.class))).willReturn(createJestResult(500, false, ""));
		Health health = this.healthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertThat(health.getDetails()).contains(entry("statusCode", 500));
	}

	@SuppressWarnings("unchecked")
	@Test
	void elasticsearchIsOutOfServiceByStatus() throws IOException {
		given(this.jestClient.execute(any(Action.class))).willReturn(createJestResult(200, true, "red"));
		Health health = this.healthIndicator.health();
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

	private static JestResult createJestResult(int responseCode, boolean succeeded, String status) {

		SearchResult searchResult = new SearchResult(new Gson());
		String json;
		if (responseCode == 200) {
			json = String.format(
					"{\"cluster_name\":\"elasticsearch\","
							+ "\"status\":\"%s\",\"timed_out\":false,\"number_of_nodes\":1,"
							+ "\"number_of_data_nodes\":1,\"active_primary_shards\":0,"
							+ "\"active_shards\":0,\"relocating_shards\":0,\"initializing_shards\":0,"
							+ "\"unassigned_shards\":0,\"delayed_unassigned_shards\":0,"
							+ "\"number_of_pending_tasks\":0,\"number_of_in_flight_fetch\":0,"
							+ "\"task_max_waiting_in_queue_millis\":0,\"active_shards_percent_as_number\":100.0}",
					status);
		}
		else {
			json = "{\n" + "  \"error\": \"Server Error\",\n" + "  \"status\": \"" + status + "\"\n" + "}";
		}
		searchResult.setJsonString(json);
		searchResult.setJsonObject(new JsonParser().parse(json).getAsJsonObject());
		searchResult.setResponseCode(responseCode);
		searchResult.setSucceeded(succeeded);
		return searchResult;
	}

}
