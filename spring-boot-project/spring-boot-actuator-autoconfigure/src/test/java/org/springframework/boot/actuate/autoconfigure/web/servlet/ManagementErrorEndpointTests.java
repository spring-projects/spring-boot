/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.web.servlet;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.web.ErrorProperties;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ManagementErrorEndpoint}.
 *
 * @author Scott Frederick
 */
class ManagementErrorEndpointTests {

	private final ErrorAttributes errorAttributes = new DefaultErrorAttributes();

	private final ErrorProperties errorProperties = new ErrorProperties();

	private final MockHttpServletRequest request = new MockHttpServletRequest();

	@BeforeEach
	void setUp() {
		this.request.setAttribute("javax.servlet.error.exception", new RuntimeException("test exception"));
	}

	@Test
	void errorResponseNeverDetails() {
		ManagementErrorEndpoint endpoint = new ManagementErrorEndpoint(this.errorAttributes, this.errorProperties);
		Map<String, Object> response = endpoint.invoke(new ServletWebRequest(new MockHttpServletRequest()));
		assertThat(response).containsEntry("message", "An error occurred while processing the request");
		assertThat(response).doesNotContainKey("trace");
	}

	@Test
	void errorResponseAlwaysDetails() {
		this.errorProperties.setIncludeStacktrace(ErrorProperties.IncludeStacktrace.ALWAYS);
		this.errorProperties.setIncludeDetails(ErrorProperties.IncludeDetails.ALWAYS);
		this.request.addParameter("trace", "false");
		this.request.addParameter("details", "false");
		ManagementErrorEndpoint endpoint = new ManagementErrorEndpoint(this.errorAttributes, this.errorProperties);
		Map<String, Object> response = endpoint.invoke(new ServletWebRequest(this.request));
		assertThat(response).containsEntry("message", "test exception");
		assertThat(response).hasEntrySatisfying("trace",
				(value) -> assertThat(value).asString().startsWith("java.lang.RuntimeException: test exception"));
	}

	@Test
	void errorResponseParamsAbsent() {
		this.errorProperties.setIncludeStacktrace(ErrorProperties.IncludeStacktrace.ON_TRACE_PARAM);
		this.errorProperties.setIncludeDetails(ErrorProperties.IncludeDetails.ON_DETAILS_PARAM);
		ManagementErrorEndpoint endpoint = new ManagementErrorEndpoint(this.errorAttributes, this.errorProperties);
		Map<String, Object> response = endpoint.invoke(new ServletWebRequest(this.request));
		assertThat(response).containsEntry("message", "An error occurred while processing the request");
		assertThat(response).doesNotContainKey("trace");
	}

	@Test
	void errorResponseParamsTrue() {
		this.errorProperties.setIncludeStacktrace(ErrorProperties.IncludeStacktrace.ON_TRACE_PARAM);
		this.errorProperties.setIncludeDetails(ErrorProperties.IncludeDetails.ON_DETAILS_PARAM);
		this.request.addParameter("trace", "true");
		this.request.addParameter("details", "true");
		ManagementErrorEndpoint endpoint = new ManagementErrorEndpoint(this.errorAttributes, this.errorProperties);
		Map<String, Object> response = endpoint.invoke(new ServletWebRequest(this.request));
		assertThat(response).containsEntry("message", "test exception");
		assertThat(response).hasEntrySatisfying("trace",
				(value) -> assertThat(value).asString().startsWith("java.lang.RuntimeException: test exception"));
	}

	@Test
	void errorResponseParamsFalse() {
		this.errorProperties.setIncludeStacktrace(ErrorProperties.IncludeStacktrace.ON_TRACE_PARAM);
		this.errorProperties.setIncludeDetails(ErrorProperties.IncludeDetails.ON_DETAILS_PARAM);
		this.request.addParameter("trace", "false");
		this.request.addParameter("details", "false");
		ManagementErrorEndpoint endpoint = new ManagementErrorEndpoint(this.errorAttributes, this.errorProperties);
		Map<String, Object> response = endpoint.invoke(new ServletWebRequest(this.request));
		assertThat(response).containsEntry("message", "An error occurred while processing the request");
		assertThat(response).doesNotContainKey("trace");
	}

}
