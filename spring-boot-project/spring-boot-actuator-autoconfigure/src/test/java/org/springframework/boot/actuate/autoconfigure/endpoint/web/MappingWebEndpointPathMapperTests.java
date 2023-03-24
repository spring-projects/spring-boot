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

package org.springframework.boot.actuate.autoconfigure.endpoint.web;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.web.PathMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MappingWebEndpointPathMapper}.
 *
 * @author Stephane Nicoll
 */
class MappingWebEndpointPathMapperTests {

	@Test
	void defaultConfiguration() {
		MappingWebEndpointPathMapper mapper = new MappingWebEndpointPathMapper(Collections.emptyMap());
		assertThat(PathMapper.getRootPath(Collections.singletonList(mapper), EndpointId.of("test"))).isEqualTo("test");
	}

	@Test
	void userConfiguration() {
		MappingWebEndpointPathMapper mapper = new MappingWebEndpointPathMapper(
				Collections.singletonMap("test", "custom"));
		assertThat(PathMapper.getRootPath(Collections.singletonList(mapper), EndpointId.of("test")))
			.isEqualTo("custom");
	}

	@Test
	void mixedCaseDefaultConfiguration() {
		MappingWebEndpointPathMapper mapper = new MappingWebEndpointPathMapper(Collections.emptyMap());
		assertThat(PathMapper.getRootPath(Collections.singletonList(mapper), EndpointId.of("testEndpoint")))
			.isEqualTo("testEndpoint");
	}

	@Test
	void mixedCaseUserConfiguration() {
		MappingWebEndpointPathMapper mapper = new MappingWebEndpointPathMapper(
				Collections.singletonMap("test-endpoint", "custom"));
		assertThat(PathMapper.getRootPath(Collections.singletonList(mapper), EndpointId.of("testEndpoint")))
			.isEqualTo("custom");
	}

}
