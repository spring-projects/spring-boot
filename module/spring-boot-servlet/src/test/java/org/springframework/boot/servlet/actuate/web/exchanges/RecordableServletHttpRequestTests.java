/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.servlet.actuate.web.exchanges;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link RecordableServletHttpRequest}.
 *
 * @author Madhura Bhave
 */
class RecordableServletHttpRequestTests {

	private MockHttpServletRequest request;

	@BeforeEach
	void setup() {
		this.request = new MockHttpServletRequest("GET", "/script");
	}

	@Test
	void getUriWithoutQueryStringShouldReturnUri() {
		validate("http://localhost/script");
	}

	@Test
	void getUriShouldReturnUriWithQueryString() {
		this.request.setQueryString("a=b");
		validate("http://localhost/script?a=b");
	}

	@Test
	void getUriWithSpecialCharactersInQueryStringShouldEncode() {
		this.request.setQueryString("a=${b}");
		validate("http://localhost/script?a=$%7Bb%7D");
	}

	@Test
	void getUriWithSpecialCharactersEncodedShouldNotDoubleEncode() {
		this.request.setQueryString("a=$%7Bb%7D");
		validate("http://localhost/script?a=$%7Bb%7D");
	}

	private void validate(String expectedUri) {
		RecordableServletHttpRequest sourceRequest = new RecordableServletHttpRequest(this.request);
		assertThat(sourceRequest.getUri()).hasToString(expectedUri);
	}

}
