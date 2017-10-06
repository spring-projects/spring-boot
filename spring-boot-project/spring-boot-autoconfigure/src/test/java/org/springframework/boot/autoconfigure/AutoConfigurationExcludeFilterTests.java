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

package org.springframework.boot.autoconfigure;

import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.context.filtersample.ExampleConfiguration;
import org.springframework.boot.autoconfigure.context.filtersample.ExampleFilteredAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AutoConfigurationExcludeFilter}.
 *
 * @author Stephane Nicoll
 */
public class AutoConfigurationExcludeFilterTests {

	private static final Class<?> FILTERED = ExampleFilteredAutoConfiguration.class;

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private AnnotationConfigApplicationContext context;

	@After
	public void cleanUp() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void filterExcludeAutoConfiguration() {
		this.context = new AnnotationConfigApplicationContext(Config.class);
		assertThat(this.context.getBeansOfType(String.class)).hasSize(1);
		assertThat(this.context.getBean(String.class)).isEqualTo("test");
		this.thrown.expect(NoSuchBeanDefinitionException.class);
		this.context.getBean(FILTERED);
	}

	@Configuration
	@ComponentScan(basePackageClasses = ExampleConfiguration.class, excludeFilters = @ComponentScan.Filter(type = FilterType.CUSTOM, classes = TestAutoConfigurationExcludeFilter.class))
	static class Config {

	}

	static class TestAutoConfigurationExcludeFilter
			extends AutoConfigurationExcludeFilter {

		@Override
		protected List<String> getAutoConfigurations() {
			return Collections.singletonList(FILTERED.getName());
		}

	}

}
