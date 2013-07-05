/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.bootstrap.actuate.autoconfigure;

import javax.servlet.Filter;
import javax.servlet.FilterChain;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.bootstrap.actuate.metrics.CounterService;
import org.springframework.bootstrap.actuate.metrics.GaugeService;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link MetricFilterAutoConfiguration}.
 * 
 * @author Phillip Webb
 */
public class MetricFilterAutoConfigurationTests {

	@Test
	public void recordsHttpInteractions() throws Exception {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				Config.class, MetricFilterAutoConfiguration.class);
		Filter filter = context.getBean(Filter.class);
		final MockHttpServletRequest request = new MockHttpServletRequest("GET",
				"/test/path");
		final MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain chain = mock(FilterChain.class);
		willAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				response.setStatus(200);
				return null;
			}
		}).given(chain).doFilter(request, response);
		filter.doFilter(request, response, chain);
		verify(context.getBean(CounterService.class)).increment("status.200.test.path");
		verify(context.getBean(GaugeService.class)).set(eq("response.test.path"),
				anyDouble());
		context.close();
	}

	@Test
	public void skipsFilterIfMissingServices() throws Exception {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				MetricFilterAutoConfiguration.class);
		assertThat(context.getBeansOfType(Filter.class).size(), equalTo(0));
		context.close();
	}

	@Configuration
	public static class Config {

		@Bean
		public CounterService counterService() {
			return mock(CounterService.class);
		}

		@Bean
		public GaugeService gaugeService() {
			return mock(GaugeService.class);
		}
	}

}
