/*
 * Copyright 2012-2018 the original author or authors.
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
import java.util.stream.Collectors;

import org.junit.Test;

import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.ExposeExcludePropertyEndpointFilter;
import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.http.ActuatorMediaType;
import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint;
import org.springframework.boot.actuate.endpoint.web.PathMappedEndpoint;
import org.springframework.boot.actuate.endpoint.web.PathMapper;
import org.springframework.boot.actuate.endpoint.web.annotation.ControllerEndpointDiscoverer;
import org.springframework.boot.actuate.endpoint.web.annotation.ServletEndpointDiscoverer;
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
public class WebEndpointAutoConfigurationTests {

	private static final AutoConfigurations CONFIGURATIONS = AutoConfigurations
			.of(EndpointAutoConfiguration.class, WebEndpointAutoConfiguration.class);

	private WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withConfiguration(CONFIGURATIONS);

	@Test
	public void webApplicationConfiguresEndpointMediaTypes() {
		this.contextRunner.run((context) -> {
			EndpointMediaTypes endpointMediaTypes = context
					.getBean(EndpointMediaTypes.class);
			assertThat(endpointMediaTypes.getConsumed())
					.containsExactly(ActuatorMediaType.V2_JSON, "application/json");
		});
	}

	@Test
	public void webApplicationConfiguresPathMapper() {
		this.contextRunner
				.withPropertyValues(
						"management.endpoints.web.path-mapping.health=healthcheck")
				.run((context) -> {
					assertThat(context).hasSingleBean(PathMapper.class);
					String pathMapping = context.getBean(PathMapper.class)
							.getRootPath(EndpointId.of("health"));
					assertThat(pathMapping).isEqualTo("healthcheck");
				});
	}

	@Test
	public void webApplicationSupportCustomPathMatcher() {
		this.contextRunner
				.withPropertyValues("management.endpoints.web.exposure.include=*",
						"management.endpoints.web.path-mapping.testanotherone=foo")
				.withUserConfiguration(TestPathMatcher.class, TestOneEndpoint.class,
						TestAnotherOneEndpoint.class, TestTwoEndpoint.class)
				.run((context) -> {
					WebEndpointDiscoverer discoverer = context
							.getBean(WebEndpointDiscoverer.class);
					Collection<ExposableWebEndpoint> endpoints = discoverer
							.getEndpoints();
					ExposableWebEndpoint[] webEndpoints = endpoints
							.toArray(new ExposableWebEndpoint[0]);
					List<String> paths = Arrays.stream(webEndpoints)
							.map(PathMappedEndpoint::getRootPath)
							.collect(Collectors.toList());
					assertThat(paths).containsOnly("1/testone", "foo", "testtwo");
				});
	}

	@Test
	public void webApplicationConfiguresEndpointDiscoverer() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(ControllerEndpointDiscoverer.class);
			assertThat(context).hasSingleBean(WebEndpointDiscoverer.class);
		});
	}

	@Test
	public void webApplicationConfiguresExposeExcludePropertyEndpointFilter() {
		this.contextRunner.run((context) -> assertThat(context)
				.getBeans(ExposeExcludePropertyEndpointFilter.class)
				.containsKeys("webExposeExcludePropertyEndpointFilter",
						"controllerExposeExcludePropertyEndpointFilter"));
	}

	@Test
	public void contextShouldConfigureServletEndpointDiscoverer() {
		this.contextRunner.run((context) -> assertThat(context)
				.hasSingleBean(ServletEndpointDiscoverer.class));
	}

	@Test
	public void contextWhenNotServletShouldNotConfigureServletEndpointDiscoverer() {
		new ApplicationContextRunner().withConfiguration(CONFIGURATIONS)
				.run((context) -> assertThat(context)
						.doesNotHaveBean(ServletEndpointDiscoverer.class));
	}

	@Component
	private static class TestPathMatcher implements PathMapper {

		@Override
		public String getRootPath(EndpointId endpointId) {
			if (endpointId.toString().endsWith("one")) {
				return "1/" + endpointId.toString();
			}
			return null;
		}

	}

	@Component
	@Endpoint(id = "testone")
	private static class TestOneEndpoint {

	}

	@Component
	@Endpoint(id = "testanotherone")
	private static class TestAnotherOneEndpoint {

	}

	@Component
	@Endpoint(id = "testtwo")
	private static class TestTwoEndpoint {

	}

}
