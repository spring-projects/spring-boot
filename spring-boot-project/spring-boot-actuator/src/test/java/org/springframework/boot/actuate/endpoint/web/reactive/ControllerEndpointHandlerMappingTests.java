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

package org.springframework.boot.actuate.endpoint.web.reactive;

import java.time.Duration;
import java.util.Arrays;

import org.junit.Test;

import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.web.EndpointMapping;
import org.springframework.boot.actuate.endpoint.web.annotation.ControllerEndpoint;
import org.springframework.boot.actuate.endpoint.web.annotation.ExposableControllerEndpoint;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.server.MethodNotAllowedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ControllerEndpointHandlerMapping}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
public class ControllerEndpointHandlerMappingTests {

	private final StaticApplicationContext context = new StaticApplicationContext();

	@Test
	public void mappingWithNoPrefix() throws Exception {
		ExposableControllerEndpoint first = firstEndpoint();
		ExposableControllerEndpoint second = secondEndpoint();
		ControllerEndpointHandlerMapping mapping = createMapping("", first, second);
		assertThat(getHandler(mapping, HttpMethod.GET, "/first"))
				.isEqualTo(handlerOf(first.getController(), "get"));
		assertThat(getHandler(mapping, HttpMethod.POST, "/second"))
				.isEqualTo(handlerOf(second.getController(), "save"));
		assertThat(getHandler(mapping, HttpMethod.GET, "/third")).isNull();
	}

	@Test
	public void mappingWithPrefix() throws Exception {
		ExposableControllerEndpoint first = firstEndpoint();
		ExposableControllerEndpoint second = secondEndpoint();
		ControllerEndpointHandlerMapping mapping = createMapping("actuator", first,
				second);
		assertThat(getHandler(mapping, HttpMethod.GET, "/actuator/first"))
				.isEqualTo(handlerOf(first.getController(), "get"));
		assertThat(getHandler(mapping, HttpMethod.POST, "/actuator/second"))
				.isEqualTo(handlerOf(second.getController(), "save"));
		assertThat(getHandler(mapping, HttpMethod.GET, "/first")).isNull();
		assertThat(getHandler(mapping, HttpMethod.GET, "/second")).isNull();
	}

	@Test
	public void mappingWithNoPath() throws Exception {
		ExposableControllerEndpoint pathless = pathlessEndpoint();
		ControllerEndpointHandlerMapping mapping = createMapping("actuator", pathless);
		assertThat(getHandler(mapping, HttpMethod.GET, "/actuator/pathless"))
				.isEqualTo(handlerOf(pathless.getController(), "get"));
		assertThat(getHandler(mapping, HttpMethod.GET, "/pathless")).isNull();
		assertThat(getHandler(mapping, HttpMethod.GET, "/")).isNull();
	}

	@Test
	public void mappingNarrowedToMethod() throws Exception {
		ExposableControllerEndpoint first = firstEndpoint();
		ControllerEndpointHandlerMapping mapping = createMapping("actuator", first);
		assertThatExceptionOfType(MethodNotAllowedException.class).isThrownBy(
				() -> getHandler(mapping, HttpMethod.POST, "/actuator/first"));
	}

	private Object getHandler(ControllerEndpointHandlerMapping mapping, HttpMethod method,
			String requestURI) {
		return mapping.getHandler(exchange(method, requestURI))
				.block(Duration.ofSeconds(30));
	}

	private ControllerEndpointHandlerMapping createMapping(String prefix,
			ExposableControllerEndpoint... endpoints) {
		ControllerEndpointHandlerMapping mapping = new ControllerEndpointHandlerMapping(
				new EndpointMapping(prefix), Arrays.asList(endpoints), null);
		mapping.setApplicationContext(this.context);
		mapping.afterPropertiesSet();
		return mapping;
	}

	private HandlerMethod handlerOf(Object source, String methodName) {
		return new HandlerMethod(source,
				ReflectionUtils.findMethod(source.getClass(), methodName));
	}

	private MockServerWebExchange exchange(HttpMethod method, String requestURI) {
		return MockServerWebExchange
				.from(MockServerHttpRequest.method(method, requestURI).build());
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
	private static class FirstTestMvcEndpoint {

		@GetMapping("/")
		public String get() {
			return "test";
		}

	}

	@ControllerEndpoint(id = "second")
	private static class SecondTestMvcEndpoint {

		@PostMapping("/")
		public void save() {

		}

	}

	@ControllerEndpoint(id = "pathless")
	private static class PathlessControllerEndpoint {

		@GetMapping
		public String get() {
			return "test";
		}

	}

}
