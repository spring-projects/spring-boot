/*
 * Copyright 2012-2016 the original author or authors.
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
import org.thymeleaf.spring4.view.ThymeleafViewResolver;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.PropertyAccessor;
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
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mobile.device.view.AbstractDeviceDelegatingViewResolver;
import org.springframework.mobile.device.view.LiteDeviceDelegatingViewResolver;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DeviceDelegatingViewResolverAutoConfiguration}.
 *
 * @author Roy Clarkson
 * @author Stephane Nicoll
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
		assertThat(internalResourceViewResolver).isNotNull();
		assertThat(deviceDelegatingViewResolver).isNotNull();
		assertThat(deviceDelegatingViewResolver.getViewResolver())
				.isInstanceOf(InternalResourceViewResolver.class);
		try {
			this.context.getBean(ThymeleafViewResolver.class);
		}
		catch (NoSuchBeanDefinitionException ex) {
			// expected. ThymeleafViewResolver shouldn't be defined.
		}
		assertThat(deviceDelegatingViewResolver.getOrder())
				.isEqualTo(internalResourceViewResolver.getOrder() - 1);
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
		assertThat(this.context.getBean(InternalResourceViewResolver.class)).isNotNull();
		try {
			this.context.getBean(ThymeleafViewResolver.class);
		}
		catch (NoSuchBeanDefinitionException ex) {
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
		assertThat(thymeleafViewResolver).isNotNull();
		assertThat(deviceDelegatingViewResolver).isNotNull();
		assertThat(deviceDelegatingViewResolver.getViewResolver())
				.isInstanceOf(ThymeleafViewResolver.class);
		assertThat(this.context.getBean(InternalResourceViewResolver.class)).isNotNull();
		assertThat(this.context.getBean(ThymeleafViewResolver.class)).isNotNull();
		assertThat(deviceDelegatingViewResolver.getOrder())
				.isEqualTo(thymeleafViewResolver.getOrder() - 1);
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
		assertThat(this.context.getBean(InternalResourceViewResolver.class)).isNotNull();
		assertThat(this.context.getBean(ThymeleafViewResolver.class)).isNotNull();
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
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context, configuration);
		this.context.register(Config.class, WebMvcAutoConfiguration.class,
				HttpMessageConvertersAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class,
				DeviceDelegatingViewResolverConfiguration.class);
		this.context.refresh();
		LiteDeviceDelegatingViewResolver liteDeviceDelegatingViewResolver = this.context
				.getBean("deviceDelegatingViewResolver",
						LiteDeviceDelegatingViewResolver.class);
		return new DirectFieldAccessor(liteDeviceDelegatingViewResolver);
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
