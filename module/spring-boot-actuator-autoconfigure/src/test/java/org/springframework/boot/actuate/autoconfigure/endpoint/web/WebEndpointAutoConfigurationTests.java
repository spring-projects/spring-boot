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

package org.springframework.boot.actuate.autoconfigure.endpoint.web;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.expose.IncludeExcludeEndpointFilter;
import org.springframework.boot.actuate.endpoint.ApiVersion;
import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint;
import org.springframework.boot.actuate.endpoint.web.PathMappedEndpoint;
import org.springframework.boot.actuate.endpoint.web.PathMapper;
import org.springframework.boot.actuate.endpoint.web.annotation.WebEndpointDiscoverer;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.stereotype.Component;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebEndpointAutoConfiguration}.
 *
 * @author Andy Wilkinson
 * @author Yunkun Huang
 * @author Phillip Webb
 */
class WebEndpointAutoConfigurationTests {

	private static final String V2_JSON = ApiVersion.V2.getProducedMimeType().toString();

	private static final String V3_JSON = ApiVersion.V3.getProducedMimeType().toString();

	private static final AutoConfigurations CONFIGURATIONS = AutoConfigurations.of(EndpointAutoConfiguration.class,
			WebEndpointAutoConfiguration.class);

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
		.withConfiguration(CONFIGURATIONS);

	@Test
	void webApplicationConfiguresEndpointMediaTypes() {
		this.contextRunner.run((context) -> {
			EndpointMediaTypes endpointMediaTypes = context.getBean(EndpointMediaTypes.class);
			assertThat(endpointMediaTypes.getConsumed()).containsExactly(V3_JSON, V2_JSON, "application/json");
		});
	}

	@Test
	void webApplicationConfiguresPathMapper() {
		this.contextRunner.withPropertyValues("management.endpoints.web.path-mapping.health=healthcheck")
			.run((context) -> {
				assertThat(context).hasSingleBean(PathMapper.class);
				String pathMapping = context.getBean(PathMapper.class).getRootPath(EndpointId.of("health"));
				assertThat(pathMapping).isEqualTo("healthcheck");
			});
	}

	@Test
	void webApplicationSupportCustomPathMatcher() {
		this.contextRunner
			.withPropertyValues("management.endpoints.web.exposure.include=*",
					"management.endpoints.web.path-mapping.testanotherone=foo")
			.withUserConfiguration(TestPathMatcher.class, TestOneEndpoint.class, TestAnotherOneEndpoint.class,
					TestTwoEndpoint.class)
			.run((context) -> {
				WebEndpointDiscoverer discoverer = context.getBean(WebEndpointDiscoverer.class);
				Collection<ExposableWebEndpoint> endpoints = discoverer.getEndpoints();
				ExposableWebEndpoint[] webEndpoints = endpoints.toArray(new ExposableWebEndpoint[0]);
				List<String> paths = Arrays.stream(webEndpoints).map(PathMappedEndpoint::getRootPath).toList();
				assertThat(paths).containsOnly("1/testone", "foo", "testtwo");
			});
	}

	@Test
	@SuppressWarnings("removal")
	void webApplicationConfiguresEndpointDiscoverer() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(
					org.springframework.boot.actuate.endpoint.web.annotation.ControllerEndpointDiscoverer.class);
			assertThat(context).hasSingleBean(WebEndpointDiscoverer.class);
		});
	}

	@Test
	void webApplicationConfiguresExposeExcludePropertyEndpointFilter() {
		this.contextRunner.run((context) -> assertThat(context).getBeans(IncludeExcludeEndpointFilter.class)
			.containsKeys("webExposeExcludePropertyEndpointFilter", "controllerExposeExcludePropertyEndpointFilter"));
	}

	@Test
	@SuppressWarnings("removal")
	void contextShouldConfigureServletEndpointDiscoverer() {
		this.contextRunner.run((context) -> assertThat(context)
			.hasSingleBean(org.springframework.boot.actuate.endpoint.web.annotation.ServletEndpointDiscoverer.class));
	}

	@Test
	@SuppressWarnings("removal")
	void contextWhenNotServletShouldNotConfigureServletEndpointDiscoverer() {
		new ApplicationContextRunner().withConfiguration(CONFIGURATIONS)
			.run((context) -> assertThat(context).doesNotHaveBean(
					org.springframework.boot.actuate.endpoint.web.annotation.ServletEndpointDiscoverer.class));
	}

	@Component
	static class TestPathMatcher implements PathMapper {

		@Override
		public @Nullable String getRootPath(EndpointId endpointId) {
			if (endpointId.toString().endsWith("one")) {
				return "1/" + endpointId;
			}
			return null;
		}

	}

	@Component
	@Endpoint(id = "testone")
	static class TestOneEndpoint {

		@ReadOperation
		String read() {
			return "read";
		}

	}

	@Component
	@Endpoint(id = "testanotherone")
	static class TestAnotherOneEndpoint {

		@ReadOperation
		String read() {
			return "read";
		}

	}

	@Component
	@Endpoint(id = "testtwo")
	static class TestTwoEndpoint {

		@ReadOperation
		String read() {
			return "read";
		}

	}

}
