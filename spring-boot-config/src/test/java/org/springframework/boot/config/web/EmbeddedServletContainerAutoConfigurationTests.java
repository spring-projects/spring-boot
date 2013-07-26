/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.config.web;

import javax.servlet.Servlet;

import org.junit.Test;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.config.web.EmbeddedServletContainerAutoConfiguration;
import org.springframework.boot.strap.context.condition.ConditionalOnExpression;
import org.springframework.boot.strap.context.embedded.AnnotationConfigEmbeddedWebApplicationContext;
import org.springframework.boot.strap.context.embedded.ConfigurableEmbeddedServletContainerFactory;
import org.springframework.boot.strap.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.strap.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.strap.context.embedded.MockEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link EmbeddedServletContainerAutoConfiguration}.
 * 
 * @author Dave Syer
 */
public class EmbeddedServletContainerAutoConfigurationTests {

	private AnnotationConfigEmbeddedWebApplicationContext context;

	@Test
	public void createFromConfigClass() throws Exception {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext(
				EmbeddedContainerConfiguration.class,
				EmbeddedServletContainerAutoConfiguration.class);
		verifyContext();
	}

	@Test
	public void containerHasNoServletContext() throws Exception {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext(
				EmbeddedContainerConfiguration.class,
				EnsureContainerHasNoServletContext.class,
				EmbeddedServletContainerAutoConfiguration.class);
		verifyContext();
	}

	@Test
	public void customizeContainerThroughCallback() throws Exception {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext(
				EmbeddedContainerConfiguration.class,
				CallbackEmbeddedContainerCustomizer.class,
				EmbeddedServletContainerAutoConfiguration.class);
		verifyContext();
		assertEquals(9000, getContainerFactory().getPort());
	}

	private void verifyContext() {
		MockEmbeddedServletContainerFactory containerFactory = getContainerFactory();
		Servlet servlet = this.context.getBean(Servlet.class);
		verify(containerFactory.getServletContext()).addServlet("dispatcherServlet",
				servlet);
	}

	private MockEmbeddedServletContainerFactory getContainerFactory() {
		return this.context.getBean(MockEmbeddedServletContainerFactory.class);
	}

	@Configuration
	@ConditionalOnExpression("true")
	public static class EmbeddedContainerConfiguration {

		@Bean
		public EmbeddedServletContainerFactory containerFactory() {
			return new MockEmbeddedServletContainerFactory();
		}

	}

	@Component
	public static class EnsureContainerHasNoServletContext implements BeanPostProcessor {

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName)
				throws BeansException {
			if (bean instanceof ConfigurableEmbeddedServletContainerFactory) {
				MockEmbeddedServletContainerFactory containerFactory = (MockEmbeddedServletContainerFactory) bean;
				assertNull(containerFactory.getServletContext());
			}
			return bean;
		}

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName) {
			return bean;
		}

	}

	@Component
	public static class CallbackEmbeddedContainerCustomizer implements
			EmbeddedServletContainerCustomizer {
		@Override
		public void customize(ConfigurableEmbeddedServletContainerFactory factory) {
			factory.setPort(9000);
		}
	}

}
