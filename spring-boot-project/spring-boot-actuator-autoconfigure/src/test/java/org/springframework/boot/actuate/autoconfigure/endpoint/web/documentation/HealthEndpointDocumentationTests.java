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

package org.springframework.boot.actuate.autoconfigure.endpoint.web.documentation;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.actuate.health.AdditionalHealthEndpointPath;
import org.springframework.boot.actuate.health.CompositeHealthContributor;
import org.springframework.boot.actuate.health.DefaultHealthContributorRegistry;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.boot.actuate.health.HealthContributorRegistry;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.HealthEndpointGroup;
import org.springframework.boot.actuate.health.HealthEndpointGroups;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.HttpCodeStatusMapper;
import org.springframework.boot.actuate.health.SimpleHttpCodeStatusMapper;
import org.springframework.boot.actuate.health.SimpleStatusAggregator;
import org.springframework.boot.actuate.health.StatusAggregator;
import org.springframework.boot.actuate.jdbc.DataSourceHealthIndicator;
import org.springframework.boot.actuate.system.DiskSpaceHealthIndicator;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
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
class HealthEndpointDocumentationTests extends MockMvcEndpointDocumentationTests {

	private static final List<FieldDescriptor> componentFields = Arrays.asList(
			fieldWithPath("status").description("Status of a specific part of the application"),
			subsectionWithPath("details").description("Details of the health of a specific part of the application."));

	@Test
	void health() throws Exception {
		FieldDescriptor status = fieldWithPath("status").description("Overall status of the application.");
		FieldDescriptor components = fieldWithPath("components").description("The components that make up the health.");
		FieldDescriptor componentStatus = fieldWithPath("components.*.status")
				.description("Status of a specific part of the application.");
		FieldDescriptor nestedComponents = subsectionWithPath("components.*.components")
				.description("The nested components that make up the health.").optional();
		FieldDescriptor componentDetails = subsectionWithPath("components.*.details")
				.description("Details of the health of a specific part of the application. "
						+ "Presence is controlled by `management.endpoint.health.show-details`.")
				.optional();
		this.mockMvc.perform(get("/actuator/health").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andDo(document("health",
						responseFields(status, components, componentStatus, nestedComponents, componentDetails)));
	}

	@Test
	void healthComponent() throws Exception {
		this.mockMvc.perform(get("/actuator/health/db").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andDo(document("health/component", responseFields(componentFields)));
	}

	@Test
	void healthComponentInstance() throws Exception {
		this.mockMvc.perform(get("/actuator/health/broker/us1").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andDo(document("health/instance", responseFields(componentFields)));
	}

	@Configuration(proxyBeanMethods = false)
	@Import(BaseDocumentationConfiguration.class)
	@ImportAutoConfiguration(DataSourceAutoConfiguration.class)
	static class TestConfiguration {

		@Bean
		HealthEndpoint healthEndpoint(Map<String, HealthContributor> healthContributors) {
			HealthContributorRegistry registry = new DefaultHealthContributorRegistry(healthContributors);
			HealthEndpointGroup primary = new TestHealthEndpointGroup();
			HealthEndpointGroups groups = HealthEndpointGroups.of(primary, Collections.emptyMap());
			return new HealthEndpoint(registry, groups);
		}

		@Bean
		DiskSpaceHealthIndicator diskSpaceHealthIndicator() {
			return new DiskSpaceHealthIndicator(new File("."), DataSize.ofMegabytes(10));
		}

		@Bean
		DataSourceHealthIndicator dbHealthIndicator(DataSource dataSource) {
			return new DataSourceHealthIndicator(dataSource);
		}

		@Bean
		CompositeHealthContributor brokerHealthContributor() {
			Map<String, HealthIndicator> indicators = new LinkedHashMap<>();
			indicators.put("us1", () -> Health.up().withDetail("version", "1.0.2").build());
			indicators.put("us2", () -> Health.up().withDetail("version", "1.0.4").build());
			return CompositeHealthContributor.fromMap(indicators);
		}

	}

	private static class TestHealthEndpointGroup implements HealthEndpointGroup {

		private final StatusAggregator statusAggregator = new SimpleStatusAggregator();

		private final HttpCodeStatusMapper httpCodeStatusMapper = new SimpleHttpCodeStatusMapper();

		@Override
		public boolean isMember(String name) {
			return true;
		}

		@Override
		public boolean showComponents(SecurityContext securityContext) {
			return true;
		}

		@Override
		public boolean showDetails(SecurityContext securityContext) {
			return true;
		}

		@Override
		public StatusAggregator getStatusAggregator() {
			return this.statusAggregator;
		}

		@Override
		public HttpCodeStatusMapper getHttpCodeStatusMapper() {
			return this.httpCodeStatusMapper;
		}

		@Override
		public AdditionalHealthEndpointPath getAdditionalPath() {
			return null;
		}

	}

}
