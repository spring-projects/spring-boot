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

package org.springframework.boot.autoconfigure.web;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizerBeanPostProcessor;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.MockEmbeddedServletContainerFactory;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.i18n.FixedLocaleResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;
import org.springframework.web.servlet.view.AbstractView;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

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
		this.context.register(Config.class, WebMvcAutoConfiguration.class,
				HttpMessageConvertersAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertEquals(3, this.context.getBeanNamesForType(HandlerAdapter.class).length);
		assertFalse(this.context.getBean(RequestMappingHandlerAdapter.class)
				.getMessageConverters().isEmpty());
		assertEquals(this.context.getBean(HttpMessageConverters.class).getConverters(),
				this.context.getBean(RequestMappingHandlerAdapter.class)
						.getMessageConverters());
	}

	@Test
	public void handerMappingsCreated() throws Exception {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		this.context.register(Config.class, WebMvcAutoConfiguration.class,
				HttpMessageConvertersAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertEquals(6, this.context.getBeanNamesForType(HandlerMapping.class).length);
	}

	@Test
	public void resourceHandlerMapping() throws Exception {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		this.context.register(Config.class, WebMvcAutoConfiguration.class,
				HttpMessageConvertersAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		Map<String, List<Resource>> mappingLocations = getMappingLocations();
		assertThat(mappingLocations.get("/**").size(), equalTo(5));
		assertThat(mappingLocations.get("/webjars/**").size(), equalTo(1));
		assertThat(mappingLocations.get("/webjars/**").get(0),
				equalTo((Resource) new ClassPathResource("/META-INF/resources/webjars/")));
	}

	@Test
	public void resourceHandlerMappingOverrideWebjars() throws Exception {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		this.context.register(WebJars.class, Config.class, WebMvcAutoConfiguration.class,
				HttpMessageConvertersAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		Map<String, List<Resource>> mappingLocations = getMappingLocations();
		assertThat(mappingLocations.get("/webjars/**").size(), equalTo(1));
		assertThat(mappingLocations.get("/webjars/**").get(0),
				equalTo((Resource) new ClassPathResource("/foo/")));
	}

	@Test
	public void resourceHandlerMappingOverrideAll() throws Exception {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		this.context.register(AllResources.class, Config.class,
				WebMvcAutoConfiguration.class,
				HttpMessageConvertersAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		Map<String, List<Resource>> mappingLocations = getMappingLocations();
		assertThat(mappingLocations.get("/**").size(), equalTo(1));
		assertThat(mappingLocations.get("/**").get(0),
				equalTo((Resource) new ClassPathResource("/foo/")));
	}

	public void noLocaleResolver() throws Exception {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		this.context.register(AllResources.class, Config.class,
				WebMvcAutoConfiguration.class,
				HttpMessageConvertersAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		this.thrown.expect(NoSuchBeanDefinitionException.class);
		this.context.getBean(LocaleResolver.class);
	}

	@Test
	public void overrideLocale() throws Exception {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		// set fixed locale
		EnvironmentTestUtils.addEnvironment(this.context, "spring.mvc.locale:en_UK");
		this.context.register(AllResources.class, Config.class,
				WebMvcAutoConfiguration.class,
				HttpMessageConvertersAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		// mock request and set user preferred locale
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addPreferredLocale(StringUtils.parseLocaleString("nl_NL"));
		LocaleResolver localeResolver = this.context.getBean(LocaleResolver.class);
		Locale locale = localeResolver.resolveLocale(request);
		assertThat(localeResolver, instanceOf(FixedLocaleResolver.class));
		// test locale resolver uses fixed locale and not user preferred locale
		assertThat(locale.toString(), equalTo("en_UK"));
	}

	@SuppressWarnings("unchecked")
	protected Map<String, List<Resource>> getMappingLocations()
			throws IllegalAccessException {
		SimpleUrlHandlerMapping mapping = (SimpleUrlHandlerMapping) this.context
				.getBean("resourceHandlerMapping");
		Field locationsField = ReflectionUtils.findField(
				ResourceHttpRequestHandler.class, "locations");
		locationsField.setAccessible(true);
		Map<String, List<Resource>> mappingLocations = new LinkedHashMap<String, List<Resource>>();
		for (Map.Entry<String, Object> entry : mapping.getHandlerMap().entrySet()) {
			ResourceHttpRequestHandler handler = (ResourceHttpRequestHandler) entry
					.getValue();
			mappingLocations.put(entry.getKey(),
					(List<Resource>) locationsField.get(handler));
		}
		return mappingLocations;
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
	protected static class WebJars extends WebMvcConfigurerAdapter {

		@Override
		public void addResourceHandlers(ResourceHandlerRegistry registry) {
			registry.addResourceHandler("/webjars/**").addResourceLocations(
					"classpath:/foo/");
		}

	}

	@Configuration
	protected static class AllResources extends WebMvcConfigurerAdapter {

		@Override
		public void addResourceHandlers(ResourceHandlerRegistry registry) {
			registry.addResourceHandler("/**").addResourceLocations("classpath:/foo/");
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
