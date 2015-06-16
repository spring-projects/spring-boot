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

package org.springframework.boot.web.servlet.view.velocity;

import org.apache.struts.mock.MockServletContext;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.view.velocity.VelocityConfigurer;
import org.springframework.web.servlet.view.velocity.VelocityView;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link EmbeddedVelocityViewResolver}.
 *
 * @author Phillip Webb
 */
public class EmbeddedVelocityViewResolverTests {

	@Test
	public void standardViewWithoutToolboxConfig() throws Exception {
		ApplicationContext context = loadContext(WithoutToolboxConfig.class);
		EmbeddedVelocityViewResolver resolver = context
				.getBean(EmbeddedVelocityViewResolver.class);
		Object viewClass = ReflectionTestUtils.getField(resolver, "viewClass");
		assertEquals(VelocityView.class, viewClass);
	}

	@Test
	public void embeddedViewWithToolboxConfig() throws Exception {
		ApplicationContext context = loadContext(WithToolboxConfig.class);
		EmbeddedVelocityViewResolver resolver = context
				.getBean(EmbeddedVelocityViewResolver.class);
		Object viewClass = ReflectionTestUtils.getField(resolver, "viewClass");
		assertEquals(EmbeddedVelocityToolboxView.class, viewClass);
	}

	private ApplicationContext loadContext(Class<?> config) {
		AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
		context.setServletContext(new MockServletContext());
		context.register(config);
		context.refresh();
		return context;
	}

	@Configuration
	static class WithoutToolboxConfig {

		@Bean
		public EmbeddedVelocityViewResolver resolver() {
			return new EmbeddedVelocityViewResolver();
		}

		@Bean
		public VelocityConfigurer velocityConfigurer() {
			return new VelocityConfigurer();
		}

	}

	@Configuration
	static class WithToolboxConfig {

		@Bean
		public EmbeddedVelocityViewResolver resolver() {
			EmbeddedVelocityViewResolver resolver = new EmbeddedVelocityViewResolver();
			resolver.setToolboxConfigLocation("/toolbox.xml");
			return resolver;
		}

		@Bean
		public VelocityConfigurer velocityConfigurer() {
			return new VelocityConfigurer();
		}

	}

}
