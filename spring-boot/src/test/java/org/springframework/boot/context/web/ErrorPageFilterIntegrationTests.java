/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.context.web;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.boot.context.web.ErrorPageFilterIntegrationTests.TomcatConfig;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ErrorPageFilter}.
 *
 * @author Dave Syer
 */
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
@SpringApplicationConfiguration(TomcatConfig.class)
@IntegrationTest
@WebAppConfiguration
public class ErrorPageFilterIntegrationTests {

	@Autowired
	private HelloWorldController controller;

	@Autowired
	private AnnotationConfigEmbeddedWebApplicationContext context;

	@After
	public void init() {
		this.controller.reset();
	}

	@Test
	public void created() throws Exception {
		doTest(this.context, "/create", HttpStatus.CREATED);
		assertThat(this.controller.getStatus()).isEqualTo(201);
	}

	@Test
	public void ok() throws Exception {
		doTest(this.context, "/hello", HttpStatus.OK);
		assertThat(this.controller.getStatus()).isEqualTo(200);
	}

	private void doTest(AnnotationConfigEmbeddedWebApplicationContext context,
			String resourcePath, HttpStatus status) throws Exception {
		int port = context.getEmbeddedServletContainer().getPort();
		TestRestTemplate template = new TestRestTemplate();
		ResponseEntity<String> entity = template.getForEntity(
				new URI("http://localhost:" + port + resourcePath), String.class);
		assertThat(entity.getBody()).isEqualTo("Hello World");
		assertThat(entity.getStatusCode()).isEqualTo(status);
	}

	@Configuration
	@EnableWebMvc
	public static class TomcatConfig {

		@Bean
		public EmbeddedServletContainerFactory containerFactory() {
			return new TomcatEmbeddedServletContainerFactory(0);
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
	public static class HelloWorldController extends WebMvcConfigurerAdapter {

		private int status;

		private CountDownLatch latch = new CountDownLatch(1);

		public int getStatus() throws InterruptedException {
			assertThat(this.latch.await(1, TimeUnit.SECONDS))
					.as("Timed out waiting for latch").isTrue();
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
				public void postHandle(HttpServletRequest request,
						HttpServletResponse response, Object handler,
						ModelAndView modelAndView) throws Exception {
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

}
