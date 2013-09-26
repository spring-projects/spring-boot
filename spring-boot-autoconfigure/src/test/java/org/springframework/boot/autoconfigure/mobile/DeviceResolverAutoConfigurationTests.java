/*
 * Copyright 2013 the original author or authors.
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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.After;
import org.junit.Test;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizerBeanPostProcessor;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.MockEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mobile.device.DeviceHandlerMethodArgumentResolver;
import org.springframework.mobile.device.DeviceResolverHandlerInterceptor;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Tests for {@link DeviceResolverAutoConfiguration}.
 *
 * @author Roy Clarkson
 */
public class DeviceResolverAutoConfigurationTests {

	private static final MockEmbeddedServletContainerFactory containerFactory = new MockEmbeddedServletContainerFactory();

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
		assertNotNull(this.context.getBean(DeviceResolverHandlerInterceptor.class));
	}

	@Test
	public void deviceHandlerMethodArgumentResolverCreated() throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.register(DeviceResolverAutoConfiguration.class);
		this.context.refresh();
		assertNotNull(this.context.getBean(DeviceHandlerMethodArgumentResolver.class));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void deviceResolverHandlerInterceptorRegistered() throws Exception {
		AnnotationConfigEmbeddedWebApplicationContext context = new AnnotationConfigEmbeddedWebApplicationContext();
		context.register(Config.class, WebMvcAutoConfiguration.class,
				DeviceResolverAutoConfiguration.class);
		context.refresh();
		RequestMappingHandlerMapping mapping = (RequestMappingHandlerMapping) context
				.getBean("requestMappingHandlerMapping");
		Field interceptorsField = ReflectionUtils.findField(
				RequestMappingHandlerMapping.class, "interceptors");
		interceptorsField.setAccessible(true);
		List<Object> interceptors = (List<Object>) ReflectionUtils.getField(
				interceptorsField, mapping);
		context.close();
		for (Object o : interceptors) {
			if (o instanceof DeviceResolverHandlerInterceptor) {
				return;
			}
		}
		fail("DeviceResolverHandlerInterceptor was not registered.");
	}

	@Configuration
	protected static class Config {

		@Bean
		public EmbeddedServletContainerFactory containerFactory() {
			return containerFactory;
		}

		@Bean
		public EmbeddedServletContainerCustomizerBeanPostProcessor embeddedServletContainerCustomizerBeanPostProcessor() {
			return new EmbeddedServletContainerCustomizerBeanPostProcessor();
		}

	}

}
