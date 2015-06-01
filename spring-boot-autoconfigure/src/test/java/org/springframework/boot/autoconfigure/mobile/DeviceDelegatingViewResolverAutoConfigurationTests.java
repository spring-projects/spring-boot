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

package org.springframework.boot.autoconfigure.mobile;

import java.lang.reflect.Field;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.mobile.DeviceDelegatingViewResolverAutoConfiguration.DeviceDelegatingViewResolverConfiguration;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration;
import org.springframework.boot.autoconfigure.web.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizerBeanPostProcessor;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.MockEmbeddedServletContainerFactory;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mobile.device.view.AbstractDeviceDelegatingViewResolver;
import org.springframework.mobile.device.view.LiteDeviceDelegatingViewResolver;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.thymeleaf.spring4.view.ThymeleafViewResolver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link DeviceDelegatingViewResolverAutoConfiguration}.
 *
 * @author Roy Clarkson
 */
public class DeviceDelegatingViewResolverAutoConfigurationTests {

	private static final MockEmbeddedServletContainerFactory containerFactory = new MockEmbeddedServletContainerFactory();

	private AnnotationConfigEmbeddedWebApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test(expected = NoSuchBeanDefinitionException.class)
	public void deviceDelegatingViewResolverDefaultDisabled() throws Exception {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		this.context.register(Config.class,
				DeviceDelegatingViewResolverConfiguration.class);
		this.context.refresh();
		this.context.getBean("deviceDelegatingViewResolver",
				AbstractDeviceDelegatingViewResolver.class);
	}

	@Test
	public void deviceDelegatingInternalResourceViewResolverEnabled() throws Exception {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.mobile.devicedelegatingviewresolver.enabled:true");
		this.context.register(Config.class, WebMvcAutoConfiguration.class,
				HttpMessageConvertersAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class,
				DeviceDelegatingViewResolverConfiguration.class);
		this.context.refresh();
		InternalResourceViewResolver internalResourceViewResolver = this.context
				.getBean(InternalResourceViewResolver.class);
		AbstractDeviceDelegatingViewResolver deviceDelegatingViewResolver = this.context
				.getBean("deviceDelegatingViewResolver",
						AbstractDeviceDelegatingViewResolver.class);
		assertNotNull(internalResourceViewResolver);
		assertNotNull(deviceDelegatingViewResolver);
		assertTrue(deviceDelegatingViewResolver.getViewResolver() instanceof InternalResourceViewResolver);
		try {
			this.context.getBean(ThymeleafViewResolver.class);
		}
		catch (NoSuchBeanDefinitionException e) {
			// expected. ThymeleafViewResolver shouldn't be defined.
		}
		assertTrue(deviceDelegatingViewResolver.getOrder() == internalResourceViewResolver
				.getOrder() - 1);
	}

	@Test(expected = NoSuchBeanDefinitionException.class)
	public void deviceDelegatingInternalResourceViewResolverDisabled() throws Exception {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.mobile.devicedelegatingviewresolver.enabled:false");
		this.context.register(Config.class, WebMvcAutoConfiguration.class,
				HttpMessageConvertersAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class,
				DeviceDelegatingViewResolverConfiguration.class);
		this.context.refresh();
		assertNotNull(this.context.getBean(InternalResourceViewResolver.class));
		try {
			this.context.getBean(ThymeleafViewResolver.class);
		}
		catch (NoSuchBeanDefinitionException e) {
			// expected. ThymeleafViewResolver shouldn't be defined.
		}
		this.context.getBean("deviceDelegatingViewResolver",
				AbstractDeviceDelegatingViewResolver.class);
	}

	@Test
	public void deviceDelegatingThymeleafViewResolverEnabled() throws Exception {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.mobile.devicedelegatingviewresolver.enabled:true");
		this.context.register(Config.class, WebMvcAutoConfiguration.class,
				ThymeleafAutoConfiguration.class,
				HttpMessageConvertersAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class,
				DeviceDelegatingViewResolverConfiguration.class);
		this.context.refresh();
		ThymeleafViewResolver thymeleafViewResolver = this.context
				.getBean(ThymeleafViewResolver.class);
		AbstractDeviceDelegatingViewResolver deviceDelegatingViewResolver = this.context
				.getBean("deviceDelegatingViewResolver",
						AbstractDeviceDelegatingViewResolver.class);
		assertNotNull(thymeleafViewResolver);
		assertNotNull(deviceDelegatingViewResolver);
		assertTrue(deviceDelegatingViewResolver.getViewResolver() instanceof ThymeleafViewResolver);
		assertNotNull(this.context.getBean(InternalResourceViewResolver.class));
		assertNotNull(this.context.getBean(ThymeleafViewResolver.class));
		assertTrue(deviceDelegatingViewResolver.getOrder() == thymeleafViewResolver
				.getOrder() - 1);
	}

	@Test(expected = NoSuchBeanDefinitionException.class)
	public void deviceDelegatingThymeleafViewResolverDisabled() throws Exception {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.mobile.devicedelegatingviewresolver.enabled:false");
		this.context.register(Config.class, WebMvcAutoConfiguration.class,
				ThymeleafAutoConfiguration.class,
				HttpMessageConvertersAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class,
				DeviceDelegatingViewResolverConfiguration.class);
		this.context.refresh();
		assertNotNull(this.context.getBean(InternalResourceViewResolver.class));
		assertNotNull(this.context.getBean(ThymeleafViewResolver.class));
		this.context.getBean("deviceDelegatingViewResolver",
				AbstractDeviceDelegatingViewResolver.class);
	}

	@Test
	public void defaultPropertyValues() throws Exception {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.mobile.devicedelegatingviewresolver.enabled:true");
		this.context.register(Config.class, WebMvcAutoConfiguration.class,
				HttpMessageConvertersAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class,
				DeviceDelegatingViewResolverConfiguration.class);
		this.context.refresh();
		LiteDeviceDelegatingViewResolver liteDeviceDelegatingViewResolver = this.context
				.getBean("deviceDelegatingViewResolver",
						LiteDeviceDelegatingViewResolver.class);

		Field normalPrefixField = ReflectionUtils.findField(
				LiteDeviceDelegatingViewResolver.class, "normalPrefix");
		normalPrefixField.setAccessible(true);
		String normalPrefix = (String) ReflectionUtils.getField(normalPrefixField,
				liteDeviceDelegatingViewResolver);
		assertEquals("", normalPrefix);

		Field mobilePrefixField = ReflectionUtils.findField(
				LiteDeviceDelegatingViewResolver.class, "mobilePrefix");
		mobilePrefixField.setAccessible(true);
		String mobilePrefix = (String) ReflectionUtils.getField(mobilePrefixField,
				liteDeviceDelegatingViewResolver);
		assertEquals("mobile/", mobilePrefix);

		Field tabletPrefixField = ReflectionUtils.findField(
				LiteDeviceDelegatingViewResolver.class, "tabletPrefix");
		tabletPrefixField.setAccessible(true);
		String tabletPrefix = (String) ReflectionUtils.getField(tabletPrefixField,
				liteDeviceDelegatingViewResolver);
		assertEquals("tablet/", tabletPrefix);

		Field normalSuffixField = ReflectionUtils.findField(
				LiteDeviceDelegatingViewResolver.class, "normalSuffix");
		normalSuffixField.setAccessible(true);
		String normalSuffix = (String) ReflectionUtils.getField(normalSuffixField,
				liteDeviceDelegatingViewResolver);
		assertEquals("", normalSuffix);

		Field mobileSuffixField = ReflectionUtils.findField(
				LiteDeviceDelegatingViewResolver.class, "mobileSuffix");
		mobileSuffixField.setAccessible(true);
		String mobileSuffix = (String) ReflectionUtils.getField(mobileSuffixField,
				liteDeviceDelegatingViewResolver);
		assertEquals("", mobileSuffix);

		Field tabletSuffixField = ReflectionUtils.findField(
				LiteDeviceDelegatingViewResolver.class, "tabletSuffix");
		tabletSuffixField.setAccessible(true);
		String tabletSuffix = (String) ReflectionUtils.getField(tabletSuffixField,
				liteDeviceDelegatingViewResolver);
		assertEquals("", tabletSuffix);
	}

	@Test
	public void overrideNormalPrefix() throws Exception {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.mobile.devicedelegatingviewresolver.enabled:true",
				"spring.mobile.devicedelegatingviewresolver.normalPrefix:normal/");
		this.context.register(Config.class, WebMvcAutoConfiguration.class,
				HttpMessageConvertersAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class,
				DeviceDelegatingViewResolverConfiguration.class);
		this.context.refresh();
		LiteDeviceDelegatingViewResolver liteDeviceDelegatingViewResolver = this.context
				.getBean("deviceDelegatingViewResolver",
						LiteDeviceDelegatingViewResolver.class);
		Field normalPrefixField = ReflectionUtils.findField(
				LiteDeviceDelegatingViewResolver.class, "normalPrefix");
		normalPrefixField.setAccessible(true);
		String normalPrefix = (String) ReflectionUtils.getField(normalPrefixField,
				liteDeviceDelegatingViewResolver);
		assertEquals("normal/", normalPrefix);
	}

	@Test
	public void overrideMobilePrefix() throws Exception {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.mobile.devicedelegatingviewresolver.enabled:true",
				"spring.mobile.devicedelegatingviewresolver.mobilePrefix:mob/");
		this.context.register(Config.class, WebMvcAutoConfiguration.class,
				HttpMessageConvertersAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class,
				DeviceDelegatingViewResolverConfiguration.class);
		this.context.refresh();
		LiteDeviceDelegatingViewResolver liteDeviceDelegatingViewResolver = this.context
				.getBean("deviceDelegatingViewResolver",
						LiteDeviceDelegatingViewResolver.class);
		Field mobilePrefixField = ReflectionUtils.findField(
				LiteDeviceDelegatingViewResolver.class, "mobilePrefix");
		mobilePrefixField.setAccessible(true);
		String mobilePrefix = (String) ReflectionUtils.getField(mobilePrefixField,
				liteDeviceDelegatingViewResolver);
		assertEquals("mob/", mobilePrefix);
	}

	@Test
	public void overrideTabletPrefix() throws Exception {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.mobile.devicedelegatingviewresolver.enabled:true",
				"spring.mobile.devicedelegatingviewresolver.tabletPrefix:tab/");
		this.context.register(Config.class, WebMvcAutoConfiguration.class,
				HttpMessageConvertersAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class,
				DeviceDelegatingViewResolverConfiguration.class);
		this.context.refresh();
		LiteDeviceDelegatingViewResolver liteDeviceDelegatingViewResolver = this.context
				.getBean("deviceDelegatingViewResolver",
						LiteDeviceDelegatingViewResolver.class);
		Field tabletPrefixField = ReflectionUtils.findField(
				LiteDeviceDelegatingViewResolver.class, "tabletPrefix");
		tabletPrefixField.setAccessible(true);
		String tabletPrefix = (String) ReflectionUtils.getField(tabletPrefixField,
				liteDeviceDelegatingViewResolver);
		assertEquals("tab/", tabletPrefix);
	}

	@Test
	public void overrideNormalSuffix() throws Exception {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.mobile.devicedelegatingviewresolver.enabled:true",
				"spring.mobile.devicedelegatingviewresolver.normalSuffix:.nor");
		this.context.register(Config.class, WebMvcAutoConfiguration.class,
				HttpMessageConvertersAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class,
				DeviceDelegatingViewResolverConfiguration.class);
		this.context.refresh();
		LiteDeviceDelegatingViewResolver liteDeviceDelegatingViewResolver = this.context
				.getBean("deviceDelegatingViewResolver",
						LiteDeviceDelegatingViewResolver.class);
		Field normalSuffixField = ReflectionUtils.findField(
				LiteDeviceDelegatingViewResolver.class, "normalSuffix");
		normalSuffixField.setAccessible(true);
		String normalSuffix = (String) ReflectionUtils.getField(normalSuffixField,
				liteDeviceDelegatingViewResolver);
		assertEquals(".nor", normalSuffix);
	}

	@Test
	public void overrideMobileSuffix() throws Exception {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.mobile.devicedelegatingviewresolver.enabled:true",
				"spring.mobile.devicedelegatingviewresolver.mobileSuffix:.mob");
		this.context.register(Config.class, WebMvcAutoConfiguration.class,
				HttpMessageConvertersAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class,
				DeviceDelegatingViewResolverConfiguration.class);
		this.context.refresh();
		LiteDeviceDelegatingViewResolver liteDeviceDelegatingViewResolver = this.context
				.getBean("deviceDelegatingViewResolver",
						LiteDeviceDelegatingViewResolver.class);
		Field mobileSuffixField = ReflectionUtils.findField(
				LiteDeviceDelegatingViewResolver.class, "mobileSuffix");
		mobileSuffixField.setAccessible(true);
		String mobileSuffix = (String) ReflectionUtils.getField(mobileSuffixField,
				liteDeviceDelegatingViewResolver);
		assertEquals(".mob", mobileSuffix);
	}

	@Test
	public void overrideTabletSuffix() throws Exception {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.mobile.devicedelegatingviewresolver.enabled:true",
				"spring.mobile.devicedelegatingviewresolver.tabletSuffix:.tab");
		this.context.register(Config.class, WebMvcAutoConfiguration.class,
				HttpMessageConvertersAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class,
				DeviceDelegatingViewResolverConfiguration.class);
		this.context.refresh();
		LiteDeviceDelegatingViewResolver liteDeviceDelegatingViewResolver = this.context
				.getBean("deviceDelegatingViewResolver",
						LiteDeviceDelegatingViewResolver.class);
		Field tabletSuffixField = ReflectionUtils.findField(
				LiteDeviceDelegatingViewResolver.class, "tabletSuffix");
		tabletSuffixField.setAccessible(true);
		String tabletSuffix = (String) ReflectionUtils.getField(tabletSuffixField,
				liteDeviceDelegatingViewResolver);
		assertEquals(".tab", tabletSuffix);
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
