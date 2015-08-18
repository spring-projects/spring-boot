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

import java.util.ArrayList;
import java.util.List;

import javax.servlet.Filter;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.filter.HiddenHttpMethodFilter;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link HttpEncodingAutoConfiguration}
 *
 * @author Stephane Nicoll
 */
public class HttpEncodingAutoConfigurationTests {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	private AnnotationConfigApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void defaultConfiguration() {
		load(EmptyConfiguration.class);
		CharacterEncodingFilter filter = this.context
				.getBean(CharacterEncodingFilter.class);
		assertCharacterEncodingFilter(filter, "UTF-8", true);
	}

	@Test
	public void disableConfiguration() {
		load(EmptyConfiguration.class, "spring.http.encoding.enabled:false");
		this.thrown.expect(NoSuchBeanDefinitionException.class);
		this.context.getBean(CharacterEncodingFilter.class);
	}

	@Test
	public void customConfiguration() {
		load(EmptyConfiguration.class, "spring.http.encoding.charset:ISO-8859-15",
				"spring.http.encoding.force:false");
		CharacterEncodingFilter filter = this.context
				.getBean(CharacterEncodingFilter.class);
		assertCharacterEncodingFilter(filter, "ISO-8859-15", false);
	}

	@Test
	public void customFilterConfiguration() {
		load(FilterConfiguration.class, "spring.http.encoding.charset:ISO-8859-15",
				"spring.http.encoding.force:false");
		CharacterEncodingFilter filter = this.context
				.getBean(CharacterEncodingFilter.class);
		assertCharacterEncodingFilter(filter, "US-ASCII", false);
	}

	@Test
	public void filterIsOrderedHighest() throws Exception {
		load(OrderedConfiguration.class);
		List<Filter> beans = new ArrayList<Filter>(this.context.getBeansOfType(
				Filter.class).values());
		AnnotationAwareOrderComparator.sort(beans);
		assertThat(beans.get(0), instanceOf(CharacterEncodingFilter.class));
		assertThat(beans.get(1), instanceOf(HiddenHttpMethodFilter.class));
	}

	private void assertCharacterEncodingFilter(CharacterEncodingFilter actual,
			String encoding, boolean forceEncoding) {
		DirectFieldAccessor accessor = new DirectFieldAccessor(actual);
		assertEquals("Wrong encoding", encoding, accessor.getPropertyValue("encoding"));
		assertEquals("Wrong forceEncoding flag", forceEncoding,
				accessor.getPropertyValue("forceEncoding"));
	}

	private void load(Class<?> config, String... environment) {
		this.context = doLoad(new Class<?>[] { config }, environment);
	}

	private AnnotationConfigApplicationContext doLoad(Class<?>[] configs,
			String... environment) {
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(applicationContext, environment);
		applicationContext.register(configs);
		applicationContext.register(HttpEncodingAutoConfiguration.class);
		applicationContext.refresh();
		return applicationContext;
	}

	@Configuration
	static class EmptyConfiguration {

	}

	@Configuration
	static class FilterConfiguration {

		@Bean
		public CharacterEncodingFilter myCharacterEncodingFilter() {
			CharacterEncodingFilter filter = new CharacterEncodingFilter();
			filter.setEncoding("US-ASCII");
			filter.setForceEncoding(false);
			return filter;
		}

	}

	@Configuration
	static class OrderedConfiguration {

		@Bean
		public HiddenHttpMethodFilter hiddenHttpMethodFilter() {
			return new HiddenHttpMethodFilter();
		}

	}

}
