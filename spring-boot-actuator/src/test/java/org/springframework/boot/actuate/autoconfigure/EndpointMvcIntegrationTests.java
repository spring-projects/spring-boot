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

package org.springframework.boot.actuate.autoconfigure;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.actuate.endpoint.mvc.EndpointHandlerMapping;
import org.springframework.boot.actuate.endpoint.mvc.EndpointHandlerMappingCustomizer;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.EmbeddedServletContainerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ErrorMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.web.HttpMessageConverters;
import org.springframework.boot.autoconfigure.web.ServerPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for MVC {@link Endpoint}s.
 *
 * @author Dave Syer
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext
public class EndpointMvcIntegrationTests {

	@LocalServerPort
	private int port;

	@Autowired
	private TestInterceptor interceptor;

	@Test
	public void envEndpointHidden() throws InterruptedException {
		String body = new TestRestTemplate().getForObject(
				"http://localhost:" + this.port + "/env/user.dir", String.class);
		assertThat(body).isNotNull().contains("spring-boot-actuator");
		assertThat(this.interceptor.invoked()).isTrue();
	}

	@Test
	public void healthEndpointNotHidden() throws InterruptedException {
		String body = new TestRestTemplate()
				.getForObject("http://localhost:" + this.port + "/health", String.class);
		assertThat(body).isNotNull().contains("status");
		assertThat(this.interceptor.invoked()).isTrue();
	}

	@Configuration
	@MinimalWebConfiguration
	@Import({ ManagementServerPropertiesAutoConfiguration.class,
			JacksonAutoConfiguration.class, EndpointAutoConfiguration.class,
			EndpointWebMvcAutoConfiguration.class })
	@RestController
	protected static class Application {

		private final List<HttpMessageConverter<?>> converters;

		public Application(
				ObjectProvider<List<HttpMessageConverter<?>>> convertersProvider) {
			this.converters = convertersProvider.getIfAvailable();
		}

		@RequestMapping("/{name}/{env}/{bar}")
		public Map<String, Object> master(@PathVariable String name,
				@PathVariable String env, @PathVariable String label) {
			return Collections.singletonMap("foo", (Object) "bar");
		}

		@RequestMapping("/{name}/{env}")
		public Map<String, Object> master(@PathVariable String name,
				@PathVariable String env) {
			return Collections.singletonMap("foo", (Object) "bar");
		}

		@Bean
		@ConditionalOnMissingBean
		public HttpMessageConverters messageConverters() {
			return new HttpMessageConverters(this.converters == null
					? Collections.<HttpMessageConverter<?>>emptyList() : this.converters);
		}

		@Bean
		public EndpointHandlerMappingCustomizer mappingCustomizer() {
			return new EndpointHandlerMappingCustomizer() {

				@Override
				public void customize(EndpointHandlerMapping mapping) {
					mapping.setInterceptors(new Object[] { interceptor() });
				}

			};
		}

		@Bean
		protected TestInterceptor interceptor() {
			return new TestInterceptor();
		}

	}

	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Import({ EmbeddedServletContainerAutoConfiguration.class,
			ServerPropertiesAutoConfiguration.class,
			DispatcherServletAutoConfiguration.class, WebMvcAutoConfiguration.class,
			JacksonAutoConfiguration.class, ErrorMvcAutoConfiguration.class,
			PropertyPlaceholderAutoConfiguration.class })
	protected @interface MinimalWebConfiguration {

	}

	protected static class TestInterceptor extends HandlerInterceptorAdapter {

		private final CountDownLatch latch = new CountDownLatch(1);

		@Override
		public void postHandle(HttpServletRequest request, HttpServletResponse response,
				Object handler, ModelAndView modelAndView) throws Exception {
			this.latch.countDown();
		}

		public boolean invoked() throws InterruptedException {
			return this.latch.await(30, TimeUnit.SECONDS);
		}

	}

}
