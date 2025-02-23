/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.devtools.livereload;

import java.nio.charset.StandardCharsets;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LiveReloadScriptInjectingFilter}.
 *
 * @author Vedran Pavic
 */
class LiveReloadScriptInjectingFilterTests {

	private static final String SCRIPT_ELEMENT = "<script src=\"/livereload.js?port=1234\"></script>";

	private final LiveReloadScriptInjectingFilter filter = new LiveReloadScriptInjectingFilter(1234);

	@ParameterizedTest
	@ValueSource(strings = { MediaType.TEXT_HTML_VALUE, "text/html; charset=utf-8" })
	void givenHtmlCompatibleContentTypeThenShouldInjectScriptElement(String contentType) throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();
		byte[] responseBody = "<!DOCTYPE html><html lang=\"en\"><head><title>test</title></head><body></body></html>"
			.getBytes(StandardCharsets.UTF_8);
		FilterChain filterChain = (filterRequest, filterResponse) -> {
			((HttpServletResponse) filterResponse).setStatus(HttpServletResponse.SC_OK);
			filterResponse.setContentType(contentType);
			FileCopyUtils.copy(responseBody, filterResponse.getOutputStream());
		};
		this.filter.doFilter(new MockHttpServletRequest(), response, filterChain);
		assertThat(response.getContentLength()).isEqualTo(responseBody.length + SCRIPT_ELEMENT.length());
		assertThat(response.getContentAsString()).containsOnlyOnce("<head>" + SCRIPT_ELEMENT);
	}

	@ParameterizedTest
	@ValueSource(strings = { "<html><head><title>test</title></head><body></body></html>",
			"<html>\n\t<head><title>test</title></head><body></body></html>",
			"<html><head with=\"attribute\"><title>test</title></head><body></body></html>",
			"<html><HEAD><title>test</title></head><body></body></html>",
			"<html><title>test</title><body></body></html>" })
	void givenValidHtmlThenShouldInjectScriptElement(String htmlContent) throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();
		byte[] responseBody = htmlContent.getBytes(StandardCharsets.UTF_8);
		FilterChain filterChain = (filterRequest, filterResponse) -> {
			((HttpServletResponse) filterResponse).setStatus(HttpServletResponse.SC_OK);
			filterResponse.setContentType(MediaType.TEXT_HTML_VALUE);
			FileCopyUtils.copy(responseBody, filterResponse.getOutputStream());
		};
		this.filter.doFilter(new MockHttpServletRequest(), response, filterChain);
		assertThat(response.getContentLength()).isEqualTo(responseBody.length + SCRIPT_ELEMENT.length());
		assertThat(response.getContentAsString()).containsOnlyOnce(SCRIPT_ELEMENT);
	}

	@ParameterizedTest
	@ValueSource(strings = { MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE })
	void givenIncompatibleContentTypeThenShouldNotInjectScriptElement(String contentType) throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();
		byte[] responseBody = "{}".getBytes(StandardCharsets.UTF_8);
		FilterChain filterChain = (filterRequest, filterResponse) -> {
			((HttpServletResponse) filterResponse).setStatus(HttpServletResponse.SC_OK);
			filterResponse.setContentType(contentType);
			FileCopyUtils.copy(responseBody, filterResponse.getOutputStream());
		};
		this.filter.doFilter(new MockHttpServletRequest(), response, filterChain);
		assertThat(response.getContentLength()).isEqualTo(responseBody.length);
		assertThat(response.getContentAsString()).doesNotContain(SCRIPT_ELEMENT);
	}

	@Test
	void givenNoContentTypeThenShouldNotInjectScriptElement() throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();
		byte[] responseBody = "test".getBytes(StandardCharsets.UTF_8);
		FilterChain filterChain = (filterRequest, filterResponse) -> {
			((HttpServletResponse) filterResponse).setStatus(HttpServletResponse.SC_OK);
			FileCopyUtils.copy(responseBody, filterResponse.getOutputStream());
		};
		this.filter.doFilter(new MockHttpServletRequest(), response, filterChain);
		assertThat(response.getContentLength()).isEqualTo(responseBody.length);
		assertThat(response.getContentAsString()).doesNotContain(SCRIPT_ELEMENT);
	}

	@Test
	void givenInvalidHtmlThenShouldNotInjectScriptElement() throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();
		byte[] responseBody = "<!DOCTYPE html><head></head><body></body>".getBytes(StandardCharsets.UTF_8);
		FilterChain filterChain = (filterRequest, filterResponse) -> {
			((HttpServletResponse) filterResponse).setStatus(HttpServletResponse.SC_OK);
			filterResponse.setContentType(MediaType.TEXT_HTML_VALUE);
			FileCopyUtils.copy(responseBody, filterResponse.getOutputStream());
		};
		this.filter.doFilter(new MockHttpServletRequest(), response, filterChain);
		assertThat(response.getContentLength()).isEqualTo(responseBody.length);
		assertThat(response.getContentAsString()).doesNotContain(SCRIPT_ELEMENT);
	}

}
