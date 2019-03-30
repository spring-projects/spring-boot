/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.cloudfoundry;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.function.Function;

import org.junit.Test;

import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.InvocationContext;
import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.invoke.convert.ConversionServiceParameterValueMapper;
import org.springframework.boot.actuate.endpoint.invoker.cache.CachingOperationInvokerAdvisor;
import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint;
import org.springframework.boot.actuate.endpoint.web.PathMapper;
import org.springframework.boot.actuate.endpoint.web.WebOperation;
import org.springframework.boot.actuate.endpoint.web.annotation.EndpointWebExtension;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.support.DefaultConversionService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link CloudFoundryWebEndpointDiscoverer}.
 *
 * @author Madhura Bhave
 */
public class CloudFoundryWebEndpointDiscovererTests {

	@Test
	public void getEndpointsShouldAddCloudFoundryHealthExtension() {
		load(TestConfiguration.class, (discoverer) -> {
			Collection<ExposableWebEndpoint> endpoints = discoverer.getEndpoints();
			assertThat(endpoints.size()).isEqualTo(2);
			for (ExposableWebEndpoint endpoint : endpoints) {
				if (endpoint.getEndpointId().equals(EndpointId.of("health"))) {
					WebOperation operation = findMainReadOperation(endpoint);
					assertThat(operation.invoke(new InvocationContext(
							mock(SecurityContext.class), Collections.emptyMap())))
									.isEqualTo("cf");
				}
			}
		});
	}

	private WebOperation findMainReadOperation(ExposableWebEndpoint endpoint) {
		for (WebOperation operation : endpoint.getOperations()) {
			if (operation.getRequestPredicate().getPath().equals("health")) {
				return operation;
			}
		}
		throw new IllegalStateException(
				"No main read operation found from " + endpoint.getOperations());
	}

	private void load(Class<?> configuration,
			Consumer<CloudFoundryWebEndpointDiscoverer> consumer) {
		this.load((id) -> null, EndpointId::toString, configuration, consumer);
	}

	private void load(Function<EndpointId, Long> timeToLive,
			PathMapper endpointPathMapper, Class<?> configuration,
			Consumer<CloudFoundryWebEndpointDiscoverer> consumer) {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				configuration)) {
			ConversionServiceParameterValueMapper parameterMapper = new ConversionServiceParameterValueMapper(
					DefaultConversionService.getSharedInstance());
			EndpointMediaTypes mediaTypes = new EndpointMediaTypes(
					Collections.singletonList("application/json"),
					Collections.singletonList("application/json"));
			CloudFoundryWebEndpointDiscoverer discoverer = new CloudFoundryWebEndpointDiscoverer(
					context, parameterMapper, mediaTypes,
					Collections.singletonList(endpointPathMapper),
					Collections.singleton(new CachingOperationInvokerAdvisor(timeToLive)),
					Collections.emptyList());
			consumer.accept(discoverer);
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class TestConfiguration {

		@Bean
		public TestEndpoint testEndpoint() {
			return new TestEndpoint();
		}

		@Bean
		public TestEndpointWebExtension testEndpointWebExtension() {
			return new TestEndpointWebExtension();
		}

		@Bean
		public HealthEndpoint healthEndpoint() {
			return new HealthEndpoint(mock(HealthIndicator.class));
		}

		@Bean
		public HealthEndpointWebExtension healthEndpointWebExtension() {
			return new HealthEndpointWebExtension();
		}

		@Bean
		public TestHealthEndpointCloudFoundryExtension testHealthEndpointCloudFoundryExtension() {
			return new TestHealthEndpointCloudFoundryExtension();
		}

	}

	@Endpoint(id = "test")
	static class TestEndpoint {

		@ReadOperation
		public Object getAll() {
			return null;
		}

	}

	@EndpointWebExtension(endpoint = TestEndpoint.class)
	static class TestEndpointWebExtension {

		@ReadOperation
		public Object getAll() {
			return null;
		}

	}

	@EndpointWebExtension(endpoint = HealthEndpoint.class)
	static class HealthEndpointWebExtension {

		@ReadOperation
		public Object getAll() {
			return null;
		}

	}

	@EndpointCloudFoundryExtension(endpoint = HealthEndpoint.class)
	static class TestHealthEndpointCloudFoundryExtension {

		@ReadOperation
		public Object getAll() {
			return "cf";
		}

	}

}
