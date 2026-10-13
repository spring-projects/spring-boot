/*
 * Copyright 2012-present the original author or authors.
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
