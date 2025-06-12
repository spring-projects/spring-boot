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

package org.springframework.boot.actuate.health;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;

import org.springframework.boot.health.contributor.Health;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link IndicatedHealthDescriptor}.
 *
 * @author Phillip Webb
 */
class IndicatedHealthDescriptorTests {

	@Test
	void serializeWithJacksonReturnsValidJson() throws Exception {
		IndicatedHealthDescriptor descriptor = new IndicatedHealthDescriptor(
				Health.outOfService().withDetail("spring", "boot").build());
		ObjectMapper mapper = JsonMapper.builder().enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY).build();
		String json = mapper.writeValueAsString(descriptor);
		assertThat(json).isEqualTo("""
				{"details":{"spring":"boot"},"status":"OUT_OF_SERVICE"}""");
	}

	@Test
	void serializeWithJacksonWhenEmptyDetailsReturnsValidJson() throws Exception {
		IndicatedHealthDescriptor descriptor = new IndicatedHealthDescriptor(Health.outOfService().build());
		ObjectMapper mapper = JsonMapper.builder().enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY).build();
		String json = mapper.writeValueAsString(descriptor);
		assertThat(json).isEqualTo("""
				{"status":"OUT_OF_SERVICE"}""");
	}

}
