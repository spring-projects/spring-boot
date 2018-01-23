/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.web.reactive.error;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.validation.BindingResult;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DefaultErrorAttributes}.
 *
 * @author Brian Clozel
 * @author Stephane Nicoll
 */
public class DefaultErrorAttributesTests {

	private static final ResponseStatusException NOT_FOUND = new ResponseStatusException(
			HttpStatus.NOT_FOUND);

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private DefaultErrorAttributes errorAttributes = new DefaultErrorAttributes();

	private List<HttpMessageReader<?>> readers = ServerCodecConfigurer.create()
			.getReaders();

	@Test
	public void missingExceptionAttribute() {
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage("Missing exception attribute in ServerWebExchange");
		MockServerWebExchange exchange = MockServerWebExchange
				.from(MockServerHttpRequest.get("/test").build());
		ServerRequest request = ServerRequest.create(exchange, this.readers);
		this.errorAttributes.getErrorAttributes(request, false);
	}

	@Test
	public void includeTimeStamp() {
		MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
		Map<String, Object> attributes = this.errorAttributes
				.getErrorAttributes(buildServerRequest(request, NOT_FOUND), false);
		assertThat(attributes.get("timestamp")).isInstanceOf(Date.class);
	}

	@Test
	public void defaultStatusCode() {
		Error error = new OutOfMemoryError("Test error");
		MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
		Map<String, Object> attributes = this.errorAttributes
				.getErrorAttributes(buildServerRequest(request, error), false);
		assertThat(attributes.get("error"))
				.isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
		assertThat(attributes.get("status")).isEqualTo(500);
	}

	@Test
	public void includeStatusCode() {
		MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
		Map<String, Object> attributes = this.errorAttributes
				.getErrorAttributes(buildServerRequest(request, NOT_FOUND), false);
		assertThat(attributes.get("error"))
				.isEqualTo(HttpStatus.NOT_FOUND.getReasonPhrase());
		assertThat(attributes.get("status")).isEqualTo(404);
	}

	@Test
	public void getError() {
		Error error = new OutOfMemoryError("Test error");
		MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
		ServerRequest serverRequest = buildServerRequest(request, error);
		Map<String, Object> attributes = this.errorAttributes
				.getErrorAttributes(serverRequest, false);
		assertThat(this.errorAttributes.getError(serverRequest)).isSameAs(error);
		assertThat(attributes.get("exception")).isNull();
		assertThat(attributes.get("message")).isEqualTo("Test error");
	}

	@Test
	public void includeException() {
		RuntimeException error = new RuntimeException("Test");
		this.errorAttributes = new DefaultErrorAttributes(true);
		MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
		ServerRequest serverRequest = buildServerRequest(request, error);
		Map<String, Object> attributes = this.errorAttributes
				.getErrorAttributes(serverRequest, false);
		assertThat(this.errorAttributes.getError(serverRequest)).isSameAs(error);
		assertThat(attributes.get("exception"))
				.isEqualTo(RuntimeException.class.getName());
		assertThat(attributes.get("message")).isEqualTo("Test");
	}

	@Test
	public void processResponseStatusException() {
		RuntimeException nested = new RuntimeException("Test");
		ResponseStatusException error = new ResponseStatusException(
				HttpStatus.BAD_REQUEST, "invalid request", nested);
		this.errorAttributes = new DefaultErrorAttributes(true);
		MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
		ServerRequest serverRequest = buildServerRequest(request, error);
		Map<String, Object> attributes = this.errorAttributes
				.getErrorAttributes(serverRequest, false);
		assertThat(attributes.get("status")).isEqualTo(400);
		assertThat(attributes.get("message")).isEqualTo("invalid request");
		assertThat(attributes.get("exception"))
				.isEqualTo(RuntimeException.class.getName());
		assertThat(this.errorAttributes.getError(serverRequest)).isSameAs(error);
	}

	@Test
	public void processResponseStatusExceptionWithNoNestedCause() {
		ResponseStatusException error = new ResponseStatusException(
				HttpStatus.NOT_ACCEPTABLE, "could not process request");
		this.errorAttributes = new DefaultErrorAttributes(true);
		MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
		ServerRequest serverRequest = buildServerRequest(request, error);
		Map<String, Object> attributes = this.errorAttributes
				.getErrorAttributes(serverRequest, false);
		assertThat(attributes.get("status")).isEqualTo(406);
		assertThat(attributes.get("message")).isEqualTo("could not process request");
		assertThat(attributes.get("exception"))
				.isEqualTo(ResponseStatusException.class.getName());
		assertThat(this.errorAttributes.getError(serverRequest)).isSameAs(error);
	}

	@Test
	public void notIncludeTrace() {
		RuntimeException ex = new RuntimeException("Test");
		MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
		Map<String, Object> attributes = this.errorAttributes
				.getErrorAttributes(buildServerRequest(request, ex), false);
		assertThat(attributes.get("trace")).isNull();
	}

	@Test
	public void includeTrace() {
		RuntimeException ex = new RuntimeException("Test");
		MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
		Map<String, Object> attributes = this.errorAttributes
				.getErrorAttributes(buildServerRequest(request, ex), true);
		assertThat(attributes.get("trace").toString()).startsWith("java.lang");
	}

	@Test
	public void includePath() {
		MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
		Map<String, Object> attributes = this.errorAttributes
				.getErrorAttributes(buildServerRequest(request, NOT_FOUND), false);
		assertThat(attributes.get("path")).isEqualTo("/test");
	}

	@Test
	public void extractBindingResultErrors() throws Exception {
		Method method = getClass().getMethod("method", String.class);
		MethodParameter stringParam = new MethodParameter(method, 0);
		BindingResult bindingResult = new MapBindingResult(
				Collections.singletonMap("a", "b"), "objectName");
		bindingResult.addError(new ObjectError("c", "d"));
		Exception ex = new WebExchangeBindException(stringParam, bindingResult);
		MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
		Map<String, Object> attributes = this.errorAttributes
				.getErrorAttributes(buildServerRequest(request, ex), false);
		assertThat(attributes.get("message")).asString()
				.startsWith("Validation failed for argument at index 0 in method: "
						+ "public int org.springframework.boot.web.reactive.error.DefaultErrorAttributesTests"
						+ ".method(java.lang.String), with 1 error(s)");
		assertThat(attributes.get("errors")).isEqualTo(bindingResult.getAllErrors());
	}

	private ServerRequest buildServerRequest(MockServerHttpRequest request,
			Throwable error) {
		ServerWebExchange exchange = MockServerWebExchange.from(request);
		this.errorAttributes.storeErrorInformation(error, exchange);
		return ServerRequest.create(exchange, this.readers);
	}

	public int method(String firstParam) {
		return 42;
	}

}
