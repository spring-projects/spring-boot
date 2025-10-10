/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.context;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.context.filtersample.ExampleComponent;
import org.springframework.boot.context.filtersample.ExampleFilteredComponent;
import org.springframework.boot.context.filtersample.SampleTypeExcludeFilter;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link TypeExcludeFilter}.
 *
 * @author Phillip Webb
 */
class TypeExcludeFilterTests {

	private @Nullable AnnotationConfigApplicationContext context;

	@AfterEach
	void cleanUp() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	void loadsTypeExcludeFilters() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.getBeanFactory().registerSingleton("filter1", new WithoutMatchOverrideFilter());
		this.context.getBeanFactory().registerSingleton("filter2", new SampleTypeExcludeFilter());
		this.context.register(Config.class);
		this.context.refresh();
		assertThat(this.context.getBean(ExampleComponent.class)).isNotNull();
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(() -> {
			assertThat(this.context).isNotNull();
			this.context.getBean(ExampleFilteredComponent.class);
		});
	}

	@Configuration(proxyBeanMethods = false)
	@ComponentScan(basePackageClasses = SampleTypeExcludeFilter.class,
			excludeFilters = @Filter(type = FilterType.CUSTOM, classes = SampleTypeExcludeFilter.class))
	static class Config {

	}

	static class WithoutMatchOverrideFilter extends TypeExcludeFilter {

	}

}
