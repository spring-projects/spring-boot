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

package org.springframework.boot.autoconfigure.mobile;

import org.junit.After;
import org.junit.Test;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.data.rest.RepositoryRestMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebAutoConfiguration;
import org.springframework.boot.autoconfigure.web.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mobile.device.Device;
import org.springframework.mobile.device.DeviceHandlerMethodArgumentResolver;
import org.springframework.mobile.device.DeviceResolverHandlerInterceptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletContext;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for {@link DeviceResolverAutoConfiguration}.
 *
 * @author Roy Clarkson
 * @author Andy Wilkinson
 */
public class DeviceResolverAutoConfigurationTests {

	private AnnotationConfigWebApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void deviceResolverHandlerInterceptorCreated() throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.register(DeviceResolverAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBean(DeviceResolverHandlerInterceptor.class))
				.isNotNull();
	}

	@Test
	public void deviceHandlerMethodArgumentResolverCreated() throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.register(DeviceResolverAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBean(DeviceHandlerMethodArgumentResolver.class))
				.isNotNull();
	}

	@Test
	public void deviceResolverHandlerInterceptorRegistered() throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.setServletContext(new MockServletContext());
		this.context.register(Config.class);
		this.context.refresh();
		RequestMappingHandlerMapping mapping = this.context
				.getBean(RequestMappingHandlerMapping.class);
		HandlerInterceptor[] interceptors = mapping
				.getHandler(new MockHttpServletRequest()).getInterceptors();
		assertThat(interceptors)
				.hasAtLeastOneElementOfType(DeviceResolverHandlerInterceptor.class);
	}

	@Test
	public void deviceHandlerMethodArgumentWorksWithSpringData() throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.register(Config.class);
		this.context.setServletContext(new MockServletContext());
		this.context.refresh();
		MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(this.context).build();
		mockMvc.perform(get("/")).andExpect(status().isOk());
	}

	@Configuration
	@ImportAutoConfiguration({ WebMvcAutoConfiguration.class,
			HttpMessageConvertersAutoConfiguration.class,
			DeviceResolverAutoConfiguration.class,
			PropertyPlaceholderAutoConfiguration.class,
			SpringDataWebAutoConfiguration.class,
			RepositoryRestMvcAutoConfiguration.class })
	protected static class Config {

		@Bean
		public MyController controller() {
			return new MyController();
		}

	}

	@Controller
	protected static class MyController {

		@RequestMapping("/")
		public ResponseEntity<Void> test(Device device) {
			if (device.getDevicePlatform() != null) {
				return new ResponseEntity<Void>(HttpStatus.OK);
			}
			return new ResponseEntity<Void>(HttpStatus.INTERNAL_SERVER_ERROR);
		}

	}

}
