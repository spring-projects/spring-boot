/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.actuate.health;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SimpleHttpCodeStatusMapper}.
 *
 * @author Phillip Webb
 */
class SimpleHttpCodeStatusMapperTests {

	@Test
	void createWhenMappingsAreNullUsesDefaultMappings() {
		SimpleHttpCodeStatusMapper mapper = new SimpleHttpCodeStatusMapper(null);
		assertThat(mapper.getStatusCode(Status.UNKNOWN)).isEqualTo(WebEndpointResponse.STATUS_OK);
		assertThat(mapper.getStatusCode(Status.UP)).isEqualTo(WebEndpointResponse.STATUS_OK);
		assertThat(mapper.getStatusCode(Status.DOWN)).isEqualTo(WebEndpointResponse.STATUS_SERVICE_UNAVAILABLE);
		assertThat(mapper.getStatusCode(Status.OUT_OF_SERVICE))
			.isEqualTo(WebEndpointResponse.STATUS_SERVICE_UNAVAILABLE);
	}

	@Test
	void getStatusCodeReturnsMappedStatus() {
		Map<String, Integer> map = new LinkedHashMap<>();
		map.put("up", 123);
		map.put("down", 456);
		SimpleHttpCodeStatusMapper mapper = new SimpleHttpCodeStatusMapper(map);
		assertThat(mapper.getStatusCode(Status.UP)).isEqualTo(123);
		assertThat(mapper.getStatusCode(Status.DOWN)).isEqualTo(456);
		assertThat(mapper.getStatusCode(Status.OUT_OF_SERVICE)).isEqualTo(200);
	}

	@Test
	void getStatusCodeWhenMappingsAreNotUniformReturnsMappedStatus() {
		Map<String, Integer> map = new LinkedHashMap<>();
		map.put("out-of-service", 123);
		SimpleHttpCodeStatusMapper mapper = new SimpleHttpCodeStatusMapper(map);
		assertThat(mapper.getStatusCode(Status.OUT_OF_SERVICE)).isEqualTo(123);
	}

}
