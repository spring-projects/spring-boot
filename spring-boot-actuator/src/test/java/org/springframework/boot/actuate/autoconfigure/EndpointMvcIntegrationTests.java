/*
 * Copyright 2012-2014 the original author or authors.
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
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.EndpointMvcIntegrationTests.Application;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.actuate.endpoint.mvc.EndpointHandlerMapping;
import org.springframework.boot.actuate.endpoint.mvc.EndpointHandlerMappingCustomizer;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.web.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.EmbeddedServletContainerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ErrorMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.web.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ServerPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Integration tests for MVC {@link Endpoint}s.
 *
 * @author Dave Syer
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@IntegrationTest("server.port=0")
@WebAppConfiguration
@DirtiesContext
public class EndpointMvcIntegrationTests {

	@Value("${local.server.port}")
	private int port;

	@Autowired
	private TestInterceptor interceptor;

	@Test
	public void envEndpointNotHidden() {
		String body = new TestRestTemplate().getForObject("http://localhost:" + this.port
				+ "/env/user.dir", String.class);
		assertNotNull(body);
		assertTrue("Wrong body: \n" + body, body.contains("spring-boot-actuator"));
		assertEquals(1, this.interceptor.getCount());
	}

	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Import({ EmbeddedServletContainerAutoConfiguration.class,
			ServerPropertiesAutoConfiguration.class,
			DispatcherServletAutoConfiguration.class, WebMvcAutoConfiguration.class,
			HttpMessageConvertersAutoConfiguration.class,
			ErrorMvcAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class })
	protected static @interface MinimalWebConfiguration {

	}

	@Configuration
	@MinimalWebConfiguration
	@Import({ ManagementServerPropertiesAutoConfiguration.class,
			EndpointAutoConfiguration.class, EndpointWebMvcAutoConfiguration.class })
	@RestController
	protected static class Application {

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

	protected static class TestInterceptor extends HandlerInterceptorAdapter {

		private final AtomicInteger count = new AtomicInteger(0);

		@Override
		public void postHandle(HttpServletRequest request, HttpServletResponse response,
				Object handler, ModelAndView modelAndView) throws Exception {
			this.count.incrementAndGet();
		}

		public int getCount() {
			return this.count.get();
		}

	}

}
