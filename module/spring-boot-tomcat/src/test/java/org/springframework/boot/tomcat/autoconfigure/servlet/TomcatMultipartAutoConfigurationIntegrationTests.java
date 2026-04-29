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

package org.springframework.boot.tomcat.autoconfigure.servlet;

import java.net.URI;

import jakarta.servlet.MultipartConfigElement;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.servlet.autoconfigure.MultipartAutoConfiguration;
import org.springframework.boot.testsupport.classpath.ForkedClassPath;
import org.springframework.boot.testsupport.web.servlet.DirtiesUrlFactories;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;
import org.springframework.boot.web.server.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MultipartAutoConfiguration} integrated in tomcat. Tests an empty
 * configuration, no multipart configuration, and a multipart configuration.
 *
 * @author Greg Turnquist
 * @author Dave Syer
 * @author Josh Long
 * @author Ivan Sopov
 * @author Toshiaki Maki
 * @author Yanming Zhou
 * @author Hans Hosea Schaefer
 */
@DirtiesUrlFactories
class TomcatMultipartAutoConfigurationIntegrationTests {

	private @Nullable AnnotationConfigServletWebServerApplicationContext context;

	@AfterEach
	void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	void tomcatWebServerWithoutAnyController() throws Exception {
		this.context = new AnnotationConfigServletWebServerApplicationContext(
				MultipartTomcatIntegrationTestConfiguration.class);
		DispatcherServlet servlet = this.context.getBean(DispatcherServlet.class);
		verify404(this.context);
		assertThat(servlet.getMultipartResolver()).isNotNull();
		assertThat(this.context.getBeansOfType(StandardServletMultipartResolver.class)).hasSize(1);
		assertThat(this.context.getBeansOfType(MultipartResolver.class)).hasSize(1);
	}

	@Test
	@ForkedClassPath
	void tomcatWebServerWithNoMultipartConfiguration() {
		this.context = new AnnotationConfigServletWebServerApplicationContext(ControllerConfiguration.class,
				MultipartTomcatIntegrationTestConfiguration.class);
		assertThat(this.context.getBeansOfType(StandardServletMultipartResolver.class)).hasSize(1);
		assertThat(this.context.getBeansOfType(MultipartResolver.class)).hasSize(1);
		verifyServletWorks(this.context);
		assertThat(this.context.getBean(StandardServletMultipartResolver.class))
			.isSameAs(this.context.getBean(DispatcherServlet.class).getMultipartResolver());
	}

	@Test
	@ForkedClassPath
	void tomcatWebServerWithAutomatedMultipartConfiguration() {
		this.context = new AnnotationConfigServletWebServerApplicationContext(ControllerConfiguration.class,
				MultipartConfiguration.class, MultipartTomcatIntegrationTestConfiguration.class);
		this.context.getBean(MultipartConfigElement.class);
		verifyServletWorks(this.context);
		assertThat(this.context.getBean(StandardServletMultipartResolver.class))
			.isSameAs(this.context.getBean(DispatcherServlet.class).getMultipartResolver());
	}

	private void verify404(AnnotationConfigServletWebServerApplicationContext context) throws Exception {
		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
		WebServer webServer = context.getWebServer();
		assertThat(webServer).isNotNull();
		ClientHttpRequest request = requestFactory
			.createRequest(new URI("http://localhost:" + webServer.getPort() + "/"), HttpMethod.GET);
		try (ClientHttpResponse response = request.execute()) {
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		}
	}

	private void verifyServletWorks(AnnotationConfigServletWebServerApplicationContext context) {
		RestTemplate restTemplate = new RestTemplate();
		WebServer webServer = context.getWebServer();
		assertThat(webServer).isNotNull();
		String url = "http://localhost:" + webServer.getPort() + "/";
		assertThat(restTemplate.getForObject(url, String.class)).isEqualTo("Hello");
	}

	@Configuration(proxyBeanMethods = false)
	@Import({ TomcatServletWebServerAutoConfiguration.class, MultipartAutoConfiguration.class })
	static class MultipartTomcatIntegrationTestConfiguration {

		@Bean
		ServerProperties serverProperties() {
			ServerProperties properties = new ServerProperties();
			properties.setPort(0);
			return properties;
		}

		@Bean
		DispatcherServlet dispatcherServlet() {
			return new DispatcherServlet();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ControllerConfiguration {

		@Bean
		WebController controller() {
			return new WebController();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableWebMvc
	static class MultipartConfiguration {

		@Bean
		MultipartConfigElement multipartConfigElement() {
			return new MultipartConfigElement("");
		}

	}

	@Controller
	static class WebController {

		@RequestMapping("/")
		@ResponseBody
		String index() {
			return "Hello";
		}

	}

}
