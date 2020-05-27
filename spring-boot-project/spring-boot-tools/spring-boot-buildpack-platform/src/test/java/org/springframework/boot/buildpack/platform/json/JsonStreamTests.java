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

package org.springframework.boot.buildpack.platform.json;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JsonStream}.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 */
class JsonStreamTests extends AbstractJsonTests {

	private JsonStream jsonStream;

	JsonStreamTests() {
		this.jsonStream = new JsonStream(getObjectMapper());
	}

	@Test
	void getWhenReadingObjectNodeReturnsNodes() throws Exception {
		List<ObjectNode> result = new ArrayList<>();
		this.jsonStream.get(getContent("stream.json"), result::add);
		assertThat(result).hasSize(595);
		assertThat(result.get(594).toString())
				.contains("Status: Downloaded newer image for paketo-buildpacks/cnb:base");
	}

	@Test
	void getWhenReadTypesReturnsTypes() throws Exception {
		List<TestEvent> result = new ArrayList<>();
		this.jsonStream.get(getContent("stream.json"), TestEvent.class, result::add);
		assertThat(result).hasSize(595);
		assertThat(result.get(1).getId()).isEqualTo("5667fdb72017");
		assertThat(result.get(594).getStatus())
				.isEqualTo("Status: Downloaded newer image for paketo-buildpacks/cnb:base");
	}

	/**
	 * Event for type deserialization tests.
	 */
	static class TestEvent {

		private final String id;

		private final String status;

		@JsonCreator
		TestEvent(String id, String status) {
			this.id = id;
			this.status = status;
		}

		String getId() {
			return this.id;
		}

		String getStatus() {
			return this.status;
		}

	}

}
