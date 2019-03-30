/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.autoconfigure.web.servlet.error;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.boot.autoconfigure.template.TemplateAvailabilityProvider;
import org.springframework.boot.autoconfigure.template.TemplateAvailabilityProviders;
import org.springframework.boot.autoconfigure.web.ResourceProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Tests for {@link DefaultErrorViewResolver}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
public class DefaultErrorViewResolverTests {

	private DefaultErrorViewResolver resolver;

	@Mock
	private TemplateAvailabilityProvider templateAvailabilityProvider;

	private ResourceProperties resourceProperties;

	private Map<String, Object> model = new HashMap<>();

	private HttpServletRequest request = new MockHttpServletRequest();

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
		applicationContext.refresh();
		this.resourceProperties = new ResourceProperties();
		TemplateAvailabilityProviders templateAvailabilityProviders = new TestTemplateAvailabilityProviders(
				this.templateAvailabilityProvider);
		this.resolver = new DefaultErrorViewResolver(applicationContext,
				this.resourceProperties, templateAvailabilityProviders);
	}

	@Test
	public void createWhenApplicationContextIsNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> new DefaultErrorViewResolver(null, new ResourceProperties()))
				.withMessageContaining("ApplicationContext must not be null");
	}

	@Test
	public void createWhenResourcePropertiesIsNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> new DefaultErrorViewResolver(mock(ApplicationContext.class), null))
				.withMessageContaining("ResourceProperties must not be null");
	}

	@Test
	public void resolveWhenNoMatchShouldReturnNull() {
		ModelAndView resolved = this.resolver.resolveErrorView(this.request,
				HttpStatus.NOT_FOUND, this.model);
		assertThat(resolved).isNull();
	}

	@Test
	public void resolveWhenExactTemplateMatchShouldReturnTemplate() {
		given(this.templateAvailabilityProvider.isTemplateAvailable(eq("error/404"),
				any(Environment.class), any(ClassLoader.class),
				any(ResourceLoader.class))).willReturn(true);
		ModelAndView resolved = this.resolver.resolveErrorView(this.request,
				HttpStatus.NOT_FOUND, this.model);
		assertThat(resolved).isNotNull();
		assertThat(resolved.getViewName()).isEqualTo("error/404");
		verify(this.templateAvailabilityProvider).isTemplateAvailable(eq("error/404"),
				any(Environment.class), any(ClassLoader.class),
				any(ResourceLoader.class));
		verifyNoMoreInteractions(this.templateAvailabilityProvider);
	}

	@Test
	public void resolveWhenSeries5xxTemplateMatchShouldReturnTemplate() {
		given(this.templateAvailabilityProvider.isTemplateAvailable(eq("error/5xx"),
				any(Environment.class), any(ClassLoader.class),
				any(ResourceLoader.class))).willReturn(true);
		ModelAndView resolved = this.resolver.resolveErrorView(this.request,
				HttpStatus.SERVICE_UNAVAILABLE, this.model);
		assertThat(resolved.getViewName()).isEqualTo("error/5xx");
	}

	@Test
	public void resolveWhenSeries4xxTemplateMatchShouldReturnTemplate() {
		given(this.templateAvailabilityProvider.isTemplateAvailable(eq("error/4xx"),
				any(Environment.class), any(ClassLoader.class),
				any(ResourceLoader.class))).willReturn(true);
		ModelAndView resolved = this.resolver.resolveErrorView(this.request,
				HttpStatus.NOT_FOUND, this.model);
		assertThat(resolved.getViewName()).isEqualTo("error/4xx");
	}

	@Test
	public void resolveWhenExactResourceMatchShouldReturnResource() throws Exception {
		setResourceLocation("/exact");
		ModelAndView resolved = this.resolver.resolveErrorView(this.request,
				HttpStatus.NOT_FOUND, this.model);
		MockHttpServletResponse response = render(resolved);
		assertThat(response.getContentAsString().trim()).isEqualTo("exact/404");
		assertThat(response.getContentType()).isEqualTo(MediaType.TEXT_HTML_VALUE);
	}

	@Test
	public void resolveWhenSeries4xxResourceMatchShouldReturnResource() throws Exception {
		setResourceLocation("/4xx");
		ModelAndView resolved = this.resolver.resolveErrorView(this.request,
				HttpStatus.NOT_FOUND, this.model);
		MockHttpServletResponse response = render(resolved);
		assertThat(response.getContentAsString().trim()).isEqualTo("4xx/4xx");
		assertThat(response.getContentType()).isEqualTo(MediaType.TEXT_HTML_VALUE);
	}

	@Test
	public void resolveWhenSeries5xxResourceMatchShouldReturnResource() throws Exception {
		setResourceLocation("/5xx");
		ModelAndView resolved = this.resolver.resolveErrorView(this.request,
				HttpStatus.INTERNAL_SERVER_ERROR, this.model);
		MockHttpServletResponse response = render(resolved);
		assertThat(response.getContentAsString().trim()).isEqualTo("5xx/5xx");
		assertThat(response.getContentType()).isEqualTo(MediaType.TEXT_HTML_VALUE);
	}

	@Test
	public void resolveWhenTemplateAndResourceMatchShouldFavorTemplate() {
		setResourceLocation("/exact");
		given(this.templateAvailabilityProvider.isTemplateAvailable(eq("error/404"),
				any(Environment.class), any(ClassLoader.class),
				any(ResourceLoader.class))).willReturn(true);
		ModelAndView resolved = this.resolver.resolveErrorView(this.request,
				HttpStatus.NOT_FOUND, this.model);
		assertThat(resolved.getViewName()).isEqualTo("error/404");
	}

	@Test
	public void resolveWhenExactResourceMatchAndSeriesTemplateMatchShouldFavorResource()
			throws Exception {
		setResourceLocation("/exact");
		given(this.templateAvailabilityProvider.isTemplateAvailable(eq("error/4xx"),
				any(Environment.class), any(ClassLoader.class),
				any(ResourceLoader.class))).willReturn(true);
		ModelAndView resolved = this.resolver.resolveErrorView(this.request,
				HttpStatus.NOT_FOUND, this.model);
		MockHttpServletResponse response = render(resolved);
		assertThat(response.getContentAsString().trim()).isEqualTo("exact/404");
		assertThat(response.getContentType()).isEqualTo(MediaType.TEXT_HTML_VALUE);
	}

	@Test
	public void orderShouldBeLowest() {
		assertThat(this.resolver.getOrder()).isEqualTo(Ordered.LOWEST_PRECEDENCE);
	}

	@Test
	public void setOrderShouldChangeOrder() {
		this.resolver.setOrder(123);
		assertThat(this.resolver.getOrder()).isEqualTo(123);
	}

	private void setResourceLocation(String path) {
		String packageName = getClass().getPackage().getName();
		this.resourceProperties.setStaticLocations(new String[] {
				"classpath:" + packageName.replace('.', '/') + path + "/" });
	}

	private MockHttpServletResponse render(ModelAndView modelAndView) throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();
		modelAndView.getView().render(this.model, this.request, response);
		return response;
	}

	private static class TestTemplateAvailabilityProviders
			extends TemplateAvailabilityProviders {

		TestTemplateAvailabilityProviders(TemplateAvailabilityProvider provider) {
			super(Collections.singletonList(provider));
		}

	}

}
