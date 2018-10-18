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

package org.springframework.boot.autoconfigure.web.servlet;

import java.net.URI;

import javax.servlet.MultipartConfigElement;

import org.junit.After;
import org.junit.Test;

import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Controller;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link MultipartAutoConfiguration}. Tests an empty configuration, no
 * multipart configuration, and a multipart configuration (with both Jetty and Tomcat).
 *
 * @author Greg Turnquist
 * @author Dave Syer
 * @author Josh Long
 * @author Ivan Sopov
 * @author Toshiaki Maki
 */
public class MultipartAutoConfigurationTests {

	private AnnotationConfigServletWebServerApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void webServerWithNothing() throws Exception {
		this.context = new AnnotationConfigServletWebServerApplicationContext(
				WebServerWithNothing.class, BaseConfiguration.class);
		DispatcherServlet servlet = this.context.getBean(DispatcherServlet.class);
		verify404();
		assertThat(servlet.getMultipartResolver()).isNotNull();
		assertThat(this.context.getBeansOfType(StandardServletMultipartResolver.class))
				.hasSize(1);
		assertThat(this.context.getBeansOfType(MultipartResolver.class)).hasSize(1);
	}

	@Test
	public void webServerWithNoMultipartJettyConfiguration() {
		this.context = new AnnotationConfigServletWebServerApplicationContext(
				WebServerWithNoMultipartJetty.class, BaseConfiguration.class);
		DispatcherServlet servlet = this.context.getBean(DispatcherServlet.class);
		assertThat(servlet.getMultipartResolver()).isNotNull();
		assertThat(this.context.getBeansOfType(StandardServletMultipartResolver.class))
				.hasSize(1);
		assertThat(this.context.getBeansOfType(MultipartResolver.class)).hasSize(1);
		verifyServletWorks();
	}

	@Test
	public void webServerWithNoMultipartUndertowConfiguration() {
		this.context = new AnnotationConfigServletWebServerApplicationContext(
				WebServerWithNoMultipartUndertow.class, BaseConfiguration.class);
		DispatcherServlet servlet = this.context.getBean(DispatcherServlet.class);
		verifyServletWorks();
		assertThat(servlet.getMultipartResolver()).isNotNull();
		assertThat(this.context.getBeansOfType(StandardServletMultipartResolver.class))
				.hasSize(1);
		assertThat(this.context.getBeansOfType(MultipartResolver.class)).hasSize(1);
	}

	@Test
	public void webServerWithNoMultipartTomcatConfiguration() {
		this.context = new AnnotationConfigServletWebServerApplicationContext(
				WebServerWithNoMultipartTomcat.class, BaseConfiguration.class);
		DispatcherServlet servlet = this.context.getBean(DispatcherServlet.class);
		assertThat(servlet.getMultipartResolver()).isNull();
		assertThat(this.context.getBeansOfType(StandardServletMultipartResolver.class))
				.hasSize(1);
		assertThat(this.context.getBeansOfType(MultipartResolver.class)).hasSize(1);
		verifyServletWorks();
	}

	@Test
	public void webServerWithAutomatedMultipartJettyConfiguration() {
		this.context = new AnnotationConfigServletWebServerApplicationContext(
				WebServerWithEverythingJetty.class, BaseConfiguration.class);
		this.context.getBean(MultipartConfigElement.class);
		assertThat(this.context.getBean(StandardServletMultipartResolver.class)).isSameAs(
				this.context.getBean(DispatcherServlet.class).getMultipartResolver());
		verifyServletWorks();
	}

	@Test
	public void webServerWithAutomatedMultipartTomcatConfiguration() {
		this.context = new AnnotationConfigServletWebServerApplicationContext(
				WebServerWithEverythingTomcat.class, BaseConfiguration.class);
		new RestTemplate().getForObject(
				"http://localhost:" + this.context.getWebServer().getPort() + "/",
				String.class);
		this.context.getBean(MultipartConfigElement.class);
		assertThat(this.context.getBean(StandardServletMultipartResolver.class)).isSameAs(
				this.context.getBean(DispatcherServlet.class).getMultipartResolver());
		verifyServletWorks();
	}

	@Test
	public void webServerWithAutomatedMultipartUndertowConfiguration() {
		this.context = new AnnotationConfigServletWebServerApplicationContext(
				WebServerWithEverythingUndertow.class, BaseConfiguration.class);
		this.context.getBean(MultipartConfigElement.class);
		verifyServletWorks();
		assertThat(this.context.getBean(StandardServletMultipartResolver.class)).isSameAs(
				this.context.getBean(DispatcherServlet.class).getMultipartResolver());
	}

	@Test
	public void webServerWithMultipartConfigDisabled() {
		testWebServerWithCustomMultipartConfigEnabledSetting("false", 0);
	}

	@Test
	public void webServerWithMultipartConfigEnabled() {
		testWebServerWithCustomMultipartConfigEnabledSetting("true", 1);
	}

	private void testWebServerWithCustomMultipartConfigEnabledSetting(
			final String propertyValue, int expectedNumberOfMultipartConfigElementBeans) {
		this.context = new AnnotationConfigServletWebServerApplicationContext();
		TestPropertyValues.of("spring.servlet.multipart.enabled=" + propertyValue)
				.applyTo(this.context);
		this.context.register(WebServerWithNoMultipartTomcat.class,
				BaseConfiguration.class);
		this.context.refresh();
		this.context.getBean(MultipartProperties.class);
		assertThat(this.context.getBeansOfType(MultipartConfigElement.class))
				.hasSize(expectedNumberOfMultipartConfigElementBeans);
	}

	@Test
	public void webServerWithCustomMultipartResolver() {
		this.context = new AnnotationConfigServletWebServerApplicationContext(
				WebServerWithCustomMultipartResolver.class, BaseConfiguration.class);
		MultipartResolver multipartResolver = this.context
				.getBean(MultipartResolver.class);
		assertThat(multipartResolver)
				.isNotInstanceOf(StandardServletMultipartResolver.class);
		assertThat(this.context.getBeansOfType(MultipartConfigElement.class)).hasSize(1);
	}

	@Test
	public void containerWithCommonsMultipartResolver() throws Exception {
		this.context = new AnnotationConfigServletWebServerApplicationContext(
				ContainerWithCommonsMultipartResolver.class, BaseConfiguration.class);
		MultipartResolver multipartResolver = this.context
				.getBean(MultipartResolver.class);
		assertThat(multipartResolver).isInstanceOf(CommonsMultipartResolver.class);
		assertThat(this.context.getBeansOfType(MultipartConfigElement.class)).hasSize(0);
	}

	@Test
	public void configureResolveLazily() {
		this.context = new AnnotationConfigServletWebServerApplicationContext();
		TestPropertyValues.of("spring.servlet.multipart.resolve-lazily=true")
				.applyTo(this.context);
		this.context.register(WebServerWithNothing.class, BaseConfiguration.class);
		this.context.refresh();
		StandardServletMultipartResolver multipartResolver = this.context
				.getBean(StandardServletMultipartResolver.class);
		boolean resolveLazily = (Boolean) ReflectionTestUtils.getField(multipartResolver,
				"resolveLazily");
		assertThat(resolveLazily).isTrue();
	}

	private void verify404() throws Exception {
		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
		ClientHttpRequest request = requestFactory.createRequest(new URI(
				"http://localhost:" + this.context.getWebServer().getPort() + "/"),
				HttpMethod.GET);
		ClientHttpResponse response = request.execute();
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	private void verifyServletWorks() {
		RestTemplate restTemplate = new RestTemplate();
		String url = "http://localhost:" + this.context.getWebServer().getPort() + "/";
		assertThat(restTemplate.getForObject(url, String.class)).isEqualTo("Hello");
	}

	@Configuration
	public static class WebServerWithNothing {

	}

	@Configuration
	public static class WebServerWithNoMultipartJetty {

		@Bean
		JettyServletWebServerFactory webServerFactory() {
			return new JettyServletWebServerFactory();
		}

		@Bean
		WebController controller() {
			return new WebController();
		}

	}

	@Configuration
	public static class WebServerWithNoMultipartUndertow {

		@Bean
		UndertowServletWebServerFactory webServerFactory() {
			return new UndertowServletWebServerFactory();
		}

		@Bean
		WebController controller() {
			return new WebController();
		}

	}

	@Configuration
	@Import({ ServletWebServerFactoryAutoConfiguration.class,
			DispatcherServletAutoConfiguration.class, MultipartAutoConfiguration.class })
	@EnableConfigurationProperties(MultipartProperties.class)
	protected static class BaseConfiguration {

		@Bean
		public ServerProperties serverProperties() {
			ServerProperties properties = new ServerProperties();
			properties.setPort(0);
			return properties;
		}

	}

	@Configuration
	public static class WebServerWithNoMultipartTomcat {

		@Bean
		TomcatServletWebServerFactory webServerFactory() {
			return new TomcatServletWebServerFactory();
		}

		@Bean
		WebController controller() {
			return new WebController();
		}

	}

	@Configuration
	public static class WebServerWithEverythingJetty {

		@Bean
		MultipartConfigElement multipartConfigElement() {
			return new MultipartConfigElement("");
		}

		@Bean
		JettyServletWebServerFactory webServerFactory() {
			return new JettyServletWebServerFactory();
		}

		@Bean
		WebController webController() {
			return new WebController();
		}

	}

	@Configuration
	@EnableWebMvc
	public static class WebServerWithEverythingTomcat {

		@Bean
		MultipartConfigElement multipartConfigElement() {
			return new MultipartConfigElement("");
		}

		@Bean
		TomcatServletWebServerFactory webServerFactory() {
			return new TomcatServletWebServerFactory();
		}

		@Bean
		WebController webController() {
			return new WebController();
		}

	}

	@Configuration
	@EnableWebMvc
	public static class WebServerWithEverythingUndertow {

		@Bean
		MultipartConfigElement multipartConfigElement() {
			return new MultipartConfigElement("");
		}

		@Bean
		UndertowServletWebServerFactory webServerFactory() {
			return new UndertowServletWebServerFactory();
		}

		@Bean
		WebController webController() {
			return new WebController();
		}

	}

	public static class WebServerWithCustomMultipartResolver {

		@Bean
		MultipartResolver multipartResolver() {
			return mock(MultipartResolver.class);
		}

	}

	public static class ContainerWithCommonsMultipartResolver {

		@Bean
		CommonsMultipartResolver multipartResolver() {
			return mock(CommonsMultipartResolver.class);
		}

	}

	@Controller
	public static class WebController {

		@RequestMapping("/")
		@ResponseBody
		public String index() {
			return "Hello";
		}

	}

}
