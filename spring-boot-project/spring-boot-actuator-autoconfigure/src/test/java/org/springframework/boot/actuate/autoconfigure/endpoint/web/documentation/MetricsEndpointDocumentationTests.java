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

import io.micrometer.core.instrument.Statistic;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.metrics.MetricsEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.requestParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for generating documentation describing the {@link MetricsEndpoint}.
 *
 * @author Andy Wilkinson
 */
class MetricsEndpointDocumentationTests extends MockMvcEndpointDocumentationTests {

	@Test
	void metricNames() throws Exception {
		this.mockMvc.perform(get("/actuator/metrics")).andExpect(status().isOk()).andDo(document("metrics/names",
				responseFields(fieldWithPath("names").description("Names of the known metrics."))));
	}

	@Test
	void metric() throws Exception {
		this.mockMvc.perform(get("/actuator/metrics/jvm.memory.max")).andExpect(status().isOk())
				.andDo(document("metrics/metric",
						responseFields(fieldWithPath("name").description("Name of the metric"),
								fieldWithPath("description").description("Description of the metric"),
								fieldWithPath("baseUnit").description("Base unit of the metric"),
								fieldWithPath("measurements").description("Measurements of the metric"),
								fieldWithPath("measurements[].statistic").description(
										"Statistic of the measurement. (" + describeEnumValues(Statistic.class) + ")."),
								fieldWithPath("measurements[].value").description("Value of the measurement."),
								fieldWithPath("availableTags").description("Tags that are available for drill-down."),
								fieldWithPath("availableTags[].tag").description("Name of the tag."),
								fieldWithPath("availableTags[].values").description("Possible values of the tag."))));
	}

	@Test
	void metricWithTags() throws Exception {
		this.mockMvc
				.perform(get("/actuator/metrics/jvm.memory.max").param("tag", "area:nonheap").param("tag",
						"id:Compressed Class Space"))
				.andExpect(status().isOk())
				.andDo(document("metrics/metric-with-tags", requestParameters(parameterWithName("tag")
						.description("A tag to use for drill-down in the form `name:value`."))));
	}

	@Configuration(proxyBeanMethods = false)
	@Import(BaseDocumentationConfiguration.class)
	static class TestConfiguration {

		@Bean
		public MetricsEndpoint endpoint() {
			SimpleMeterRegistry registry = new SimpleMeterRegistry();
			new JvmMemoryMetrics().bindTo(registry);
			return new MetricsEndpoint(registry);
		}

	}

}
