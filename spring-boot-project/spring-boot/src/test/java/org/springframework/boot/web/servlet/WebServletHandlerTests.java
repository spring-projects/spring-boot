/*
 * Copyright 2012-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.web.servlet;

import java.io.IOException;
import java.util.Map;

import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;

import org.junit.jupiter.api.Test;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.SimpleBeanDefinitionRegistry;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link WebServletHandler}.
 *
 * @author Andy Wilkinson
 */
class WebServletHandlerTests {

	private final WebServletHandler handler = new WebServletHandler();

	private final SimpleBeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();

	@SuppressWarnings("unchecked")
	@Test
	void defaultServletConfiguration() throws IOException {
		AnnotatedBeanDefinition servletDefinition = createBeanDefinition(DefaultConfigurationServlet.class);
		this.handler.handle(servletDefinition, this.registry);
		BeanDefinition servletRegistrationBean = this.registry
				.getBeanDefinition(DefaultConfigurationServlet.class.getName());
		MutablePropertyValues propertyValues = servletRegistrationBean.getPropertyValues();
		assertThat(propertyValues.get("asyncSupported")).isEqualTo(false);
		assertThat(((Map<String, String>) propertyValues.get("initParameters"))).isEmpty();
		assertThat((Integer) propertyValues.get("loadOnStartup")).isEqualTo(-1);
		assertThat(propertyValues.get("name")).isEqualTo(DefaultConfigurationServlet.class.getName());
		assertThat((String[]) propertyValues.get("urlMappings")).isEmpty();
		assertThat(propertyValues.get("servlet")).isEqualTo(servletDefinition);
	}

	@Test
	void servletWithCustomName() throws IOException {
		AnnotatedBeanDefinition definition = createBeanDefinition(CustomNameServlet.class);
		this.handler.handle(definition, this.registry);
		BeanDefinition servletRegistrationBean = this.registry.getBeanDefinition("custom");
		MutablePropertyValues propertyValues = servletRegistrationBean.getPropertyValues();
		assertThat(propertyValues.get("name")).isEqualTo("custom");
	}

	@Test
	void asyncSupported() throws IOException {
		BeanDefinition servletRegistrationBean = handleBeanDefinitionForClass(AsyncSupportedServlet.class);
		MutablePropertyValues propertyValues = servletRegistrationBean.getPropertyValues();
		assertThat(propertyValues.get("asyncSupported")).isEqualTo(true);
	}

	@SuppressWarnings("unchecked")
	@Test
	void initParameters() throws IOException {
		BeanDefinition servletRegistrationBean = handleBeanDefinitionForClass(InitParametersServlet.class);
		MutablePropertyValues propertyValues = servletRegistrationBean.getPropertyValues();
		assertThat((Map<String, String>) propertyValues.get("initParameters")).containsEntry("a", "alpha")
				.containsEntry("b", "bravo");
	}

	@Test
	void urlMappings() throws IOException {
		BeanDefinition servletRegistrationBean = handleBeanDefinitionForClass(UrlPatternsServlet.class);
		MutablePropertyValues propertyValues = servletRegistrationBean.getPropertyValues();
		assertThat((String[]) propertyValues.get("urlMappings")).contains("alpha", "bravo");
	}

	@Test
	void urlMappingsFromValue() throws IOException {
		BeanDefinition servletRegistrationBean = handleBeanDefinitionForClass(UrlPatternsFromValueServlet.class);
		MutablePropertyValues propertyValues = servletRegistrationBean.getPropertyValues();
		assertThat((String[]) propertyValues.get("urlMappings")).contains("alpha", "bravo");
	}

	@Test
	void urlPatternsDeclaredTwice() {
		assertThatIllegalStateException()
				.isThrownBy(() -> handleBeanDefinitionForClass(UrlPatternsDeclaredTwiceServlet.class))
				.withMessageContaining("The urlPatterns and value attributes are mutually exclusive.");
	}

	private AnnotatedBeanDefinition createBeanDefinition(Class<?> servletClass) throws IOException {
		AnnotatedBeanDefinition definition = mock(AnnotatedBeanDefinition.class);
		given(definition.getBeanClassName()).willReturn(servletClass.getName());
		given(definition.getMetadata()).willReturn(
				new SimpleMetadataReaderFactory().getMetadataReader(servletClass.getName()).getAnnotationMetadata());
		return definition;
	}

	private BeanDefinition handleBeanDefinitionForClass(Class<?> filterClass) throws IOException {
		this.handler.handle(createBeanDefinition(filterClass), this.registry);
		return this.registry.getBeanDefinition(filterClass.getName());
	}

	@WebServlet
	class DefaultConfigurationServlet extends HttpServlet {

	}

	@WebServlet(asyncSupported = true)
	class AsyncSupportedServlet extends HttpServlet {

	}

	@WebServlet(initParams = { @WebInitParam(name = "a", value = "alpha"), @WebInitParam(name = "b", value = "bravo") })
	class InitParametersServlet extends HttpServlet {

	}

	@WebServlet(urlPatterns = { "alpha", "bravo" })
	class UrlPatternsServlet extends HttpServlet {

	}

	@WebServlet({ "alpha", "bravo" })
	class UrlPatternsFromValueServlet extends HttpServlet {

	}

	@WebServlet(value = { "alpha", "bravo" }, urlPatterns = { "alpha", "bravo" })
	class UrlPatternsDeclaredTwiceServlet extends HttpServlet {

	}

	@WebServlet(name = "custom")
	class CustomNameServlet extends HttpServlet {

	}

}
