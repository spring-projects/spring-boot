/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.webmvc.actuate.endpoint.web;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.endpoint.Access;
import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.web.EndpointMapping;
import org.springframework.boot.actuate.endpoint.web.annotation.ControllerEndpoint;
import org.springframework.boot.actuate.endpoint.web.annotation.ExposableControllerEndpoint;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ControllerEndpointHandlerMapping}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @deprecated since 3.3.5 in favor of {@code @Endpoint} and {@code @WebEndpoint} support
 */
@Deprecated(since = "3.3.5", forRemoval = true)
@SuppressWarnings("removal")
class ControllerEndpointHandlerMappingTests {

	private final StaticApplicationContext context = new StaticApplicationContext();

	@Test
	void mappingWithNoPrefix() throws Exception {
		ExposableControllerEndpoint firstEndpoint = firstEndpoint();
		ExposableControllerEndpoint secondEndpoint = secondEndpoint();
		ControllerEndpointHandlerMapping mapping = createMapping("", firstEndpoint, secondEndpoint);
		HandlerExecutionChain firstHandler = mapping.getHandler(request("GET", "/first"));
		assertThat(firstHandler).isNotNull();
		assertThat(firstHandler.getHandler()).isEqualTo(handlerOf(firstEndpoint.getController(), "get"));
		HandlerExecutionChain secondHandler = mapping.getHandler(request("POST", "/second"));
		assertThat(secondHandler).isNotNull();
		assertThat(secondHandler.getHandler()).isEqualTo(handlerOf(secondEndpoint.getController(), "save"));
		HandlerExecutionChain thirdHandler = mapping.getHandler(request("GET", "/third"));
		assertThat(thirdHandler).isNull();
	}

	@Test
	void mappingWithPrefix() throws Exception {
		ExposableControllerEndpoint firstEndpoint = firstEndpoint();
		ExposableControllerEndpoint secondEndpoint = secondEndpoint();
		ControllerEndpointHandlerMapping mapping = createMapping("actuator", firstEndpoint, secondEndpoint);
		HandlerExecutionChain firstHandler = mapping.getHandler(request("GET", "/actuator/first"));
		assertThat(firstHandler).isNotNull();
		assertThat(firstHandler.getHandler()).isEqualTo(handlerOf(firstEndpoint.getController(), "get"));
		HandlerExecutionChain secondHandler = mapping.getHandler(request("POST", "/actuator/second"));
		assertThat(secondHandler).isNotNull();
		assertThat(secondHandler.getHandler()).isEqualTo(handlerOf(secondEndpoint.getController(), "save"));
		assertThat(mapping.getHandler(request("GET", "/first"))).isNull();
		assertThat(mapping.getHandler(request("GET", "/second"))).isNull();
	}

	@Test
	void mappingNarrowedToMethod() {
		ExposableControllerEndpoint first = firstEndpoint();
		ControllerEndpointHandlerMapping mapping = createMapping("actuator", first);
		assertThatExceptionOfType(HttpRequestMethodNotSupportedException.class)
			.isThrownBy(() -> mapping.getHandler(request("POST", "/actuator/first")));
	}

	@Test
	void mappingWithNoPath() throws Exception {
		ExposableControllerEndpoint pathless = pathlessEndpoint();
		ControllerEndpointHandlerMapping mapping = createMapping("actuator", pathless);
		HandlerExecutionChain handler = mapping.getHandler(request("GET", "/actuator/pathless"));
		assertThat(handler).isNotNull();
		assertThat(handler.getHandler()).isEqualTo(handlerOf(pathless.getController(), "get"));
		assertThat(mapping.getHandler(request("GET", "/pathless"))).isNull();
		assertThat(mapping.getHandler(request("GET", "/"))).isNull();
	}

	private ControllerEndpointHandlerMapping createMapping(String prefix, ExposableControllerEndpoint... endpoints) {
		ControllerEndpointHandlerMapping mapping = new ControllerEndpointHandlerMapping(new EndpointMapping(prefix),
				Arrays.asList(endpoints), null, (endpointId, defaultAccess) -> Access.UNRESTRICTED);
		mapping.setApplicationContext(this.context);
		mapping.afterPropertiesSet();
		return mapping;
	}

	private HandlerMethod handlerOf(Object source, String methodName) {
		Method method = ReflectionUtils.findMethod(source.getClass(), methodName);
		assertThat(method).isNotNull();
		return new HandlerMethod(source, method);
	}

	private MockHttpServletRequest request(String method, String requestURI) {
		return new MockHttpServletRequest(method, requestURI);
	}

	private ExposableControllerEndpoint firstEndpoint() {
		return mockEndpoint(EndpointId.of("first"), new FirstTestMvcEndpoint());
	}

	private ExposableControllerEndpoint secondEndpoint() {
		return mockEndpoint(EndpointId.of("second"), new SecondTestMvcEndpoint());
	}

	private ExposableControllerEndpoint pathlessEndpoint() {
		return mockEndpoint(EndpointId.of("pathless"), new PathlessControllerEndpoint());
	}

	private ExposableControllerEndpoint mockEndpoint(EndpointId id, Object controller) {
		ExposableControllerEndpoint endpoint = mock(ExposableControllerEndpoint.class);
		given(endpoint.getEndpointId()).willReturn(id);
		given(endpoint.getController()).willReturn(controller);
		given(endpoint.getRootPath()).willReturn(id.toString());
		return endpoint;
	}

	@ControllerEndpoint(id = "first")
	static class FirstTestMvcEndpoint {

		@GetMapping("/")
		String get() {
			return "test";
		}

	}

	@ControllerEndpoint(id = "second")
	static class SecondTestMvcEndpoint {

		@PostMapping("/")
		void save() {

		}

	}

	@ControllerEndpoint(id = "pathless")
	static class PathlessControllerEndpoint {

		@GetMapping
		String get() {
			return "test";
		}

	}

}
