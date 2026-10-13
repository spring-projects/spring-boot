/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.buildpack.platform.docker.transport;

import java.util.Iterator;

import org.junit.jupiter.api.Test;

import org.springframework.boot.buildpack.platform.docker.transport.Errors.Error;
import org.springframework.boot.buildpack.platform.json.AbstractJsonTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Errors}.
 *
 * @author Phillip Webb
 */
class ErrorsTests extends AbstractJsonTests {

	@Test
	void readValueDeserializesJson() throws Exception {
		Errors errors = getJsonMapper().readValue(getContent("errors.json"), Errors.class);
		Iterator<Error> iterator = errors.iterator();
		Error error1 = iterator.next();
		Error error2 = iterator.next();
		assertThat(iterator.hasNext()).isFalse();
		assertThat(error1.getCode()).isEqualTo("TEST1");
		assertThat(error1.getMessage()).isEqualTo("Test One");
		assertThat(error2.getCode()).isEqualTo("TEST2");
		assertThat(error2.getMessage()).isEqualTo("Test Two");
	}

	@Test
	void toStringHasErrorDetails() throws Exception {
		Errors errors = getJsonMapper().readValue(getContent("errors.json"), Errors.class);
		assertThat(errors).hasToString("[TEST1: Test One, TEST2: Test Two]");
	}

}
