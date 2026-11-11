/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.json;

/**
 * Tests for {@link BasicJsonParser}.
 *
 * @author Dave Syer
 */
class BasicJsonParserTests extends AbstractJsonParserTests {

	@Override
	protected JsonParser getParser() {
		return new BasicJsonParser();
	}

}
