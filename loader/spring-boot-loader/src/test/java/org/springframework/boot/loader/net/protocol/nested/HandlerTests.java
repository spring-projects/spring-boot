/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.loader.net.protocol.nested;

import java.io.File;
import java.net.URL;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.loader.net.protocol.Handlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Tests for {@link Handler}.
 *
 * @author Phillip Webb
 */
class HandlerTests {

	@TempDir
	File temp;

	@BeforeAll
	static void registerHandlers() {
		Handlers.register();
	}

	@Test
	void openConnectionReturnsNestedUrlConnection() throws Exception {
		URL url = new URL("nested:" + this.temp.getAbsolutePath() + "/!nested.jar");
		assertThat(url.openConnection()).isInstanceOf(NestedUrlConnection.class);
	}

	@Test
	void assertUrlIsNotMalformedWhenUrlIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> Handler.assertUrlIsNotMalformed(null))
			.withMessageContaining("'url' must not be null");
	}

	@Test
	void assertUrlIsNotMalformedWhenUrlIsNotNestedThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> Handler.assertUrlIsNotMalformed("file:"))
			.withMessageContaining("must use 'nested'");
	}

	@Test
	void assertUrlIsNotMalformedWhenUrlIsValidDoesNotThrowException() {
		String url = "nested:" + this.temp.getAbsolutePath() + "/!nested.jar";
		assertThatNoException().isThrownBy(() -> Handler.assertUrlIsNotMalformed(url));
	}

	@Test
	void getHostAddressIsNull() throws Exception {
		// gh-46063
		String url = "nested:" + this.temp.getAbsolutePath() + "/!nested.jar";
		assertThat(new Handler().getHostAddress(new URL(url))).isNull();
	}

}
