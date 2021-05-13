/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.actuate.metrics.web.servlet;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MockClock;
import io.micrometer.core.instrument.simple.SimpleConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.metrics.AutoTimer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.util.NestedServletException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for {@link WebMvcMetricsFilter} in the presence of a custom exception handler.
 *
 * @author Jon Schneider
 */
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@TestPropertySource(properties = "security.ignored=/**")
class WebMvcMetricsIntegrationTests {

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private SimpleMeterRegistry registry;

	@Autowired
	private WebMvcMetricsFilter filter;

	private MockMvc mvc;

	@BeforeEach
	void setupMockMvc() {
		this.mvc = MockMvcBuilders.webAppContextSetup(this.context).addFilters(this.filter).build();
	}

	@Test
	void handledExceptionIsRecordedInMetricTag() throws Exception {
		this.mvc.perform(get("/api/handledError")).andExpect(status().is5xxServerError());
		assertThat(this.registry.get("http.server.requests").tags("exception", "Exception1", "status", "500").timer()
				.count()).isEqualTo(1L);
	}

	@Test
	void rethrownExceptionIsRecordedInMetricTag() {
		assertThatExceptionOfType(NestedServletException.class)
				.isThrownBy(() -> this.mvc.perform(get("/api/rethrownError")).andReturn());
		assertThat(this.registry.get("http.server.requests").tags("exception", "Exception2", "status", "500").timer()
				.count()).isEqualTo(1L);
	}

	@Configuration(proxyBeanMethods = false)
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
		WebMvcMetricsFilter webMetricsFilter(MeterRegistry registry, WebApplicationContext ctx) {
			return new WebMvcMetricsFilter(registry, new DefaultWebMvcTagsProvider(), "http.server.requests",
					AutoTimer.ENABLED);
		}

		@Configuration(proxyBeanMethods = false)
		@RestController
		@RequestMapping("/api")
		@Timed
		static class Controller1 {

			@Bean
			CustomExceptionHandler controllerAdvice() {
				return new CustomExceptionHandler();
			}

			@GetMapping("/handledError")
			String handledError() {
				throw new Exception1();
			}

			@GetMapping("/rethrownError")
			String rethrownError() {
				throw new Exception2();
			}

		}

	}

	static class Exception1 extends RuntimeException {

	}

	static class Exception2 extends RuntimeException {

	}

	@ControllerAdvice
	static class CustomExceptionHandler {

		@ExceptionHandler
		ResponseEntity<String> handleError(Exception1 ex) {
			return new ResponseEntity<>("this is a custom exception body", HttpStatus.INTERNAL_SERVER_ERROR);
		}

		@ExceptionHandler
		ResponseEntity<String> rethrowError(Exception2 ex) {
			throw ex;
		}

	}

}
