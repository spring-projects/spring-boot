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

package org.springframework.boot.web.servlet.context;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Controller;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ServletWebServerApplicationContext} and {@link WebServer}s
 * running Spring MVC.
 *
 * @author Phillip Webb
 * @author Ivan Sopov
 */
class ServletWebServerMvcIntegrationTests {

	private AnnotationConfigServletWebServerApplicationContext context;

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
	void tomcat() throws Exception {
		this.context = new AnnotationConfigServletWebServerApplicationContext(TomcatConfig.class);
		doTest(this.context, "/hello");
	}

	@Test
	void jetty() throws Exception {
		this.context = new AnnotationConfigServletWebServerApplicationContext(JettyConfig.class);
		doTest(this.context, "/hello");
	}

	@Test
	void undertow() throws Exception {
		this.context = new AnnotationConfigServletWebServerApplicationContext(UndertowConfig.class);
		doTest(this.context, "/hello");
	}

	@Test
	void advancedConfig() throws Exception {
		this.context = new AnnotationConfigServletWebServerApplicationContext(AdvancedConfig.class);
		doTest(this.context, "/example/spring/hello");
	}

	private void doTest(AnnotationConfigServletWebServerApplicationContext context, String resourcePath)
			throws Exception {
		SimpleClientHttpRequestFactory clientHttpRequestFactory = new SimpleClientHttpRequestFactory();
		ClientHttpRequest request = clientHttpRequestFactory.createRequest(
				new URI("http://localhost:" + context.getWebServer().getPort() + resourcePath), HttpMethod.GET);
		try (ClientHttpResponse response = request.execute()) {
			String actual = StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);
			assertThat(actual).isEqualTo("Hello World");
		}
	}

	// Simple main method for testing in a browser
	@SuppressWarnings("resource")
	static void main(String[] args) {
		new AnnotationConfigServletWebServerApplicationContext(JettyServletWebServerFactory.class, Config.class);
	}

	@Configuration(proxyBeanMethods = false)
	@Import(Config.class)
	static class TomcatConfig {

		@Bean
		ServletWebServerFactory webServerFactory() {
			return new TomcatServletWebServerFactory(0);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(Config.class)
	static class JettyConfig {

		@Bean
		ServletWebServerFactory webServerFactory() {
			return new JettyServletWebServerFactory(0);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(Config.class)
	static class UndertowConfig {

		@Bean
		ServletWebServerFactory webServerFactory() {
			return new UndertowServletWebServerFactory(0);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableWebMvc
	static class Config {

		@Bean
		DispatcherServlet dispatcherServlet() {
			return new DispatcherServlet();
			// Alternatively you can use ServletContextInitializer beans including
			// ServletRegistration and FilterRegistration. Read the
			// EmbeddedWebApplicationContext Javadoc for details.
		}

		@Bean
		HelloWorldController helloWorldController() {
			return new HelloWorldController();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableWebMvc
	@PropertySource("classpath:/org/springframework/boot/web/servlet/context/conf.properties")
	static class AdvancedConfig {

		private final Environment env;

		AdvancedConfig(Environment env) {
			this.env = env;
		}

		@Bean
		ServletWebServerFactory webServerFactory() {
			JettyServletWebServerFactory factory = new JettyServletWebServerFactory(0);
			factory.setContextPath(this.env.getProperty("context"));
			return factory;
		}

		@Bean
		ServletRegistrationBean<DispatcherServlet> dispatcherRegistration(DispatcherServlet dispatcherServlet) {
			ServletRegistrationBean<DispatcherServlet> registration = new ServletRegistrationBean<>(dispatcherServlet);
			registration.addUrlMappings("/spring/*");
			return registration;
		}

		@Bean
		DispatcherServlet dispatcherServlet() {
			// Can configure dispatcher servlet here as would usually do via init-params
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
