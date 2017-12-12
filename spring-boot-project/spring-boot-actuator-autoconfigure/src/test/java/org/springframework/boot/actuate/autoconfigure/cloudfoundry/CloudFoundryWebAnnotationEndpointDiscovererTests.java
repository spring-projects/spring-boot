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

package org.springframework.boot.actuate.autoconfigure.cloudfoundry;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.function.Function;

import org.junit.Test;

import org.springframework.boot.actuate.endpoint.EndpointInfo;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.cache.CachingOperationInvokerAdvisor;
import org.springframework.boot.actuate.endpoint.convert.ConversionServiceParameterMapper;
import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
import org.springframework.boot.actuate.endpoint.web.EndpointPathResolver;
import org.springframework.boot.actuate.endpoint.web.WebOperation;
import org.springframework.boot.actuate.endpoint.web.annotation.EndpointWebExtension;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.support.DefaultConversionService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CloudFoundryWebAnnotationEndpointDiscoverer}.
 *
 * @author Madhura Bhave
 */
public class CloudFoundryWebAnnotationEndpointDiscovererTests {

	@Test
	public void discovererShouldAddSuppliedExtensionForHealthEndpoint() {
		load(TestConfiguration.class, (endpointDiscoverer) -> {
			Collection<EndpointInfo<WebOperation>> endpoints = endpointDiscoverer
					.discoverEndpoints();
			assertThat(endpoints.size()).isEqualTo(2);
		});
	}

	private void load(Class<?> configuration,
			Consumer<CloudFoundryWebAnnotationEndpointDiscoverer> consumer) {
		this.load((id) -> null, (id) -> id, configuration, consumer);
	}

	private void load(Function<String, Long> timeToLive,
			EndpointPathResolver endpointPathResolver, Class<?> configuration,
			Consumer<CloudFoundryWebAnnotationEndpointDiscoverer> consumer) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				configuration);
		try {
			ConversionServiceParameterMapper parameterMapper = new ConversionServiceParameterMapper(
					DefaultConversionService.getSharedInstance());
			EndpointMediaTypes mediaTypes = new EndpointMediaTypes(
					Collections.singletonList("application/json"),
					Collections.singletonList("application/json"));
			CloudFoundryWebAnnotationEndpointDiscoverer discoverer = new CloudFoundryWebAnnotationEndpointDiscoverer(
					context, parameterMapper, mediaTypes, endpointPathResolver,
					Collections.singleton(new CachingOperationInvokerAdvisor(timeToLive)),
					null, HealthWebEndpointExtension.class);
			consumer.accept(discoverer);
		}
		finally {
			context.close();
		}
	}

	@Configuration
	static class TestConfiguration {

		@Bean
		public TestEndpoint testEndpoint() {
			return new TestEndpoint();
		}

		@Bean
		public HealthEndpoint healthEndpoint() {
			return new HealthEndpoint(null);
		}

		@Bean
		public TestWebEndpointExtension testEndpointExtension() {
			return new TestWebEndpointExtension();
		}

		@Bean
		public HealthWebEndpointExtension healthEndpointExtension() {
			return new HealthWebEndpointExtension();
		}

		@Bean
		public OtherHealthWebEndpointExtension otherHealthEndpointExtension() {
			return new OtherHealthWebEndpointExtension();
		}

	}

	@EndpointWebExtension(endpoint = TestEndpoint.class)
	static class TestWebEndpointExtension {

		@ReadOperation
		public Object getAll() {
			return null;
		}

	}

	@Endpoint(id = "test")
	static class TestEndpoint {

		@ReadOperation
		public Object getAll() {
			return null;
		}

	}

	@EndpointWebExtension(endpoint = HealthEndpoint.class)
	static class HealthWebEndpointExtension {

		@ReadOperation
		public Object getAll() {
			return null;
		}

	}

	@EndpointWebExtension(endpoint = HealthEndpoint.class)
	static class OtherHealthWebEndpointExtension {

		@ReadOperation
		public Object getAll() {
			return null;
		}

	}

}
