/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.trace;

import java.util.Map;

import org.junit.After;
import org.junit.Test;

import org.springframework.boot.actuate.trace.TraceRepository;
import org.springframework.boot.actuate.trace.WebRequestTraceFilter;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TraceWebFilterAutoConfiguration}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
public class TraceWebFilterAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void configureFilter() {
		load();
		assertThat(this.context.getBean(WebRequestTraceFilter.class)).isNotNull();
	}

	@Test
	public void overrideTraceFilter() throws Exception {
		load(CustomTraceFilterConfig.class);
		WebRequestTraceFilter filter = this.context.getBean(WebRequestTraceFilter.class);
		assertThat(filter).isInstanceOf(TestWebRequestTraceFilter.class);
	}

	@Test
	public void skipsFilterIfPropertyDisabled() throws Exception {
		load("management.trace.filter.enabled:false");
		assertThat(this.context.getBeansOfType(WebRequestTraceFilter.class).size())
				.isEqualTo(0);
	}

	private void load(String... environment) {
		load(null, environment);
	}

	private void load(Class<?> config, String... environment) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of(environment).applyTo(context);
		if (config != null) {
			context.register(config);
		}
		context.register(PropertyPlaceholderAutoConfiguration.class,
				TraceRepositoryAutoConfiguration.class,
				TraceWebFilterAutoConfiguration.class);
		context.refresh();
		this.context = context;
	}

	@Configuration
	static class CustomTraceFilterConfig {

		@Bean
		public TestWebRequestTraceFilter testWebRequestTraceFilter(
				TraceRepository repository, TraceEndpointProperties properties) {
			return new TestWebRequestTraceFilter(repository, properties);
		}

	}

	static class TestWebRequestTraceFilter extends WebRequestTraceFilter {

		TestWebRequestTraceFilter(TraceRepository repository,
				TraceEndpointProperties properties) {
			super(repository, properties.getInclude());
		}

		@Override
		protected void postProcessRequestHeaders(Map<String, Object> headers) {
			headers.clear();
		}

	}

}
