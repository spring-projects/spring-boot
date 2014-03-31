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

package org.springframework.boot.context.embedded;

import java.net.URI;
import java.nio.charset.Charset;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.jetty.JettyEmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
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

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Integration tests for {@link EmbeddedWebApplicationContext} and
 * {@link EmbeddedServletContainer}s running Spring MVC.
 * 
 * @author Phillip Webb
 */
public class EmbeddedServletContainerMvcIntegrationTests {

	private AnnotationConfigEmbeddedWebApplicationContext context;

	@After
	public void closeContext() {
		try {
			this.context.close();
		}
		catch (Exception ex) {
		}
	}

	@Test
	public void tomcat() throws Exception {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext(
				TomcatConfig.class);
		doTest(this.context, "/hello");
	}

	@Test
	public void jetty() throws Exception {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext(
				JettyConfig.class);
		doTest(this.context, "/hello");
	}

	@Test
	public void advancedConfig() throws Exception {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext(
				AdvancedConfig.class);
		doTest(this.context, "/example/spring/hello");
	}

	private void doTest(AnnotationConfigEmbeddedWebApplicationContext context,
			String resourcePath) throws Exception {
		SimpleClientHttpRequestFactory clientHttpRequestFactory = new SimpleClientHttpRequestFactory();
		ClientHttpRequest request = clientHttpRequestFactory.createRequest(new URI(
				"http://localhost:" + context.getEmbeddedServletContainer().getPort()
						+ resourcePath), HttpMethod.GET);
		ClientHttpResponse response = request.execute();
		try {
			String actual = StreamUtils.copyToString(response.getBody(),
					Charset.forName("UTF-8"));
			assertThat(actual, equalTo("Hello World"));
		}
		finally {
			response.close();
		}
	}

	@Configuration
	@Import(Config.class)
	public static class TomcatConfig {
		@Bean
		public EmbeddedServletContainerFactory containerFactory() {
			return new TomcatEmbeddedServletContainerFactory(0);
		}
	}

	@Configuration
	@Import(Config.class)
	public static class JettyConfig {
		@Bean
		public EmbeddedServletContainerFactory containerFactory() {
			return new JettyEmbeddedServletContainerFactory(0);
		}
	}

	@Configuration
	@EnableWebMvc
	public static class Config {

		@Bean
		public DispatcherServlet dispatcherServlet() {
			return new DispatcherServlet();
			// Alternatively you can use ServletContextInitializer beans including
			// ServletRegistration and FilterRegistration. Read the
			// EmbeddedWebApplicationContext javadoc for details
		}

		@Bean
		public HelloWorldController helloWorldController() {
			return new HelloWorldController();
		}
	}

	@Configuration
	@EnableWebMvc
	@PropertySource("classpath:/org/springframework/boot/context/embedded/conf.properties")
	public static class AdvancedConfig {

		@Autowired
		private Environment env;

		@Bean
		public EmbeddedServletContainerFactory containerFactory() {
			JettyEmbeddedServletContainerFactory factory = new JettyEmbeddedServletContainerFactory(
					0);
			factory.setContextPath(this.env.getProperty("context"));
			return factory;
		}

		@Bean
		public ServletRegistrationBean dispatcherRegistration() {
			ServletRegistrationBean registration = new ServletRegistrationBean(
					dispatcherServlet());
			registration.addUrlMappings("/spring/*");
			return registration;
		}

		@Bean
		public DispatcherServlet dispatcherServlet() {
			DispatcherServlet dispatcherServlet = new DispatcherServlet();
			// Can configure dispatcher servlet here as would usually do via init-params
			return dispatcherServlet;
		}

		@Bean
		public HelloWorldController helloWorldController() {
			return new HelloWorldController();
		}
	}

	@Controller
	public static class HelloWorldController {

		@RequestMapping("/hello")
		@ResponseBody
		public String sayHello() {
			return "Hello World";
		}
	}

	// Simple main method for testing in a browser
	@SuppressWarnings("resource")
	public static void main(String[] args) {
		new AnnotationConfigEmbeddedWebApplicationContext(
				JettyEmbeddedServletContainerFactory.class, Config.class);
	}

}
