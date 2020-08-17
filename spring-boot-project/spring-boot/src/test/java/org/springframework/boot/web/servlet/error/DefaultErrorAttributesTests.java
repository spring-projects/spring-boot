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

package org.springframework.boot.web.servlet.error;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import javax.servlet.ServletException;

import org.junit.jupiter.api.Test;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.error.ErrorAttributeOptions.Include;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.util.ReflectionUtils;
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
 * @author Scott Frederick
 */
class DefaultErrorAttributesTests {

	private final DefaultErrorAttributes errorAttributes = new DefaultErrorAttributes();

	private final MockHttpServletRequest request = new MockHttpServletRequest();

	private final WebRequest webRequest = new ServletWebRequest(this.request);

	@Test
	void includeTimeStamp() {
		Map<String, Object> attributes = this.errorAttributes.getErrorAttributes(this.webRequest,
				ErrorAttributeOptions.defaults());
		assertThat(attributes.get("timestamp")).isInstanceOf(Date.class);
	}

	@Test
	void specificStatusCode() {
		this.request.setAttribute("javax.servlet.error.status_code", 404);
		Map<String, Object> attributes = this.errorAttributes.getErrorAttributes(this.webRequest,
				ErrorAttributeOptions.defaults());
		assertThat(attributes.get("error")).isEqualTo(HttpStatus.NOT_FOUND.getReasonPhrase());
		assertThat(attributes.get("status")).isEqualTo(404);
	}

	@Test
	void missingStatusCode() {
		Map<String, Object> attributes = this.errorAttributes.getErrorAttributes(this.webRequest,
				ErrorAttributeOptions.defaults());
		assertThat(attributes.get("error")).isEqualTo("None");
		assertThat(attributes.get("status")).isEqualTo(999);
	}

	@Test
	void mvcError() {
		RuntimeException ex = new RuntimeException("Test");
		ModelAndView modelAndView = this.errorAttributes.resolveException(this.request, null, null, ex);
		this.request.setAttribute("javax.servlet.error.exception", new RuntimeException("Ignored"));
		Map<String, Object> attributes = this.errorAttributes.getErrorAttributes(this.webRequest,
				ErrorAttributeOptions.of(Include.MESSAGE));
		assertThat(this.errorAttributes.getError(this.webRequest)).isSameAs(ex);
		assertThat(modelAndView).isNull();
		assertThat(attributes.containsKey("exception")).isFalse();
		assertThat(attributes.get("message")).isEqualTo("Test");
	}

	@Test
	void servletErrorWithMessage() {
		RuntimeException ex = new RuntimeException("Test");
		this.request.setAttribute("javax.servlet.error.exception", ex);
		Map<String, Object> attributes = this.errorAttributes.getErrorAttributes(this.webRequest,
				ErrorAttributeOptions.of(Include.MESSAGE));
		assertThat(this.errorAttributes.getError(this.webRequest)).isSameAs(ex);
		assertThat(attributes.containsKey("exception")).isFalse();
		assertThat(attributes.get("message")).isEqualTo("Test");
	}

	@Test
	void servletErrorWithoutMessage() {
		RuntimeException ex = new RuntimeException("Test");
		this.request.setAttribute("javax.servlet.error.exception", ex);
		Map<String, Object> attributes = this.errorAttributes.getErrorAttributes(this.webRequest,
				ErrorAttributeOptions.defaults());
		assertThat(this.errorAttributes.getError(this.webRequest)).isSameAs(ex);
		assertThat(attributes.containsKey("exception")).isFalse();
		assertThat(attributes.get("message").toString()).contains("");
	}

	@Test
	void servletMessageWithMessage() {
		this.request.setAttribute("javax.servlet.error.message", "Test");
		Map<String, Object> attributes = this.errorAttributes.getErrorAttributes(this.webRequest,
				ErrorAttributeOptions.of(Include.MESSAGE));
		assertThat(attributes.containsKey("exception")).isFalse();
		assertThat(attributes.get("message")).isEqualTo("Test");
	}

	@Test
	void servletMessageWithoutMessage() {
		this.request.setAttribute("javax.servlet.error.message", "Test");
		Map<String, Object> attributes = this.errorAttributes.getErrorAttributes(this.webRequest,
				ErrorAttributeOptions.defaults());
		assertThat(attributes.containsKey("exception")).isFalse();
		assertThat(attributes.get("message")).asString().contains("");
	}

	@Test
	void nullExceptionMessage() {
		this.request.setAttribute("javax.servlet.error.exception", new RuntimeException());
		this.request.setAttribute("javax.servlet.error.message", "Test");
		Map<String, Object> attributes = this.errorAttributes.getErrorAttributes(this.webRequest,
				ErrorAttributeOptions.of(Include.MESSAGE));
		assertThat(attributes.containsKey("exception")).isFalse();
		assertThat(attributes.get("message")).isEqualTo("Test");
	}

	@Test
	void nullExceptionMessageAndServletMessage() {
		this.request.setAttribute("javax.servlet.error.exception", new RuntimeException());
		Map<String, Object> attributes = this.errorAttributes.getErrorAttributes(this.webRequest,
				ErrorAttributeOptions.of(Include.MESSAGE));
		assertThat(attributes.containsKey("exception")).isFalse();
		assertThat(attributes.get("message")).isEqualTo("No message available");
	}

	@Test
	void unwrapServletException() {
		RuntimeException ex = new RuntimeException("Test");
		ServletException wrapped = new ServletException(new ServletException(ex));
		this.request.setAttribute("javax.servlet.error.exception", wrapped);
		Map<String, Object> attributes = this.errorAttributes.getErrorAttributes(this.webRequest,
				ErrorAttributeOptions.of(Include.MESSAGE));
		assertThat(this.errorAttributes.getError(this.webRequest)).isSameAs(wrapped);
		assertThat(attributes.containsKey("exception")).isFalse();
		assertThat(attributes.get("message")).isEqualTo("Test");
	}

	@Test
	void getError() {
		Error error = new OutOfMemoryError("Test error");
		this.request.setAttribute("javax.servlet.error.exception", error);
		Map<String, Object> attributes = this.errorAttributes.getErrorAttributes(this.webRequest,
				ErrorAttributeOptions.of(Include.MESSAGE));
		assertThat(this.errorAttributes.getError(this.webRequest)).isSameAs(error);
		assertThat(attributes.containsKey("exception")).isFalse();
		assertThat(attributes.get("message")).isEqualTo("Test error");
	}

	@Test
	void withBindingErrors() {
		BindingResult bindingResult = new MapBindingResult(Collections.singletonMap("a", "b"), "objectName");
		bindingResult.addError(new ObjectError("c", "d"));
		Exception ex = new BindException(bindingResult);
		testBindingResult(bindingResult, ex, ErrorAttributeOptions.of(Include.MESSAGE, Include.BINDING_ERRORS));
	}

	@Test
	void withoutBindingErrors() {
		BindingResult bindingResult = new MapBindingResult(Collections.singletonMap("a", "b"), "objectName");
		bindingResult.addError(new ObjectError("c", "d"));
		Exception ex = new BindException(bindingResult);
		testBindingResult(bindingResult, ex, ErrorAttributeOptions.defaults());
	}

	@Test
	void withMethodArgumentNotValidExceptionBindingErrors() {
		Method method = ReflectionUtils.findMethod(String.class, "substring", int.class);
		MethodParameter parameter = new MethodParameter(method, 0);
		BindingResult bindingResult = new MapBindingResult(Collections.singletonMap("a", "b"), "objectName");
		bindingResult.addError(new ObjectError("c", "d"));
		Exception ex = new MethodArgumentNotValidException(parameter, bindingResult);
		testBindingResult(bindingResult, ex, ErrorAttributeOptions.of(Include.MESSAGE, Include.BINDING_ERRORS));
	}

	private void testBindingResult(BindingResult bindingResult, Exception ex, ErrorAttributeOptions options) {
		this.request.setAttribute("javax.servlet.error.exception", ex);
		Map<String, Object> attributes = this.errorAttributes.getErrorAttributes(this.webRequest, options);
		if (options.isIncluded(Include.MESSAGE)) {
			assertThat(attributes.get("message"))
					.isEqualTo("Validation failed for object='objectName'. Error count: 1");
		}
		else {
			assertThat(attributes.get("message")).isEqualTo("");
		}
		if (options.isIncluded(Include.BINDING_ERRORS)) {
			assertThat(attributes.get("errors")).isEqualTo(bindingResult.getAllErrors());
		}
		else {
			assertThat(attributes.containsKey("errors")).isFalse();
		}
	}

	@Test
	void withExceptionAttribute() {
		DefaultErrorAttributes errorAttributes = new DefaultErrorAttributes();
		RuntimeException ex = new RuntimeException("Test");
		this.request.setAttribute("javax.servlet.error.exception", ex);
		Map<String, Object> attributes = errorAttributes.getErrorAttributes(this.webRequest,
				ErrorAttributeOptions.of(Include.EXCEPTION, Include.MESSAGE));
		assertThat(attributes.get("exception")).isEqualTo(RuntimeException.class.getName());
		assertThat(attributes.get("message")).isEqualTo("Test");
	}

	@Test
	@SuppressWarnings("deprecation")
	void excludeExceptionAttributeWithDeprecatedConstructor() {
		DefaultErrorAttributes errorAttributes = new DefaultErrorAttributes(false);
		RuntimeException ex = new RuntimeException("Test");
		this.request.setAttribute("javax.servlet.error.exception", ex);
		Map<String, Object> attributes = errorAttributes.getErrorAttributes(this.webRequest,
				ErrorAttributeOptions.of());
		assertThat(attributes.get("exception")).isNull();
	}

	@Test
	void withStackTraceAttribute() {
		RuntimeException ex = new RuntimeException("Test");
		this.request.setAttribute("javax.servlet.error.exception", ex);
		Map<String, Object> attributes = this.errorAttributes.getErrorAttributes(this.webRequest,
				ErrorAttributeOptions.of(Include.STACK_TRACE));
		assertThat(attributes.get("trace").toString()).startsWith("java.lang");
	}

	@Test
	void withoutStackTraceAttribute() {
		RuntimeException ex = new RuntimeException("Test");
		this.request.setAttribute("javax.servlet.error.exception", ex);
		Map<String, Object> attributes = this.errorAttributes.getErrorAttributes(this.webRequest,
				ErrorAttributeOptions.defaults());
		assertThat(attributes.containsKey("trace")).isFalse();
	}

	@Test
	void path() {
		this.request.setAttribute("javax.servlet.error.request_uri", "path");
		Map<String, Object> attributes = this.errorAttributes.getErrorAttributes(this.webRequest,
				ErrorAttributeOptions.defaults());
		assertThat(attributes.get("path")).isEqualTo("path");
	}

	@Test
	void whenGetMessageIsOverridenThenMessageAttributeContainsValueReturnedFromIt() {
		Map<String, Object> attributes = new DefaultErrorAttributes() {

			@Override
			protected String getMessage(WebRequest webRequest, Throwable error) {
				return "custom message";
			}

		}.getErrorAttributes(this.webRequest, ErrorAttributeOptions.of(Include.MESSAGE));
		assertThat(attributes).containsEntry("message", "custom message");
	}

}
