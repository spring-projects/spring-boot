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

package org.springframework.boot.actuate.autoconfigure.endpoint.web.documentation;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.Test;

import org.springframework.boot.actuate.health.CompositeHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.HealthIndicatorRegistryFactory;
import org.springframework.boot.actuate.health.OrderedHealthAggregator;
import org.springframework.boot.actuate.jdbc.DataSourceHealthIndicator;
import org.springframework.boot.actuate.system.DiskSpaceHealthIndicator;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.util.unit.DataSize;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for generating documentation describing the {@link HealthEndpoint}.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 */
public class HealthEndpointDocumentationTests extends MockMvcEndpointDocumentationTests {

	private static final List<FieldDescriptor> componentFields = Arrays.asList(
			fieldWithPath("status")
					.description("Status of a specific part of the application"),
			subsectionWithPath("details").description(
					"Details of the health of a specific part of the" + " application."));

	@Test
	public void health() throws Exception {
		this.mockMvc.perform(get("/actuator/health")).andExpect(status().isOk())
				.andDo(document("health", responseFields(
						fieldWithPath("status")
								.description("Overall status of the application."),
						fieldWithPath("details").description(
								"Details of the health of the application. Presence is controlled by "
										+ "`management.endpoint.health.show-details`)."),
						fieldWithPath("details.*.status").description(
								"Status of a specific part of the application."),
						subsectionWithPath("details.*.details").description(
								"Details of the health of a specific part of the"
										+ " application."))));
	}

	@Test
	public void healthComponent() throws Exception {
		this.mockMvc.perform(get("/actuator/health/db")).andExpect(status().isOk())
				.andDo(document("health/component", responseFields(componentFields)));
	}

	@Test
	public void healthComponentInstance() throws Exception {
		this.mockMvc.perform(get("/actuator/health/broker/us1"))
				.andExpect(status().isOk())
				.andDo(document("health/instance", responseFields(componentFields)));
	}

	@Configuration(proxyBeanMethods = false)
	@Import(BaseDocumentationConfiguration.class)
	@ImportAutoConfiguration(DataSourceAutoConfiguration.class)
	static class TestConfiguration {

		@Bean
		public HealthEndpoint endpoint(Map<String, HealthIndicator> healthIndicators) {
			return new HealthEndpoint(new CompositeHealthIndicator(
					new OrderedHealthAggregator(), new HealthIndicatorRegistryFactory()
							.createHealthIndicatorRegistry(healthIndicators)));
		}

		@Bean
		public DiskSpaceHealthIndicator diskSpaceHealthIndicator() {
			return new DiskSpaceHealthIndicator(new File("."), DataSize.ofMegabytes(10));
		}

		@Bean
		public DataSourceHealthIndicator dbHealthIndicator(DataSource dataSource) {
			return new DataSourceHealthIndicator(dataSource);
		}

		@Bean
		public CompositeHealthIndicator brokerHealthIndicator() {
			Map<String, HealthIndicator> indicators = new LinkedHashMap<>();
			indicators.put("us1",
					() -> Health.up().withDetail("version", "1.0.2").build());
			indicators.put("us2",
					() -> Health.up().withDetail("version", "1.0.4").build());
			return new CompositeHealthIndicator(new OrderedHealthAggregator(),
					indicators);
		}

	}

}
