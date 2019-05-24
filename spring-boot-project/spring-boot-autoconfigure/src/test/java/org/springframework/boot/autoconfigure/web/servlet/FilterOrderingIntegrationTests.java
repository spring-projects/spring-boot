/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.autoconfigure.web.servlet;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.Filter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.session.SessionAutoConfiguration;
import org.springframework.boot.testsupport.web.servlet.MockServletWebServer.RegisteredFilter;
import org.springframework.boot.web.server.WebServerFactoryCustomizerBeanPostProcessor;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.boot.web.servlet.filter.OrderedCharacterEncodingFilter;
import org.springframework.boot.web.servlet.filter.OrderedRequestContextFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.session.MapSessionRepository;
import org.springframework.session.config.annotation.web.http.EnableSpringHttpSession;
import org.springframework.session.web.http.SessionRepositoryFilter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Integration tests that verify the ordering of various filters that are auto-configured.
 *
 * @author Andy Wilkinson
 * @author Eddú Meléndez
 */
class FilterOrderingIntegrationTests {

	private AnnotationConfigServletWebServerApplicationContext context;

	@AfterEach
	public void cleanup() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	void testFilterOrdering() {
		load();
		List<RegisteredFilter> registeredFilters = this.context.getBean(MockServletWebServerFactory.class)
				.getWebServer().getRegisteredFilters();
		List<Filter> filters = new ArrayList<>(registeredFilters.size());
		for (RegisteredFilter registeredFilter : registeredFilters) {
			filters.add(registeredFilter.getFilter());
		}
		Iterator<Filter> iterator = filters.iterator();
		assertThat(iterator.next()).isInstanceOf(OrderedCharacterEncodingFilter.class);
		assertThat(iterator.next()).isInstanceOf(SessionRepositoryFilter.class);
		assertThat(iterator.next()).isInstanceOf(Filter.class);
		assertThat(iterator.next()).isInstanceOf(Filter.class);
		assertThat(iterator.next()).isInstanceOf(OrderedRequestContextFilter.class);
		assertThat(iterator.next()).isInstanceOf(FilterChainProxy.class);
	}

	private void load() {
		this.context = new AnnotationConfigServletWebServerApplicationContext();
		this.context.register(MockWebServerConfiguration.class, TestSessionConfiguration.class,
				TestRedisConfiguration.class, WebMvcAutoConfiguration.class, SecurityAutoConfiguration.class,
				SessionAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class, HttpEncodingAutoConfiguration.class);
		this.context.refresh();
	}

	@Configuration(proxyBeanMethods = false)
	static class MockWebServerConfiguration {

		@Bean
		public MockServletWebServerFactory webServerFactory() {
			return new MockServletWebServerFactory();
		}

		@Bean
		public WebServerFactoryCustomizerBeanPostProcessor ServletWebServerCustomizerBeanPostProcessor() {
			return new WebServerFactoryCustomizerBeanPostProcessor();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableSpringHttpSession
	static class TestSessionConfiguration {

		@Bean
		public MapSessionRepository mapSessionRepository() {
			return new MapSessionRepository(new ConcurrentHashMap<>());
		}

	}

	@Configuration(proxyBeanMethods = false)
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
