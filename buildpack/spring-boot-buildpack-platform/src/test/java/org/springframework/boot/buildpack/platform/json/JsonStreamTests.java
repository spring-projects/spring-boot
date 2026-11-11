/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.buildpack.platform.json;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.ObjectNode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JsonStream}.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 */
class JsonStreamTests extends AbstractJsonTests {

	private final JsonStream jsonStream;

	JsonStreamTests() {
		this.jsonStream = new JsonStream(getJsonMapper());
	}

	@Test
	void getWhenReadingObjectNodeReturnsNodes() throws Exception {
		List<ObjectNode> result = new ArrayList<>();
		this.jsonStream.get(getContent("stream.json"), result::add);
		assertThat(result).hasSize(595);
		assertThat(result.get(594).get("status").asString())
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
