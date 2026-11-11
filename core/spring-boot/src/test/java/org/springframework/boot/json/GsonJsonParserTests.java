/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.json;

/**
 * Tests for {@link GsonJsonParser}.
 *
 * @author Dave Syer
 */
class GsonJsonParserTests extends AbstractJsonParserTests {

	@Override
	protected JsonParser getParser() {
		return new GsonJsonParser();
	}

	@Override
	void listWithRepeatedOpenArray(String input) {
		// Gson does not protect against deeply nested JSON
	}

}
