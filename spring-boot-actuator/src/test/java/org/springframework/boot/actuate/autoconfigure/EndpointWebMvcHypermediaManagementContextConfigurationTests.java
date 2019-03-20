/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure;

import java.net.URL;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.boot.actuate.endpoint.mvc.DocsMvcEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.HalJsonMvcEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.ManagementServletContext;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoints;
import org.springframework.boot.autoconfigure.web.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.hal.DefaultCurieProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link EndpointWebMvcHypermediaManagementContextConfiguration}.
 *
 * @author Andy Wilkinson
 * @author Madhura Bhave
 */
public class EndpointWebMvcHypermediaManagementContextConfigurationTests {

	private AnnotationConfigWebApplicationContext context;

	@Before
	public void setRequestAttributes() {
		RequestContextHolder.setRequestAttributes(
				new ServletRequestAttributes(new MockHttpServletRequest()));
	}

	@After
	public void resetRequestAttributes() {
		RequestContextHolder.resetRequestAttributes();
	}

	@After
	public void closeContext() {
		this.context.close();
	}

	@Test
	public void basicConfiguration() {
		load();
		assertThat(this.context.getBeansOfType(ManagementServletContext.class))
				.hasSize(1);
		assertThat(this.context.getBeansOfType(HalJsonMvcEndpoint.class)).hasSize(1);
		assertThat(this.context.getBeansOfType(DocsMvcEndpoint.class)).hasSize(1);
		assertThat(this.context.getBeansOfType(DefaultCurieProvider.class)).isEmpty();
	}

	@Test
	public void curiesEnabledWithDefaultPorts() {
		load("endpoints.docs.curies.enabled:true");
		assertThat(getCurieHref())
				.isEqualTo("http://localhost/docs/#spring_boot_actuator__{rel}");
	}

	@Test
	public void curiesEnabledWithRandomPorts() {
		load("endpoints.docs.curies.enabled:true", "server.port:0", "management.port:0");
		assertThat(getCurieHref())
				.isEqualTo("http://localhost/docs/#spring_boot_actuator__{rel}");
	}

	@Test
	public void curiesEnabledWithSpecificServerPort() {
		load("endpoints.docs.curies.enabled:true", "server.port:8080");
		assertThat(getCurieHref())
				.isEqualTo("http://localhost/docs/#spring_boot_actuator__{rel}");
	}

	@Test
	public void curiesEnabledWithSpecificManagementPort() {
		load("endpoints.docs.curies.enabled:true", "management.port:8081");
		assertThat(getCurieHref())
				.isEqualTo("http://localhost/docs/#spring_boot_actuator__{rel}");
	}

	@Test
	public void curiesEnabledWithSpecificManagementAndServerPorts() {
		load("endpoints.docs.curies.enabled:true", "server.port:8080",
				"management.port:8081");
		assertThat(getCurieHref())
				.isEqualTo("http://localhost/docs/#spring_boot_actuator__{rel}");
	}

	@Test
	public void halJsonMvcEndpointIsConditionalOnMissingBean() throws Exception {
		createContext();
		this.context.register(HalJsonConfiguration.class, TestConfiguration.class,
				HttpMessageConvertersAutoConfiguration.class,
				EndpointWebMvcHypermediaManagementContextConfiguration.class);
		this.context.refresh();
		HalJsonMvcEndpoint bean = this.context.getBean(HalJsonMvcEndpoint.class);
		assertThat(bean).isInstanceOf(TestHalJsonMvcEndpoint.class);
	}

	@Test
	public void docsMvcEndpointIsConditionalOnMissingBean() throws Exception {
		createContext();
		this.context.register(DocsConfiguration.class, TestConfiguration.class,
				HttpMessageConvertersAutoConfiguration.class,
				EndpointWebMvcHypermediaManagementContextConfiguration.class);
		this.context.refresh();
		DocsMvcEndpoint bean = this.context.getBean(DocsMvcEndpoint.class);
		assertThat(bean).isInstanceOf(TestDocsMvcEndpoint.class);
	}

	private void load(String... properties) {
		createContext();
		EnvironmentTestUtils.addEnvironment(this.context, properties);
		this.context.register(TestConfiguration.class,
				HttpMessageConvertersAutoConfiguration.class,
				EndpointWebMvcHypermediaManagementContextConfiguration.class);
		this.context.refresh();
	}

	private void createContext() {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.setClassLoader(new ClassLoader(getClass().getClassLoader()) {

			@Override
			public URL getResource(String name) {
				if ("META-INF/resources/spring-boot-actuator/docs/index.html"
						.equals(name)) {
					return super.getResource("actuator-docs-index.html");
				}
				return super.getResource(name);
			}

		});
	}

	private String getCurieHref() {
		DefaultCurieProvider curieProvider = this.context
				.getBean(DefaultCurieProvider.class);
		Link link = (Link) curieProvider.getCurieInformation(null).iterator().next();
		return link.getHref();
	}

	@Configuration
	@EnableConfigurationProperties({ ManagementServerProperties.class,
			ServerProperties.class })
	static class TestConfiguration {

		@Bean
		public MvcEndpoints mvcEndpoints() {
			return new MvcEndpoints();
		}

	}

	@Configuration
	static class DocsConfiguration {

		@Bean
		public DocsMvcEndpoint testDocsMvcEndpoint(
				ManagementServletContext managementServletContext) {
			return new TestDocsMvcEndpoint(managementServletContext);
		}

	}

	@Configuration
	static class HalJsonConfiguration {

		@Bean
		public HalJsonMvcEndpoint testHalJsonMvcEndpoint(
				ManagementServletContext managementServletContext) {
			return new TestHalJsonMvcEndpoint(managementServletContext);
		}

	}

	static class TestDocsMvcEndpoint extends DocsMvcEndpoint {

		TestDocsMvcEndpoint(ManagementServletContext managementServletContext) {
			super(managementServletContext);
		}

	}

	static class TestHalJsonMvcEndpoint extends HalJsonMvcEndpoint {

		TestHalJsonMvcEndpoint(ManagementServletContext managementServletContext) {
			super(managementServletContext);
		}

	}

}
