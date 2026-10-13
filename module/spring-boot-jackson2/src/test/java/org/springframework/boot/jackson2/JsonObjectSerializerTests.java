/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jackson2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JsonObjectSerializer}.
 *
 * @author Phillip Webb
 * @deprecated since 4.0.0 for removal in 4.3.0 in favor of Jackson 3
 */
@Deprecated(since = "4.0.0", forRemoval = true)
@SuppressWarnings("removal")
class JsonObjectSerializerTests {

	@Test
	void serializeObjectShouldWriteJson() throws Exception {
		org.springframework.boot.jackson2.NameAndAgeJsonComponent.Serializer serializer = new org.springframework.boot.jackson2.NameAndAgeJsonComponent.Serializer();
		SimpleModule module = new SimpleModule();
		module.addSerializer(org.springframework.boot.jackson2.types.NameAndAge.class, serializer);
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(module);
		String json = mapper.writeValueAsString(new org.springframework.boot.jackson2.types.NameAndAge("spring", 100));
		assertThat(json).isEqualToIgnoringWhitespace("{\"name\":\"spring\",\"age\":100}");
	}

}
