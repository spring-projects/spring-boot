/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.buildpack.platform.docker.transport;

import org.junit.jupiter.api.Test;

import org.springframework.boot.buildpack.platform.json.AbstractJsonTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Message}.
 *
 * @author Scott Frederick
 */
class MessageTests extends AbstractJsonTests {

	@Test
	void readValueDeserializesJson() throws Exception {
		Message message = getJsonMapper().readValue(getContent("message.json"), Message.class);
		assertThat(message.getMessage()).isEqualTo("test message");
	}

	@Test
	void toStringHasErrorDetails() throws Exception {
		Message errors = getJsonMapper().readValue(getContent("message.json"), Message.class);
		assertThat(errors).hasToString("test message");
	}

}
