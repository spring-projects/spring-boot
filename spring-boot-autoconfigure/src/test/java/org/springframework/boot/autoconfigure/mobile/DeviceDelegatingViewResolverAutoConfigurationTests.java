/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.autoconfigure.mobile;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.thymeleaf.spring4.view.ThymeleafViewResolver;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.PropertyAccessor;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.freemarker.FreeMarkerAutoConfiguration;
import org.springframework.boot.autoconfigure.groovy.template.GroovyTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.mustache.MustacheAutoConfiguration;
import org.springframework.boot.autoconfigure.mustache.web.MustacheViewResolver;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration;
import org.springframework.boot.autoconfigure.web.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.core.Ordered;
import org.springframework.mobile.device.view.LiteDeviceDelegatingViewResolver;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.freemarker.FreeMarkerViewResolver;
import org.springframework.web.servlet.view.groovy.GroovyMarkupViewResolver;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DeviceDelegatingViewResolverAutoConfiguration}.
 *
 * @author Roy Clarkson
 * @author Stephane Nicoll
 */
public class DeviceDelegatingViewResolverAutoConfigurationTests {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	private AnnotationConfigWebApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void deviceDelegatingViewResolverDefaultDisabled() throws Exception {
		load();
		this.thrown.expect(NoSuchBeanDefinitionException.class);
		this.context.getBean(LiteDeviceDelegatingViewResolver.class);
	}

	@Test
	public void deviceDelegatingJspResourceViewResolver() throws Exception {
		load("spring.mobile.devicedelegatingviewresolver.enabled:true");
		assertThat(this.context.getBeansOfType(LiteDeviceDelegatingViewResolver.class))
				.hasSize(1);
		InternalResourceViewResolver internalResourceViewResolver = this.context
				.getBean(InternalResourceViewResolver.class);
		assertLiteDeviceDelegatingViewResolver(internalResourceViewResolver,
				"deviceDelegatingJspViewResolver");
	}

	@Test
	public void deviceDelegatingFreeMarkerViewResolver() throws Exception {
		load(Collections.<Class<?>>singletonList(FreeMarkerAutoConfiguration.class),
				"spring.mobile.devicedelegatingviewresolver.enabled:true");
		assertThat(this.context.getBeansOfType(LiteDeviceDelegatingViewResolver.class))
				.hasSize(2);
		assertLiteDeviceDelegatingViewResolver(
				this.context.getBean(FreeMarkerViewResolver.class),
				"deviceDelegatingFreeMarkerViewResolver");
	}

	@Test
	public void deviceDelegatingGroovyMarkupViewResolver() throws Exception {
		load(Collections.<Class<?>>singletonList(GroovyTemplateAutoConfiguration.class),
				"spring.mobile.devicedelegatingviewresolver.enabled:true");
		assertThat(this.context.getBeansOfType(LiteDeviceDelegatingViewResolver.class))
				.hasSize(2);
		assertLiteDeviceDelegatingViewResolver(
				this.context.getBean(GroovyMarkupViewResolver.class),
				"deviceDelegatingGroovyMarkupViewResolver");
	}

	@Test
	public void deviceDelegatingMustacheViewResolver() throws Exception {
		load(Collections.<Class<?>>singletonList(MustacheAutoConfiguration.class),
				"spring.mobile.devicedelegatingviewresolver.enabled:true");
		assertThat(this.context.getBeansOfType(LiteDeviceDelegatingViewResolver.class))
				.hasSize(2);
		assertLiteDeviceDelegatingViewResolver(
				this.context.getBean(MustacheViewResolver.class),
				"deviceDelegatingMustacheViewResolver");
	}

	@Test
	public void deviceDelegatingThymeleafViewResolver() throws Exception {
		load(Collections.<Class<?>>singletonList(ThymeleafAutoConfiguration.class),
				"spring.mobile.devicedelegatingviewresolver.enabled:true");
		assertThat(this.context.getBeansOfType(LiteDeviceDelegatingViewResolver.class))
				.hasSize(2);
		assertLiteDeviceDelegatingViewResolver(
				this.context.getBean(ThymeleafViewResolver.class),
				"deviceDelegatingThymeleafViewResolver");
	}

	public void assertLiteDeviceDelegatingViewResolver(ViewResolver delegate,
			String delegatingBeanName) {
		LiteDeviceDelegatingViewResolver deviceDelegatingViewResolver = this.context
				.getBean(delegatingBeanName, LiteDeviceDelegatingViewResolver.class);
		assertThat(deviceDelegatingViewResolver.getViewResolver()).isSameAs(delegate);
		assertThat(deviceDelegatingViewResolver.getOrder())
				.isEqualTo(((Ordered) delegate).getOrder() - 1);
	}

	@Test
	public void deviceDelegatingViewResolverDisabled() throws Exception {
		load(Arrays.asList(FreeMarkerAutoConfiguration.class,
				GroovyTemplateAutoConfiguration.class, MustacheAutoConfiguration.class,
				ThymeleafAutoConfiguration.class),
				"spring.mobile.devicedelegatingviewresolver.enabled:false");
		assertThat(this.context.getBeansOfType(LiteDeviceDelegatingViewResolver.class))
				.hasSize(0);
	}

	@Test
	public void defaultPropertyValues() throws Exception {
		load("spring.mobile.devicedelegatingviewresolver.enabled:true");
		LiteDeviceDelegatingViewResolver liteDeviceDelegatingViewResolver = this.context
				.getBean("deviceDelegatingJspViewResolver",
						LiteDeviceDelegatingViewResolver.class);
		DirectFieldAccessor accessor = new DirectFieldAccessor(
				liteDeviceDelegatingViewResolver);
		assertThat(accessor.getPropertyValue("enableFallback")).isEqualTo(Boolean.FALSE);
		assertThat(accessor.getPropertyValue("normalPrefix")).isEqualTo("");
		assertThat(accessor.getPropertyValue("mobilePrefix")).isEqualTo("mobile/");
		assertThat(accessor.getPropertyValue("tabletPrefix")).isEqualTo("tablet/");
		assertThat(accessor.getPropertyValue("normalSuffix")).isEqualTo("");
		assertThat(accessor.getPropertyValue("mobileSuffix")).isEqualTo("");
		assertThat(accessor.getPropertyValue("tabletSuffix")).isEqualTo("");
	}

	@Test
	public void overrideEnableFallback() throws Exception {
		PropertyAccessor accessor = getLiteDeviceDelegatingViewResolverAccessor(
				"spring.mobile.devicedelegatingviewresolver.enabled:true",
				"spring.mobile.devicedelegatingviewresolver.enableFallback:true");
		assertThat(accessor.getPropertyValue("enableFallback")).isEqualTo(Boolean.TRUE);
	}

	@Test
	public void overrideNormalPrefix() throws Exception {
		PropertyAccessor accessor = getLiteDeviceDelegatingViewResolverAccessor(
				"spring.mobile.devicedelegatingviewresolver.enabled:true",
				"spring.mobile.devicedelegatingviewresolver.normalPrefix:normal/");
		assertThat(accessor.getPropertyValue("normalPrefix")).isEqualTo("normal/");
	}

	@Test
	public void overrideMobilePrefix() throws Exception {
		PropertyAccessor accessor = getLiteDeviceDelegatingViewResolverAccessor(
				"spring.mobile.devicedelegatingviewresolver.enabled:true",
				"spring.mobile.devicedelegatingviewresolver.mobilePrefix:mob/");
		assertThat(accessor.getPropertyValue("mobilePrefix")).isEqualTo("mob/");
	}

	@Test
	public void overrideTabletPrefix() throws Exception {
		PropertyAccessor accessor = getLiteDeviceDelegatingViewResolverAccessor(
				"spring.mobile.devicedelegatingviewresolver.enabled:true",
				"spring.mobile.devicedelegatingviewresolver.tabletPrefix:tab/");
		assertThat(accessor.getPropertyValue("tabletPrefix")).isEqualTo("tab/");
	}

	@Test
	public void overrideNormalSuffix() throws Exception {
		PropertyAccessor accessor = getLiteDeviceDelegatingViewResolverAccessor(
				"spring.mobile.devicedelegatingviewresolver.enabled:true",
				"spring.mobile.devicedelegatingviewresolver.normalSuffix:.nor");
		assertThat(accessor.getPropertyValue("normalSuffix")).isEqualTo(".nor");
	}

	@Test
	public void overrideMobileSuffix() throws Exception {
		PropertyAccessor accessor = getLiteDeviceDelegatingViewResolverAccessor(
				"spring.mobile.devicedelegatingviewresolver.enabled:true",
				"spring.mobile.devicedelegatingviewresolver.mobileSuffix:.mob");
		assertThat(accessor.getPropertyValue("mobileSuffix")).isEqualTo(".mob");
	}

	@Test
	public void overrideTabletSuffix() throws Exception {
		PropertyAccessor accessor = getLiteDeviceDelegatingViewResolverAccessor(
				"spring.mobile.devicedelegatingviewresolver.enabled:true",
				"spring.mobile.devicedelegatingviewresolver.tabletSuffix:.tab");
		assertThat(accessor.getPropertyValue("tabletSuffix")).isEqualTo(".tab");
	}

	private PropertyAccessor getLiteDeviceDelegatingViewResolverAccessor(
			String... configuration) {
		load(configuration);
		LiteDeviceDelegatingViewResolver liteDeviceDelegatingViewResolver = this.context
				.getBean("deviceDelegatingJspViewResolver",
						LiteDeviceDelegatingViewResolver.class);
		return new DirectFieldAccessor(liteDeviceDelegatingViewResolver);
	}

	public void load(String... environment) {
		load(null, environment);
	}

	public void load(List<Class<?>> config, String... environment) {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.setServletContext(new MockServletContext());
		if (config != null) {
			this.context.register(config.toArray(new Class[config.size()]));
		}
		this.context.register(WebMvcAutoConfiguration.class,
				HttpMessageConvertersAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class,
				DeviceDelegatingViewResolverAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context, environment);
		this.context.refresh();
	}

}
