/*
 * Copyright 2012-2015 the original author or authors.
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
import org.junit.Test;

import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.session.SessionAutoConfiguration;
import org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizerBeanPostProcessor;
import org.springframework.boot.context.embedded.MockEmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.MockEmbeddedServletContainerFactory.RegisteredFilter;
import org.springframework.boot.context.web.OrderedCharacterEncodingFilter;
import org.springframework.boot.context.web.OrderedRequestContextFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.session.web.http.SessionRepositoryFilter;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Integration tests that verify the ordering of various filters that are auto-configured.
 *
 * @author Andy Wilkinson
 */
public class FilterOrderingIntegrationTests {

	private AnnotationConfigEmbeddedWebApplicationContext context;

	@After
	public void cleanup() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testFilterOrdering() {
		load();
		List<RegisteredFilter> registeredFilters = this.context
				.getBean(MockEmbeddedServletContainerFactory.class).getContainer()
				.getRegisteredFilters();
		List<Filter> filters = new ArrayList<Filter>(registeredFilters.size());
		for (RegisteredFilter registeredFilter : registeredFilters) {
			filters.add(registeredFilter.getFilter());
		}
		assertThat(filters, contains(instanceOf(OrderedCharacterEncodingFilter.class),
				instanceOf(SessionRepositoryFilter.class), instanceOf(Filter.class),
				instanceOf(Filter.class), instanceOf(OrderedRequestContextFilter.class),
				instanceOf(FilterChainProxy.class)));
	}

	private void load() {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		this.context.register(MockEmbeddedServletContainerConfiguration.class,
				TestRedisConfiguration.class, WebMvcAutoConfiguration.class,
				SecurityAutoConfiguration.class, SessionAutoConfiguration.class,
				HttpMessageConvertersAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class,
				HttpEncodingAutoConfiguration.class);
		this.context.refresh();
	}

	@Configuration
	static class MockEmbeddedServletContainerConfiguration {

		@Bean
		public MockEmbeddedServletContainerFactory containerFactory() {
			return new MockEmbeddedServletContainerFactory();
		}

		@Bean
		public EmbeddedServletContainerCustomizerBeanPostProcessor embeddedServletContainerCustomizerBeanPostProcessor() {
			return new EmbeddedServletContainerCustomizerBeanPostProcessor();
		}

	}

	@Configuration
	static class TestRedisConfiguration {

		@Bean
		public RedisConnectionFactory redisConnectionFactory() {
			RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);
			RedisConnection connection = mock(RedisConnection.class);
			given(connectionFactory.getConnection()).willReturn(connection);
			return connectionFactory;
		}

	}

}
