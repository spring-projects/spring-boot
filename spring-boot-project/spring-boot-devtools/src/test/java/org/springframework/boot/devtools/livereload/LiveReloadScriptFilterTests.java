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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.http.MediaType;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LiveReloadScriptFilter}.
 *
 * @author Vedran Pavic
 */
class LiveReloadScriptFilterTests {

	@ParameterizedTest
	@ValueSource(strings = { MediaType.TEXT_HTML_VALUE, "text/html; charset=utf-8" })
	void givenHtmlCompatibleContentTypeThenResponseShouldContainScript(String contentType) throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();
		response.getWriter().write("<html><head><title>test</title></head><body></body></html>");
		response.setContentType(contentType);
		LiveReloadScriptFilter filter = new LiveReloadScriptFilter(1234);
		filter.doFilter(new MockHttpServletRequest(), response, new MockFilterChain());
		assertThat(response.getContentAsString()).endsWith("<script src=\"/livereload.js?port=1234\"></script>");
	}

	@ParameterizedTest
	@ValueSource(strings = { MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE })
	void givenNonHtmlCompatibleContentTypeThenResponseShouldNotContainScript(String contentType) throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();
		response.getWriter().write("{}");
		response.setContentType(contentType);
		LiveReloadScriptFilter filter = new LiveReloadScriptFilter(1234);
		filter.doFilter(new MockHttpServletRequest(), response, new MockFilterChain());
		assertThat(response.getContentAsString()).doesNotContain("<script src=\"/livereload.js?port=1234\"></script>");
	}

	@Test
	void givenNoContentTypeThenResponseShouldNotContainScript() throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();
		response.getWriter().write("test");
		LiveReloadScriptFilter filter = new LiveReloadScriptFilter(1234);
		filter.doFilter(new MockHttpServletRequest(), response, new MockFilterChain());
		assertThat(response.getContentAsString()).doesNotContain("<script src=\"/livereload.js?port=1234\"></script>");
	}

	@Test
	void givenResponseWriterAccessNotAllowedThenResponseShouldNotContainScript() throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();
		response.setWriterAccessAllowed(false);
		response.setContentType(MediaType.TEXT_HTML_VALUE);
		LiveReloadScriptFilter filter = new LiveReloadScriptFilter(1234);
		filter.doFilter(new MockHttpServletRequest(), response, new MockFilterChain());
		assertThat(response.getContentAsString()).doesNotContain("<script src=\"/livereload.js?port=1234\"></script>");
	}

}
