/*
 * Copyright 2012-2015 the original author or authors.
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

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.web.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mobile.device.site.SitePreferenceHandlerInterceptor;
import org.springframework.mobile.device.site.SitePreferenceHandlerMethodArgumentResolver;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletContext;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import static org.hamcrest.Matchers.hasItemInArray;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link SitePreferenceAutoConfiguration}.
 *
 * @author Roy Clarkson
 * @author Andy Wilkinson
 */
public class SitePreferenceAutoConfigurationTests {

	private AnnotationConfigWebApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void sitePreferenceHandlerInterceptorCreated() {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.register(SitePreferenceAutoConfiguration.class);
		this.context.refresh();
		assertNotNull(this.context.getBean(SitePreferenceHandlerInterceptor.class));
	}

	@Test
	public void sitePreferenceHandlerInterceptorEnabled() throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.mobile.sitepreference.enabled:true");
		this.context.register(SitePreferenceAutoConfiguration.class);
		this.context.refresh();
		assertNotNull(this.context.getBean(SitePreferenceHandlerInterceptor.class));
	}

	@Test(expected = NoSuchBeanDefinitionException.class)
	public void sitePreferenceHandlerInterceptorDisabled() {
		this.context = new AnnotationConfigWebApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.mobile.sitepreference.enabled:false");
		this.context.register(SitePreferenceAutoConfiguration.class);
		this.context.refresh();
		this.context.getBean(SitePreferenceHandlerInterceptor.class);
	}

	@Test
	public void sitePreferenceMethodArgumentResolverCreated() throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.register(SitePreferenceAutoConfiguration.class);
		this.context.refresh();
		assertNotNull(
				this.context.getBean(SitePreferenceHandlerMethodArgumentResolver.class));
	}

	@Test
	public void sitePreferenceMethodArgumentResolverEnabled() throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.mobile.sitepreference.enabled:true");
		this.context.register(SitePreferenceAutoConfiguration.class);
		this.context.refresh();
		assertNotNull(
				this.context.getBean(SitePreferenceHandlerMethodArgumentResolver.class));
	}

	@Test(expected = NoSuchBeanDefinitionException.class)
	public void sitePreferenceMethodArgumentResolverDisabled() {
		this.context = new AnnotationConfigWebApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.mobile.sitepreference.enabled:false");
		this.context.register(SitePreferenceAutoConfiguration.class);
		this.context.refresh();
		this.context.getBean(SitePreferenceHandlerMethodArgumentResolver.class);
	}

	@Test
	public void sitePreferenceHandlerInterceptorRegistered() throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.setServletContext(new MockServletContext());
		this.context.register(Config.class, WebMvcAutoConfiguration.class,
				HttpMessageConvertersAutoConfiguration.class,
				SitePreferenceAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		RequestMappingHandlerMapping mapping = this.context
				.getBean(RequestMappingHandlerMapping.class);
		HandlerInterceptor[] interceptors = mapping
				.getHandler(new MockHttpServletRequest()).getInterceptors();
		assertThat(interceptors,
				hasItemInArray(instanceOf(SitePreferenceHandlerInterceptor.class)));
	}

	@Configuration
	protected static class Config {

		@Bean
		public MyController controller() {
			return new MyController();
		}

	}

	@Controller
	protected static class MyController {

		@RequestMapping("/")
		public void test() {

		}

	}

}
