/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.actuate.autoconfigure;

import java.io.FileNotFoundException;
import java.net.SocketException;
import java.net.URI;
import java.nio.charset.Charset;

import org.junit.After;
import org.junit.Test;
import org.springframework.actuate.autoconfigure.EndpointWebMvcAutoConfiguration;
import org.springframework.actuate.autoconfigure.ManagementServerPropertiesAutoConfiguration;
import org.springframework.actuate.endpoint.AbstractEndpoint;
import org.springframework.actuate.endpoint.Endpoint;
import org.springframework.actuate.properties.ManagementServerProperties;
import org.springframework.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.autoconfigure.web.EmbeddedServletContainerAutoConfiguration;
import org.springframework.autoconfigure.web.ServerPropertiesAutoConfiguration;
import org.springframework.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.bootstrap.TestUtils;
import org.springframework.bootstrap.context.embedded.AnnotationConfigEmbeddedWebApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Controller;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link EndpointWebMvcAutoConfiguration}.
 * 
 * @author Phillip Webb
 * @author Greg Turnquist
 */
public class EndpointWebMvcAutoConfigurationTests {

	private AnnotationConfigEmbeddedWebApplicationContext applicationContext = new AnnotationConfigEmbeddedWebApplicationContext();

	@After
	public void close() {
		try {
			this.applicationContext.close();
		}
		catch (Exception ex) {
		}
	}

	@Test
	public void onSamePort() throws Exception {
		this.applicationContext.register(RootConfig.class,
				PropertyPlaceholderAutoConfiguration.class,
				EmbeddedServletContainerAutoConfiguration.class,
				WebMvcAutoConfiguration.class,
				ManagementServerPropertiesAutoConfiguration.class,
				EndpointWebMvcAutoConfiguration.class);
		this.applicationContext.refresh();
		assertContent("/controller", 8080, "controlleroutput");
		assertContent("/endpoint", 8080, "endpointoutput");
		assertContent("/controller", 8081, null);
		assertContent("/endpoint", 8081, null);
		this.applicationContext.close();
		assertAllClosed();
	}

	@Test
	public void onDifferentPort() throws Exception {
		this.applicationContext.register(RootConfig.class, DifferentPortConfig.class,
				PropertyPlaceholderAutoConfiguration.class,
				EmbeddedServletContainerAutoConfiguration.class,
				WebMvcAutoConfiguration.class,
				ManagementServerPropertiesAutoConfiguration.class,
				EndpointWebMvcAutoConfiguration.class);
		this.applicationContext.refresh();
		assertContent("/controller", 8080, "controlleroutput");
		assertContent("/endpoint", 8080, null);
		assertContent("/controller", 8081, null);
		assertContent("/endpoint", 8081, "endpointoutput");
		this.applicationContext.close();
		assertAllClosed();
	}

	@Test
	public void disabled() throws Exception {
		this.applicationContext.register(RootConfig.class, DisableConfig.class,
				PropertyPlaceholderAutoConfiguration.class,
				EmbeddedServletContainerAutoConfiguration.class,
				WebMvcAutoConfiguration.class,
				ManagementServerPropertiesAutoConfiguration.class,
				EndpointWebMvcAutoConfiguration.class);
		this.applicationContext.refresh();
		assertContent("/controller", 8080, "controlleroutput");
		assertContent("/endpoint", 8080, null);
		assertContent("/controller", 8081, null);
		assertContent("/endpoint", 8081, null);
		this.applicationContext.close();
		assertAllClosed();
	}

	@Test
	public void specificPortsViaProperties() throws Exception {
		TestUtils.addEnviroment(this.applicationContext, "server.port:7070",
				"management.port:7071");
		this.applicationContext.register(RootConfig.class,
				PropertyPlaceholderAutoConfiguration.class,
				ManagementServerPropertiesAutoConfiguration.class,
				ServerPropertiesAutoConfiguration.class,
				EmbeddedServletContainerAutoConfiguration.class,
				WebMvcAutoConfiguration.class, EndpointWebMvcAutoConfiguration.class);
		this.applicationContext.refresh();
		assertContent("/controller", 7070, "controlleroutput");
		assertContent("/endpoint", 7070, null);
		assertContent("/controller", 7071, null);
		assertContent("/endpoint", 7071, "endpointoutput");
		this.applicationContext.close();
		assertAllClosed();
	}

	@Test
	public void contextPath() throws Exception {
		TestUtils.addEnviroment(this.applicationContext, "management.contextPath:/test");
		this.applicationContext.register(RootConfig.class,
				PropertyPlaceholderAutoConfiguration.class,
				ManagementServerPropertiesAutoConfiguration.class,
				ServerPropertiesAutoConfiguration.class,
				EmbeddedServletContainerAutoConfiguration.class,
				WebMvcAutoConfiguration.class, EndpointWebMvcAutoConfiguration.class);
		this.applicationContext.refresh();
		assertContent("/controller", 8080, "controlleroutput");
		assertContent("/test/endpoint", 8080, "endpointoutput");
		this.applicationContext.close();
		assertAllClosed();
	}

	private void assertAllClosed() throws Exception {
		assertContent("/controller", 8080, null);
		assertContent("/endpoint", 8080, null);
		assertContent("/controller", 8081, null);
		assertContent("/endpoint", 8081, null);
	}

	public void assertContent(String url, int port, Object expected) throws Exception {
		SimpleClientHttpRequestFactory clientHttpRequestFactory = new SimpleClientHttpRequestFactory();
		ClientHttpRequest request = clientHttpRequestFactory.createRequest(new URI(
				"http://localhost:" + port + url), HttpMethod.GET);
		try {
			ClientHttpResponse response = request.execute();
			try {
				String actual = StreamUtils.copyToString(response.getBody(),
						Charset.forName("UTF-8"));
				assertThat(actual, equalTo(expected));
			}
			finally {
				response.close();
			}
		}
		catch (Exception ex) {
			if (expected == null) {
				if (SocketException.class.isInstance(ex)
						|| FileNotFoundException.class.isInstance(ex)) {
					return;
				}
			}
			throw ex;
		}
	}

	@Configuration
	public static class RootConfig {

		@Bean
		public TestController testController() {
			return new TestController();
		}

		@Bean
		public Endpoint<String> testEndpoint() {
			return new AbstractEndpoint<String>("/endpoint", false) {
				@Override
				public String invoke() {
					return "endpointoutput";
				}
			};
		}
	}

	@Controller
	public static class TestController {

		@RequestMapping("/controller")
		@ResponseBody
		public String requestMappedMethod() {
			return "controlleroutput";
		}

	}

	@Configuration
	public static class DifferentPortConfig {

		@Bean
		public ManagementServerProperties managementServerProperties() {
			ManagementServerProperties properties = new ManagementServerProperties();
			properties.setPort(8081);
			return properties;
		}

	}

	@Configuration
	public static class DisableConfig {

		@Bean
		public ManagementServerProperties managementServerProperties() {
			ManagementServerProperties properties = new ManagementServerProperties();
			properties.setPort(0);
			return properties;
		}

	}

}
