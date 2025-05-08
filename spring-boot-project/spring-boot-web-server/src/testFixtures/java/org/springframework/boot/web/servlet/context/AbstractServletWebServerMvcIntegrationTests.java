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

package org.springframework.boot.web.servlet.context;

import java.net.URI;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.testsupport.classpath.resources.WithResource;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.server.WebServerFactoryCustomizerBeanPostProcessor;
import org.springframework.boot.web.server.servlet.ConfigurableServletWebServerFactory;
import org.springframework.boot.web.server.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base class for integration testing of {@link ServletWebServerApplicationContext} and
 * {@link WebServer}s running Spring MVC.
 *
 * @author Phillip Webb
 * @author Ivan Sopov
 */
public abstract class AbstractServletWebServerMvcIntegrationTests {

	private AnnotationConfigServletWebServerApplicationContext context;

	private Class<?> webServerConfiguration;

	protected AbstractServletWebServerMvcIntegrationTests(Class<?> webServerConfiguration) {
		this.webServerConfiguration = webServerConfiguration;
	}

	@AfterEach
	void closeContext() {
		try {
			this.context.close();
		}
		catch (Exception ex) {
			// Ignore
		}
	}

	@Test
	void basicConfig() throws Exception {
		this.context = new AnnotationConfigServletWebServerApplicationContext(this.webServerConfiguration,
				Config.class);
		doTest(this.context, "/hello");
	}

	@Test
	@WithResource(name = "conf.properties", content = "context=/example")
	void advancedConfig() throws Exception {
		this.context = new AnnotationConfigServletWebServerApplicationContext(this.webServerConfiguration,
				AdvancedConfig.class);
		doTest(this.context, "/example/spring/hello");
	}

	private void doTest(AnnotationConfigServletWebServerApplicationContext context, String resourcePath)
			throws Exception {
		SimpleClientHttpRequestFactory clientHttpRequestFactory = new SimpleClientHttpRequestFactory();
		ClientHttpRequest request = clientHttpRequestFactory.createRequest(
				new URI("http://localhost:" + context.getWebServer().getPort() + resourcePath), HttpMethod.GET);
		try (ClientHttpResponse response = request.execute()) {
			assertThat(response.getBody()).hasContent("Hello World");
		}
	}

	@Configuration(proxyBeanMethods = false)
	@EnableWebMvc
	static class Config {

		@Bean
		DispatcherServlet dispatcherServlet() {
			return new DispatcherServlet();
		}

		@Bean
		HelloWorldController helloWorldController() {
			return new HelloWorldController();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableWebMvc
	@PropertySource("classpath:conf.properties")
	static class AdvancedConfig {

		private final Environment env;

		AdvancedConfig(Environment env) {
			this.env = env;
		}

		@Bean
		static WebServerFactoryCustomizerBeanPostProcessor webServerFactoryCustomizerBeanPostProcessor() {
			return new WebServerFactoryCustomizerBeanPostProcessor();
		}

		@Bean
		WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> contextPathCustomizer() {
			return (factory) -> {
				String contextPath = this.env.getProperty("context");
				factory.setContextPath(contextPath);
			};
		}

		@Bean
		ServletRegistrationBean<DispatcherServlet> dispatcherRegistration(DispatcherServlet dispatcherServlet) {
			ServletRegistrationBean<DispatcherServlet> registration = new ServletRegistrationBean<>(dispatcherServlet);
			registration.addUrlMappings("/spring/*");
			return registration;
		}

		@Bean
		DispatcherServlet dispatcherServlet() {
			// Can configure dispatcher servlet here as would usually do through
			// init-params
			return new DispatcherServlet();
		}

		@Bean
		HelloWorldController helloWorldController() {
			return new HelloWorldController();
		}

	}

	@Controller
	static class HelloWorldController {

		@RequestMapping("/hello")
		@ResponseBody
		String sayHello() {
			return "Hello World";
		}

	}

}
