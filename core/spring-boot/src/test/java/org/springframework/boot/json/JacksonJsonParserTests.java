/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.json;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.spy;

/**
 * Tests for {@link JacksonJsonParser}.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 */
class JacksonJsonParserTests extends AbstractJsonParserTests {

	@Override
	protected JsonParser getParser() {
		return new JacksonJsonParser();
	}

	@Test
	@SuppressWarnings("unchecked")
	void instanceWithSpecificObjectMapper() {
		JsonMapper jsonMapper = spy(new JsonMapper());
		new JacksonJsonParser(jsonMapper).parseMap("{}");
		then(jsonMapper).should().readValue(eq("{}"), any(TypeReference.class));
	}

	@Override
	@Disabled("Jackson's array handling is no longer stack bound so protection has been removed.")
	// https://github.com/FasterXML/jackson-databind/commit/8238ab41d0350fb915797c89d46777b4496b74fd
	void listWithRepeatedOpenArray(String input) {

	}

}
