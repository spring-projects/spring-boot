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

package org.springframework.boot.autoconfigure.web;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizerBeanPostProcessor;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.MockEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.view.AbstractView;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link WebMvcAutoConfiguration}.
 * 
 * @author Phillip Webb
 * @author Dave Syer
 */
public class WebMvcAutoConfigurationTests {

	private static final MockEmbeddedServletContainerFactory containerFactory = new MockEmbeddedServletContainerFactory();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private AnnotationConfigEmbeddedWebApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void handerAdaptersCreated() throws Exception {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		this.context.register(Config.class, WebMvcAutoConfiguration.class);
		this.context.refresh();
		assertEquals(3, this.context.getBeanNamesForType(HandlerAdapter.class).length);
	}

	@Test
	public void handerMappingsCreated() throws Exception {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		this.context.register(Config.class, WebMvcAutoConfiguration.class);
		this.context.refresh();
		assertEquals(6, this.context.getBeanNamesForType(HandlerMapping.class).length);
	}

	@Test
	public void viewResolversCreatedIfViewsPresent() throws Exception {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		this.context.register(Config.class, ViewConfig.class,
				WebMvcAutoConfiguration.class);
		this.context.refresh();
		assertEquals(2, this.context.getBeanNamesForType(ViewResolver.class).length);
	}

	@Configuration
	protected static class ViewConfig {

		@Bean
		public View jsonView() {
			return new AbstractView() {

				@Override
				protected void renderMergedOutputModel(Map<String, Object> model,
						HttpServletRequest request, HttpServletResponse response)
						throws Exception {
					response.getOutputStream().write("Hello World".getBytes());
				}
			};
		}

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
