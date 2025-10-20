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

package org.springframework.boot.actuate.endpoint.jmx;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.boot.test.json.BasicJsonTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.spy;

/**
 * Tests for {@link JacksonJmxOperationResponseMapper}
 *
 * @author Phillip Webb
 */
class JacksonJmxOperationResponseMapperTests {

	private final JacksonJmxOperationResponseMapper mapper = new JacksonJmxOperationResponseMapper(null);

	private final BasicJsonTester json = new BasicJsonTester(getClass());

	@Test
	void createWhenObjectMapperIsNullShouldUseDefaultObjectMapper() {
		JacksonJmxOperationResponseMapper mapper = new JacksonJmxOperationResponseMapper(null);
		Object mapped = mapper.mapResponse(Collections.singleton("test"));
		assertThat(this.json.from(mapped.toString())).isEqualToJson("[test]");
	}

	@Test
	void createWhenObjectMapperIsSpecifiedShouldUseObjectMapper() {
		JsonMapper jsonMapper = spy(JsonMapper.class);
		JacksonJmxOperationResponseMapper mapper = new JacksonJmxOperationResponseMapper(jsonMapper);
		Set<String> response = Collections.singleton("test");
		mapper.mapResponse(response);
		then(jsonMapper).should().convertValue(eq(response), any(JavaType.class));
	}

	@Test
	void mapResponseTypeWhenCharSequenceShouldReturnString() {
		assertThat(this.mapper.mapResponseType(String.class)).isEqualTo(String.class);
		assertThat(this.mapper.mapResponseType(StringBuilder.class)).isEqualTo(String.class);
	}

	@Test
	void mapResponseTypeWhenArrayShouldReturnList() {
		assertThat(this.mapper.mapResponseType(String[].class)).isEqualTo(List.class);
		assertThat(this.mapper.mapResponseType(Object[].class)).isEqualTo(List.class);
	}

	@Test
	void mapResponseTypeWhenCollectionShouldReturnList() {
		assertThat(this.mapper.mapResponseType(Collection.class)).isEqualTo(List.class);
		assertThat(this.mapper.mapResponseType(Set.class)).isEqualTo(List.class);
		assertThat(this.mapper.mapResponseType(List.class)).isEqualTo(List.class);
	}

	@Test
	void mapResponseTypeWhenOtherShouldReturnMap() {
		assertThat(this.mapper.mapResponseType(ExampleBean.class)).isEqualTo(Map.class);
	}

	@Test
	void mapResponseWhenNullShouldReturnNull() {
		assertThat(this.mapper.mapResponse(null)).isNull();
	}

	@Test
	void mapResponseWhenCharSequenceShouldReturnString() {
		assertThat(this.mapper.mapResponse(new StringBuilder("test"))).isEqualTo("test");
	}

	@Test
	void mapResponseWhenArrayShouldReturnJsonArray() {
		Object mapped = this.mapper.mapResponse(new int[] { 1, 2, 3 });
		assertThat(this.json.from(mapped.toString())).isEqualToJson("[1,2,3]");
	}

	@Test
	void mapResponseWhenCollectionShouldReturnJsonArray() {
		Object mapped = this.mapper.mapResponse(Arrays.asList("a", "b", "c"));
		assertThat(this.json.from(mapped.toString())).isEqualToJson("[a,b,c]");
	}

	@Test
	void mapResponseWhenOtherShouldReturnMap() {
		ExampleBean bean = new ExampleBean();
		bean.setName("boot");
		Object mapped = this.mapper.mapResponse(bean);
		assertThat(this.json.from(mapped.toString())).isEqualToJson("{'name':'boot'}");
	}

	public static class ExampleBean {

		private @Nullable String name;

		public @Nullable String getName() {
			return this.name;
		}

		public void setName(@Nullable String name) {
			this.name = name;
		}

	}

}
