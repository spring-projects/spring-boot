/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.web.servlet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServlet;
import org.junit.jupiter.api.Test;

import org.springframework.boot.test.autoconfigure.web.servlet.SpringBootMockMvcBuilderCustomizer.DeferredLinesWriter;
import org.springframework.boot.test.autoconfigure.web.servlet.SpringBootMockMvcBuilderCustomizer.LinesWriter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SpringBootMockMvcBuilderCustomizer}.
 *
 * @author Madhura Bhave
 */
class SpringBootMockMvcBuilderCustomizerTests {

	@Test
	@SuppressWarnings("unchecked")
	void customizeShouldAddFilters() {
		AnnotationConfigServletWebApplicationContext context = new AnnotationConfigServletWebApplicationContext();
		MockServletContext servletContext = new MockServletContext();
		context.setServletContext(servletContext);
		context.register(ServletConfiguration.class, FilterConfiguration.class);
		context.refresh();
		DefaultMockMvcBuilder builder = MockMvcBuilders.webAppContextSetup(context);
		SpringBootMockMvcBuilderCustomizer customizer = new SpringBootMockMvcBuilderCustomizer(context);
		customizer.customize(builder);
		FilterRegistrationBean<?> registrationBean = (FilterRegistrationBean<?>) context
			.getBean("filterRegistrationBean");
		Filter testFilter = (Filter) context.getBean("testFilter");
		Filter otherTestFilter = registrationBean.getFilter();
		List<Filter> filters = (List<Filter>) ReflectionTestUtils.getField(builder, "filters");
		assertThat(filters).containsExactlyInAnyOrder(testFilter, otherTestFilter);
	}

	@Test
	void whenCalledInParallelDeferredLinesWriterSeparatesOutputByThread() throws Exception {
		AnnotationConfigServletWebApplicationContext context = new AnnotationConfigServletWebApplicationContext();
		MockServletContext servletContext = new MockServletContext();
		context.setServletContext(servletContext);
		context.register(ServletConfiguration.class, FilterConfiguration.class);
		context.refresh();

		CapturingLinesWriter delegate = new CapturingLinesWriter();
		new DeferredLinesWriter(context, delegate);
		CountDownLatch latch = new CountDownLatch(10);
		for (int i = 0; i < 10; i++) {
			Thread thread = new Thread(() -> {
				for (int j = 0; j < 1000; j++) {
					DeferredLinesWriter writer = DeferredLinesWriter.get(context);
					writer.write(Arrays.asList("1", "2", "3", "4", "5"));
					writer.writeDeferredResult();
					writer.clear();
				}
				latch.countDown();
			});
			thread.start();
		}
		assertThat(latch.await(60, TimeUnit.SECONDS)).isTrue();

		assertThat(delegate.allWritten).hasSize(10000);
		assertThat(delegate.allWritten)
			.allSatisfy((written) -> assertThat(written).containsExactly("1", "2", "3", "4", "5"));
	}

	private static final class CapturingLinesWriter implements LinesWriter {

		List<List<String>> allWritten = new ArrayList<>();

		private final Object monitor = new Object();

		@Override
		public void write(List<String> lines) {
			List<String> written = new ArrayList<>(lines);
			synchronized (this.monitor) {
				this.allWritten.add(written);
			}
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ServletConfiguration {

		@Bean
		TestServlet testServlet() {
			return new TestServlet();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class FilterConfiguration {

		@Bean
		FilterRegistrationBean<OtherTestFilter> filterRegistrationBean() {
			return new FilterRegistrationBean<>(new OtherTestFilter());
		}

		@Bean
		TestFilter testFilter() {
			return new TestFilter();
		}

	}

	static class TestServlet extends HttpServlet {

	}

	static class TestFilter implements Filter {

		@Override
		public void init(FilterConfig filterConfig) {

		}

		@Override
		public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {

		}

		@Override
		public void destroy() {

		}

	}

	static class OtherTestFilter implements Filter {

		@Override
		public void init(FilterConfig filterConfig) {

		}

		@Override
		public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {

		}

		@Override
		public void destroy() {

		}

	}

}
