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

package org.springframework.boot.jackson;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

import org.springframework.boot.jackson.NameAndAgeJacksonComponent.Serializer;
import org.springframework.boot.jackson.types.NameAndAge;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ObjectValueSerializer}.
 *
 * @author Phillip Webb
 */
class ObjectValueSerializerTests {

	@Test
	void serializeObjectShouldWriteJson() throws Exception {
		Serializer serializer = new NameAndAgeJacksonComponent.Serializer();
		SimpleModule module = new SimpleModule();
		module.addSerializer(NameAndAge.class, serializer);
		JsonMapper mapper = JsonMapper.builder().addModule(module).build();
		String json = mapper.writeValueAsString(NameAndAge.create("spring", 100));
		assertThat(json).isEqualToIgnoringWhitespace("{\"theName\":\"spring\",\"theAge\":100}");
	}

}
