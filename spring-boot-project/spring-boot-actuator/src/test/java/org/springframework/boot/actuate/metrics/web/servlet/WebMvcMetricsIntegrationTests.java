/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.metrics.web.servlet;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.ServletContext;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MockClock;
import io.micrometer.core.instrument.Statistic;
import io.micrometer.core.instrument.simple.SimpleConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.filter.OrderedCharacterEncodingFilter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for {@link WebMvcMetrics}.
 *
 * @author Jon Schneider
 */
@RunWith(SpringRunner.class)
@WebAppConfiguration
public class WebMvcMetricsIntegrationTests {

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private SimpleMeterRegistry registry;

	private MockMvc mvc;

	@Autowired
	private Filter[] filters;

	@Before
	public void setupMockMvc() {
		this.mvc = MockMvcBuilders.webAppContextSetup(this.context)
				.addFilters(this.filters).build();
	}

	@Test
	public void handledExceptionIsRecordedInMetricTag() throws Exception {
		this.mvc.perform(get("/api/handledError")).andExpect(status().is5xxServerError());
		assertThat(this.registry.find("http.server.requests")
				.tags("exception", "Exception1", "status", "500")
				.value(Statistic.Count, 1.0).timer()).isPresent();
	}

	@Test
	public void rethrownExceptionIsRecordedInMetricTag() {
		assertThatCode(() -> this.mvc.perform(get("/api/rethrownError"))
				.andExpect(status().is5xxServerError()));
		assertThat(this.registry.find("http.server.requests")
				.tags("exception", "Exception2", "status", "500")
				.value(Statistic.Count, 1.0).timer()).isPresent();
	}

	@Test
	public void characterEncodingFilterMustHaveHigherPriorityThanMetricsFilter()
			throws Exception {
		final String utf8Param = "Vedran Pavić";
		this.mvc.perform(
				servletContext -> new ParamEncodingHttpServletRequest(servletContext,
						HttpMethod.POST.name(), "/api/pv/post")
								.contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
								.param("name", utf8Param))
				.andExpect(content().string(utf8Param));
	}

	@Configuration
	@EnableWebMvc
	static class TestConfiguration {

		@Bean
		MockClock clock() {
			return new MockClock();
		}

		@Bean
		MeterRegistry meterRegistry(Clock clock) {
			return new SimpleMeterRegistry(SimpleConfig.DEFAULT, clock);
		}

		@Bean
		public WebMvcMetrics controllerMetrics(MeterRegistry registry) {
			return new WebMvcMetrics(registry, new DefaultWebMvcTagsProvider(),
					"http.server.requests", true, false);
		}

		@Bean
		public WebMvcMetricsFilter webMetricsFilter(ApplicationContext context) {
			return new WebMvcMetricsFilter(context);
		}

		@Bean
		public CharacterEncodingFilter characterEncodingFilter() {
			final OrderedCharacterEncodingFilter encodingFilter = new OrderedCharacterEncodingFilter();
			encodingFilter.setEncoding("UTF-8");
			encodingFilter.setForceRequestEncoding(true);
			encodingFilter.setForceResponseEncoding(true);
			return encodingFilter;
		}

		@RestController
		@RequestMapping("/api")
		@Timed
		static class Controller1 {

			@Bean
			public CustomExceptionHandler controllerAdvice() {
				return new CustomExceptionHandler();
			}

			@GetMapping("/handledError")
			public String handledError() {
				throw new Exception1();
			}

			@GetMapping("/rethrownError")
			public String rethrownError() {
				throw new Exception2();
			}

			@GetMapping(params = { "first", "second" })
			public String getWithParams() {
				return "getWithParams";
			}

			@PostMapping(path = "/{pv}/post", produces = "text/plain;charset=UTF-8")
			public String handlePost(@PathVariable("pv") String pv,
					@RequestParam("name") String name) {
				return name;
			}

		}

	}

	static class Exception1 extends RuntimeException {

	}

	static class Exception2 extends RuntimeException {

	}

	@ControllerAdvice
	static class CustomExceptionHandler {

		@Autowired
		WebMvcMetrics metrics;

		@ExceptionHandler
		ResponseEntity<String> handleError(Exception1 ex) {
			this.metrics.tagWithException(ex);
			return new ResponseEntity<>("this is a custom exception body",
					HttpStatus.INTERNAL_SERVER_ERROR);
		}

		@ExceptionHandler
		ResponseEntity<String> rethrowError(Exception2 ex) {
			throw ex;
		}

	}

	private static class ParamEncodingHttpServletRequest extends MockHttpServletRequest {

		private Map<String, String[]> parameterMap;

		ParamEncodingHttpServletRequest(ServletContext servletContext, String method,
				String requestUri) {
			super(servletContext, method, requestUri);
		}

		ParamEncodingHttpServletRequest param(String name, String value) {
			setParameter(name, value);
			return this;
		}

		ParamEncodingHttpServletRequest contentType(String contentType) {
			setContentType(contentType);
			return this;
		}

		@Override
		public String getCharacterEncoding() {
			return StringUtils.defaultIfBlank(super.getCharacterEncoding(), "ISO-8859-1");
		}

		@Override
		public Map<String, String[]> getParameterMap() {
			if (this.parameterMap == null) {
				initParamMap();
			}
			return this.parameterMap;
		}

		private void initParamMap() {
			this.parameterMap = new HashMap<>(super.getParameterMap());
			for (Map.Entry<String, String[]> entry : this.parameterMap.entrySet()) {
				this.parameterMap.put(entry.getKey(), encodeParamValues(entry.getValue()));
			}
		}

		@Override
		public String[] getParameterValues(String name) {
			if (this.parameterMap == null) {
				initParamMap();
			}
			return this.parameterMap.get(name);
		}

		private String[] encodeParamValues(String[] values) {
			final String[] encodedValues = new String[values.length];
			for (int i = 0; i < values.length; i++) {
				encodedValues[i] = new String(values[i].getBytes(),
						Charset.forName(getCharacterEncoding()));
			}
			return encodedValues;
		}
	}

}
