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
