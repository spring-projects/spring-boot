/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.autoconfigure.web;

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
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.ModelAndView;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link DefaultErrorAttributes}.
 *
 * @author Phillip Webb
 */
public class DefaultErrorAttributesTests {

	private DefaultErrorAttributes errorAttributes = new DefaultErrorAttributes();

	private MockHttpServletRequest request = new MockHttpServletRequest();

	private RequestAttributes requestAttributes = new ServletRequestAttributes(
			this.request);

	@Test
	public void includeTimeStamp() throws Exception {
		Map<String, Object> attributes = this.errorAttributes.getErrorAttributes(
				this.requestAttributes, false);
		assertThat(attributes.get("timestamp"), instanceOf(Date.class));
	}

	@Test
	public void specificStatusCode() throws Exception {
		this.request.setAttribute("javax.servlet.error.status_code", 404);
		Map<String, Object> attributes = this.errorAttributes.getErrorAttributes(
				this.requestAttributes, false);
		assertThat(attributes.get("error"),
				equalTo((Object) HttpStatus.NOT_FOUND.getReasonPhrase()));
		assertThat(attributes.get("status"), equalTo((Object) 404));
	}

	@Test
	public void missingStatusCode() throws Exception {
		Map<String, Object> attributes = this.errorAttributes.getErrorAttributes(
				this.requestAttributes, false);
		assertThat(attributes.get("error"), equalTo((Object) "None"));
		assertThat(attributes.get("status"), equalTo((Object) 999));
	}

	@Test
	public void mvcError() throws Exception {
		RuntimeException ex = new RuntimeException("Test");
		ModelAndView modelAndView = this.errorAttributes.resolveException(this.request,
				null, null, ex);
		this.request.setAttribute("javax.servlet.error.exception", new RuntimeException(
				"Ignored"));
		Map<String, Object> attributes = this.errorAttributes.getErrorAttributes(
				this.requestAttributes, false);
		assertThat(this.errorAttributes.getError(this.requestAttributes),
				sameInstance((Object) ex));
		assertThat(modelAndView, nullValue());
		assertThat(attributes.get("exception"),
				equalTo((Object) RuntimeException.class.getName()));
		assertThat(attributes.get("message"), equalTo((Object) "Test"));
	}

	@Test
	public void servletError() throws Exception {
		RuntimeException ex = new RuntimeException("Test");
		this.request.setAttribute("javax.servlet.error.exception", ex);
		Map<String, Object> attributes = this.errorAttributes.getErrorAttributes(
				this.requestAttributes, false);
		assertThat(this.errorAttributes.getError(this.requestAttributes),
				sameInstance((Object) ex));
		assertThat(attributes.get("exception"),
				equalTo((Object) RuntimeException.class.getName()));
		assertThat(attributes.get("message"), equalTo((Object) "Test"));
	}

	@Test
	public void servletMessage() throws Exception {
		this.request.setAttribute("javax.servlet.error.message", "Test");
		Map<String, Object> attributes = this.errorAttributes.getErrorAttributes(
				this.requestAttributes, false);
		assertThat(attributes.get("exception"), nullValue());
		assertThat(attributes.get("message"), equalTo((Object) "Test"));
	}

	@Test
	public void nullMessage() throws Exception {
		this.request.setAttribute("javax.servlet.error.exception", new RuntimeException());
		this.request.setAttribute("javax.servlet.error.message", "Test");
		Map<String, Object> attributes = this.errorAttributes.getErrorAttributes(
				this.requestAttributes, false);
		assertThat(attributes.get("exception"),
				equalTo((Object) RuntimeException.class.getName()));
		assertThat(attributes.get("message"), equalTo((Object) "Test"));
	}

	@Test
	public void unwrapServletException() throws Exception {
		RuntimeException ex = new RuntimeException("Test");
		ServletException wrapped = new ServletException(new ServletException(ex));
		this.request.setAttribute("javax.servlet.error.exception", wrapped);
		Map<String, Object> attributes = this.errorAttributes.getErrorAttributes(
				this.requestAttributes, false);
		assertThat(this.errorAttributes.getError(this.requestAttributes),
				sameInstance((Object) wrapped));
		assertThat(attributes.get("exception"),
				equalTo((Object) RuntimeException.class.getName()));
		assertThat(attributes.get("message"), equalTo((Object) "Test"));
	}

	@Test
	public void extractBindingResultErrors() throws Exception {
		BindingResult bindingResult = new MapBindingResult(Collections.singletonMap("a",
				"b"), "objectName");
		bindingResult.addError(new ObjectError("c", "d"));
		BindException ex = new BindException(bindingResult);
		this.request.setAttribute("javax.servlet.error.exception", ex);
		Map<String, Object> attributes = this.errorAttributes.getErrorAttributes(
				this.requestAttributes, false);
		assertThat(attributes.get("message"), equalTo((Object) ("Validation failed for "
				+ "object='objectName'. Error count: 1")));
		assertThat(attributes.get("errors"),
				equalTo((Object) bindingResult.getAllErrors()));
	}

	@Test
	public void trace() throws Exception {
		RuntimeException ex = new RuntimeException("Test");
		this.request.setAttribute("javax.servlet.error.exception", ex);
		Map<String, Object> attributes = this.errorAttributes.getErrorAttributes(
				this.requestAttributes, true);
		assertThat(attributes.get("trace").toString(), startsWith("java.lang"));
	}

	@Test
	public void noTrace() throws Exception {
		RuntimeException ex = new RuntimeException("Test");
		this.request.setAttribute("javax.servlet.error.exception", ex);
		Map<String, Object> attributes = this.errorAttributes.getErrorAttributes(
				this.requestAttributes, false);
		assertThat(attributes.get("trace"), nullValue());
	}

	@Test
	public void path() throws Exception {
		this.request.setAttribute("javax.servlet.error.request_uri", "path");
		Map<String, Object> attributes = this.errorAttributes.getErrorAttributes(
				this.requestAttributes, false);
		assertThat(attributes.get("path"), equalTo((Object) "path"));

	}
}
