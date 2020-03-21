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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SharedObjectMapper}.
 *
 * @author Phillip Webb
 */
class SharedObjectMapperTests {

	@Test
	void getReturnsConfiguredObjectMapper() {
		ObjectMapper mapper = SharedObjectMapper.get();
		assertThat(mapper).isNotNull();
		assertThat(mapper.getRegisteredModuleIds()).contains(new ParameterNamesModule().getTypeId());
		assertThat(SerializationFeature.INDENT_OUTPUT
				.enabledIn(mapper.getSerializationConfig().getSerializationFeatures())).isTrue();
		assertThat(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
				.enabledIn(mapper.getDeserializationConfig().getDeserializationFeatures())).isFalse();
		assertThat(mapper.getSerializationConfig().getPropertyNamingStrategy())
				.isEqualTo(PropertyNamingStrategy.LOWER_CAMEL_CASE);
		assertThat(mapper.getDeserializationConfig().getPropertyNamingStrategy())
				.isEqualTo(PropertyNamingStrategy.LOWER_CAMEL_CASE);
	}

}
