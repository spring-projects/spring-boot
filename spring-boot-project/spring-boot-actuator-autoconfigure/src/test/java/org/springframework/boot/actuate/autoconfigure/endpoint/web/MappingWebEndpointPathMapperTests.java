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

package org.springframework.boot.actuate.autoconfigure.endpoint.web;

import java.util.Collections;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MappingWebEndpointPathMapper}.
 *
 * @author Stephane Nicoll
 */
public class MappingWebEndpointPathMapperTests {

	@Test
	public void defaultConfiguration() {
		MappingWebEndpointPathMapper mapper = new MappingWebEndpointPathMapper(
				Collections.emptyMap());
		assertThat(mapper.getRootPath("test")).isEqualTo("test");
	}

	@Test
	public void userConfiguration() {
		MappingWebEndpointPathMapper mapper = new MappingWebEndpointPathMapper(
				Collections.singletonMap("test", "custom"));
		assertThat(mapper.getRootPath("test")).isEqualTo("custom");
	}

}
