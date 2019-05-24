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

package org.springframework.boot.web.servlet.support;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.xnio.channels.UnsupportedOptionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.boot.web.servlet.support.ErrorPageFilterIntegrationTests.EmbeddedWebContextLoader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.AbstractContextLoader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ErrorPageFilter}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 */
@ExtendWith(SpringExtension.class)
@DirtiesContext
@ContextConfiguration(classes = ErrorPageFilterIntegrationTests.TomcatConfig.class,
		loader = EmbeddedWebContextLoader.class)
class ErrorPageFilterIntegrationTests {

	@Autowired
	private HelloWorldController controller;

	@Autowired
	private AnnotationConfigServletWebServerApplicationContext context;

	@AfterEach
	public void init() {
		this.controller.reset();
	}

	@Test
	void created() throws Exception {
		doTest(this.context, "/create", HttpStatus.CREATED);
		assertThat(this.controller.getStatus()).isEqualTo(201);
	}

	@Test
	void ok() throws Exception {
		doTest(this.context, "/hello", HttpStatus.OK);
		assertThat(this.controller.getStatus()).isEqualTo(200);
	}

	private void doTest(AnnotationConfigServletWebServerApplicationContext context, String resourcePath,
			HttpStatus status) throws Exception {
		int port = context.getWebServer().getPort();
		RestTemplate template = new RestTemplate();
		ResponseEntity<String> entity = template.getForEntity(new URI("http://localhost:" + port + resourcePath),
				String.class);
		assertThat(entity.getBody()).isEqualTo("Hello World");
		assertThat(entity.getStatusCode()).isEqualTo(status);
	}

	@Configuration(proxyBeanMethods = false)
	@EnableWebMvc
	public static class TomcatConfig {

		@Bean
		public ServletWebServerFactory webServerFactory() {
			return new TomcatServletWebServerFactory(0);
		}

		@Bean
		public ErrorPageFilter errorPageFilter() {
			return new ErrorPageFilter();
		}

		@Bean
		public DispatcherServlet dispatcherServlet() {
			return new DispatcherServlet();
		}

		@Bean
		public HelloWorldController helloWorldController() {
			return new HelloWorldController();
		}

	}

	@Controller
	public static class HelloWorldController implements WebMvcConfigurer {

		private int status;

		private CountDownLatch latch = new CountDownLatch(1);

		public int getStatus() throws InterruptedException {
			assertThat(this.latch.await(1, TimeUnit.SECONDS)).as("Timed out waiting for latch").isTrue();
			return this.status;
		}

		public void setStatus(int status) {
			this.status = status;
		}

		public void reset() {
			this.status = 0;
			this.latch = new CountDownLatch(1);
		}

		@Override
		public void addInterceptors(InterceptorRegistry registry) {
			registry.addInterceptor(new HandlerInterceptorAdapter() {
				@Override
				public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
						ModelAndView modelAndView) {
					HelloWorldController.this.setStatus(response.getStatus());
					HelloWorldController.this.latch.countDown();
				}
			});
		}

		@RequestMapping("/hello")
		@ResponseBody
		public String sayHello() {
			return "Hello World";
		}

		@RequestMapping("/create")
		@ResponseBody
		@ResponseStatus(HttpStatus.CREATED)
		public String created() {
			return "Hello World";
		}

	}

	static class EmbeddedWebContextLoader extends AbstractContextLoader {

		private static final String[] EMPTY_RESOURCE_SUFFIXES = {};

		@Override
		public ApplicationContext loadContext(MergedContextConfiguration config) {
			AnnotationConfigServletWebServerApplicationContext context = new AnnotationConfigServletWebServerApplicationContext(
					config.getClasses());
			context.registerShutdownHook();
			return context;
		}

		@Override
		public ApplicationContext loadContext(String... locations) {
			throw new UnsupportedOptionException();
		}

		@Override
		protected String[] getResourceSuffixes() {
			return EMPTY_RESOURCE_SUFFIXES;
		}

		@Override
		protected String getResourceSuffix() {
			throw new UnsupportedOptionException();
		}

	}

}
