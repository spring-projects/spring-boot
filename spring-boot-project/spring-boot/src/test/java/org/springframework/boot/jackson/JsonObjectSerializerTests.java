/*
 * Copyright 2012-2017 the original author or authors.
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.Test;

import org.springframework.boot.jackson.NameAndAgeJsonComponent.Serializer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JsonObjectSerializer}.
 *
 * @author Phillip Webb
 */
public class JsonObjectSerializerTests {

	@Test
	public void serializeObjectShouldWriteJson() throws Exception {
		Serializer serializer = new NameAndAgeJsonComponent.Serializer();
		SimpleModule module = new SimpleModule();
		module.addSerializer(NameAndAge.class, serializer);
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(module);
		String json = mapper.writeValueAsString(new NameAndAge("spring", 100));
		assertThat(json).isEqualToIgnoringWhitespace("{\"name\":\"spring\",\"age\":100}");
	}

}
