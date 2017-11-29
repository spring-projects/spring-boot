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

package org.springframework.boot.actuate.autoconfigure.endpoint.web.documentation;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.actuate.autoconfigure.condition.ConditionsReportEndpoint;
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for generating documentation describing {@link ConditionsReportEndpoint}.
 *
 * @author Andy Wilkinson
 */
public class ConditionsReportEndpointDocumentationTests
		extends AbstractEndpointDocumentationTests {

	@Rule
	public final JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation();

	private MockMvc mockMvc;

	@Autowired
	private WebApplicationContext applicationContext;

	@Override
	@Before
	public void before() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(this.applicationContext)
				.apply(MockMvcRestDocumentation
						.documentationConfiguration(this.restDocumentation).uris())
				.build();
	}

	@Test
	public void conditions() throws Exception {
		this.mockMvc.perform(get("/actuator/conditions")).andExpect(status().isOk())
				.andDo(MockMvcRestDocumentation.document("conditions",
						preprocessResponse(limit("positiveMatches"),
								limit("negativeMatches")),
						responseFields(
								fieldWithPath("positiveMatches").description(
										"Classes and methods with conditions that were matched."),
								fieldWithPath("positiveMatches.*.[].condition")
										.description("Name of the condition."),
								fieldWithPath("positiveMatches.*.[].message").description(
										"Details of why the condition was matched."),
								fieldWithPath("negativeMatches").description(
										"Classes and methods with conditions that were not matched."),
								fieldWithPath("negativeMatches.*.notMatched")
										.description("Conditions that were matched."),
								fieldWithPath("negativeMatches.*.notMatched.[].condition")
										.description("Name of the condition."),
								fieldWithPath("negativeMatches.*.notMatched.[].message")
										.description(
												"Details of why the condition was not matched."),
								fieldWithPath("negativeMatches.*.matched")
										.description("Conditions that were matched."),
								fieldWithPath("negativeMatches.*.matched.[].condition")
										.description("Name of the condition.")
										.type(JsonFieldType.STRING).optional(),
								fieldWithPath("negativeMatches.*.matched.[].message")
										.description(
												"Details of why the condition was matched.")
										.type(JsonFieldType.STRING).optional(),
								fieldWithPath("unconditionalClasses").description(
										"Names of unconditional auto-configuration classes, if any."))));
	}

	@Configuration
	@Import(BaseDocumentationConfiguration.class)
	static class TestConfiguration {

		@Bean
		public ConditionsReportEndpoint autoConfigurationReportEndpoint(
				ConfigurableListableBeanFactory beanFactory) {
			ConditionEvaluationReport conditionEvaluationReport = ConditionEvaluationReport
					.get(beanFactory);
			conditionEvaluationReport.recordEvaluationCandidates(
					Arrays.asList(PropertyPlaceholderAutoConfiguration.class.getName()));
			return new ConditionsReportEndpoint(conditionEvaluationReport);
		}

	}

}
