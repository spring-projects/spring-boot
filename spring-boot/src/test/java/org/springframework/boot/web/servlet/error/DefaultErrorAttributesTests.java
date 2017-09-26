/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.web.servlet.error;

import java.util.Collections;
import java.util.Date;
import java.util.Map;

import javax.servlet.ServletException;

import org.junit.Test;

import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.ModelAndView;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DefaultErrorAttributes}.
 *
 * @author Phillip Webb
 * @author Vedran Pavic
 */
public class DefaultErrorAttributesTests {

	private DefaultErrorAttributes errorAttributes = new DefaultErrorAttributes();

	private MockHttpServletRequest request = new MockHttpServletRequest();

	private WebRequest webRequest = new ServletWebRequest(this.request);

	@Test
	public void includeTimeStamp() throws Exception {
		Map<String, Object> attributes = this.errorAttributes
				.getErrorAttributes(this.webRequest, false);
		assertThat(attributes.get("timestamp")).isInstanceOf(Date.class);
	}

	@Test
	public void specificStatusCode() throws Exception {
		this.request.setAttribute("javax.servlet.error.status_code", 404);
		Map<String, Object> attributes = this.errorAttributes
				.getErrorAttributes(this.webRequest, false);
		assertThat(attributes.get("error"))
				.isEqualTo(HttpStatus.NOT_FOUND.getReasonPhrase());
		assertThat(attributes.get("status")).isEqualTo(404);
	}

	@Test
	public void missingStatusCode() throws Exception {
		Map<String, Object> attributes = this.errorAttributes
				.getErrorAttributes(this.webRequest, false);
		assertThat(attributes.get("error")).isEqualTo("None");
		assertThat(attributes.get("status")).isEqualTo(999);
	}

	@Test
	public void mvcError() throws Exception {
		RuntimeException ex = new RuntimeException("Test");
		ModelAndView modelAndView = this.errorAttributes.resolveException(this.request,
				null, null, ex);
		this.request.setAttribute("javax.servlet.error.exception",
				new RuntimeException("Ignored"));
		Map<String, Object> attributes = this.errorAttributes
				.getErrorAttributes(this.webRequest, false);
		assertThat(this.errorAttributes.getError(this.webRequest)).isSameAs(ex);
		assertThat(modelAndView).isNull();
		assertThat(attributes.get("exception")).isNull();
		assertThat(attributes.get("message")).isEqualTo("Test");
	}

	@Test
	public void servletError() throws Exception {
		RuntimeException ex = new RuntimeException("Test");
		this.request.setAttribute("javax.servlet.error.exception", ex);
		Map<String, Object> attributes = this.errorAttributes
				.getErrorAttributes(this.webRequest, false);
		assertThat(this.errorAttributes.getError(this.webRequest)).isSameAs(ex);
		assertThat(attributes.get("exception")).isNull();
		assertThat(attributes.get("message")).isEqualTo("Test");
	}

	@Test
	public void servletMessage() throws Exception {
		this.request.setAttribute("javax.servlet.error.message", "Test");
		Map<String, Object> attributes = this.errorAttributes
				.getErrorAttributes(this.webRequest, false);
		assertThat(attributes.get("exception")).isNull();
		assertThat(attributes.get("message")).isEqualTo("Test");
	}

	@Test
	public void nullMessage() throws Exception {
		this.request.setAttribute("javax.servlet.error.exception",
				new RuntimeException());
		this.request.setAttribute("javax.servlet.error.message", "Test");
		Map<String, Object> attributes = this.errorAttributes
				.getErrorAttributes(this.webRequest, false);
		assertThat(attributes.get("exception")).isNull();
		assertThat(attributes.get("message")).isEqualTo("Test");
	}

	@Test
	public void unwrapServletException() throws Exception {
		RuntimeException ex = new RuntimeException("Test");
		ServletException wrapped = new ServletException(new ServletException(ex));
		this.request.setAttribute("javax.servlet.error.exception", wrapped);
		Map<String, Object> attributes = this.errorAttributes
				.getErrorAttributes(this.webRequest, false);
		assertThat(this.errorAttributes.getError(this.webRequest)).isSameAs(wrapped);
		assertThat(attributes.get("exception")).isNull();
		assertThat(attributes.get("message")).isEqualTo("Test");
	}

	@Test
	public void getError() throws Exception {
		Error error = new OutOfMemoryError("Test error");
		this.request.setAttribute("javax.servlet.error.exception", error);
		Map<String, Object> attributes = this.errorAttributes
				.getErrorAttributes(this.webRequest, false);
		assertThat(this.errorAttributes.getError(this.webRequest)).isSameAs(error);
		assertThat(attributes.get("exception")).isNull();
		assertThat(attributes.get("message")).isEqualTo("Test error");
	}

	@Test
	public void extractBindingResultErrors() throws Exception {
		BindingResult bindingResult = new MapBindingResult(
				Collections.singletonMap("a", "b"), "objectName");
		bindingResult.addError(new ObjectError("c", "d"));
		Exception ex = new BindException(bindingResult);
		testBindingResult(bindingResult, ex);
	}

	@Test
	public void extractMethodArgumentNotValidExceptionBindingResultErrors()
			throws Exception {
		BindingResult bindingResult = new MapBindingResult(
				Collections.singletonMap("a", "b"), "objectName");
		bindingResult.addError(new ObjectError("c", "d"));
		Exception ex = new MethodArgumentNotValidException(null, bindingResult);
		testBindingResult(bindingResult, ex);
	}

	private void testBindingResult(BindingResult bindingResult, Exception ex) {
		this.request.setAttribute("javax.servlet.error.exception", ex);
		Map<String, Object> attributes = this.errorAttributes
				.getErrorAttributes(this.webRequest, false);
		assertThat(attributes.get("message"))
				.isEqualTo("Validation failed for object='objectName'. Error count: 1");
		assertThat(attributes.get("errors")).isEqualTo(bindingResult.getAllErrors());
	}

	@Test
	public void withExceptionAttribute() throws Exception {
		DefaultErrorAttributes errorAttributes = new DefaultErrorAttributes(true);
		RuntimeException ex = new RuntimeException("Test");
		this.request.setAttribute("javax.servlet.error.exception", ex);
		Map<String, Object> attributes = errorAttributes
				.getErrorAttributes(this.webRequest, false);
		assertThat(attributes.get("exception"))
				.isEqualTo(RuntimeException.class.getName());
		assertThat(attributes.get("message")).isEqualTo("Test");
	}

	@Test
	public void trace() throws Exception {
		RuntimeException ex = new RuntimeException("Test");
		this.request.setAttribute("javax.servlet.error.exception", ex);
		Map<String, Object> attributes = this.errorAttributes
				.getErrorAttributes(this.webRequest, true);
		assertThat(attributes.get("trace").toString()).startsWith("java.lang");
	}

	@Test
	public void noTrace() throws Exception {
		RuntimeException ex = new RuntimeException("Test");
		this.request.setAttribute("javax.servlet.error.exception", ex);
		Map<String, Object> attributes = this.errorAttributes
				.getErrorAttributes(this.webRequest, false);
		assertThat(attributes.get("trace")).isNull();
	}

	@Test
	public void path() throws Exception {
		this.request.setAttribute("javax.servlet.error.request_uri", "path");
		Map<String, Object> attributes = this.errorAttributes
				.getErrorAttributes(this.webRequest, false);
		assertThat(attributes.get("path")).isEqualTo("path");
	}

}
