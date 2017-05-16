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

package org.springframework.boot.autoconfigure.conversion;

import java.util.Properties;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConversionServiceAutoConfiguration}.
 *
 * @author Mark Douglass
 * @author Rob Hill
 */
public class ConversionServiceAutoConfigurationTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private AnnotationConfigApplicationContext context;

	@After
	public void tearDown() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void createsDefaultConversionService() {
		this.context = new AnnotationConfigApplicationContext();
		Class<?>[] configs = {ConversionServiceAutoConfiguration.class};
		this.context.register(configs);
		ExampleConverter1 converter1 = new ExampleConverter1();
		this.context.getBeanFactory().registerSingleton("converter1", converter1);
		ExampleConverter2 converter2 = new ExampleConverter2();
		this.context.getBeanFactory().registerSingleton("converter2", converter2);
		this.context.refresh();

		ConversionService service = this.context.getBean(ConversionService.class);
		assertThat(service.canConvert(Properties.class, Integer.class)).isTrue();
		assertThat(service.canConvert(Integer.class, Properties.class)).isTrue();
	}

	@Test
	public void ignoresConversionServiceWithoutConverters() {
		this.context = new AnnotationConfigApplicationContext();
		Class<?>[] configs = {ConversionServiceAutoConfiguration.class};
		this.context.register(configs);
		this.context.refresh();

		this.thrown.expect(NoSuchBeanDefinitionException.class);
		this.context.getBean(ConversionService.class);
	}

	@Test
	public void wontReregisterConversionService() {
		this.context = new AnnotationConfigApplicationContext();
		Class<?>[] configs = {ConversionServiceAutoConfiguration.class};
		this.context.register(configs);
		ExampleConverter1 converter1 = new ExampleConverter1();
		this.context.getBeanFactory().registerSingleton("converter1", converter1);
		ExampleConverter2 converter2 = new ExampleConverter2();
		this.context.getBeanFactory().registerSingleton("converter2", converter2);

		this.context.getBeanFactory().registerSingleton("conversionService", new DefaultConversionService());
		this.context.refresh();

		ConversionService service = this.context.getBean(ConversionService.class);
		assertThat(service.canConvert(Properties.class, Integer.class)).isFalse();
		assertThat(service.canConvert(Integer.class, Properties.class)).isFalse();
	}

}
