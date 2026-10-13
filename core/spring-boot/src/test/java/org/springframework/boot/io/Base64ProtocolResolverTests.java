/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.io;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.Test;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link Base64ProtocolResolver}.
 *
 * @author Scott Frederick
 */
class Base64ProtocolResolverTests {

	@Test
	void base64LocationResolves() throws IOException {
		String location = Base64.getEncoder().encodeToString("test value".getBytes());
		Resource resource = new Base64ProtocolResolver().resolve("base64:" + location, new DefaultResourceLoader());
		assertThat(resource).isNotNull();
		assertThat(resource.getContentAsString(StandardCharsets.UTF_8)).isEqualTo("test value");
	}

	@Test
	void base64LocationWithInvalidBase64ThrowsException() {
		assertThatIllegalArgumentException()
			.isThrownBy(
					() -> new Base64ProtocolResolver().resolve("base64:not valid base64", new DefaultResourceLoader()))
			.withMessageContaining("Illegal base64");
	}

	@Test
	void locationWithoutPrefixDoesNotResolve() {
		Resource resource = new Base64ProtocolResolver().resolve("file:notbase64.txt", new DefaultResourceLoader());
		assertThat(resource).isNull();
	}

}
