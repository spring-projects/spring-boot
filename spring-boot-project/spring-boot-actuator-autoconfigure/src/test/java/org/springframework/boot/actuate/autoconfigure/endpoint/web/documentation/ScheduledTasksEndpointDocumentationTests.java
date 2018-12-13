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

package org.springframework.boot.actuate.autoconfigure.endpoint.web.documentation;

import java.util.Collection;
import java.util.regex.Pattern;

import org.junit.Test;

import org.springframework.boot.actuate.scheduling.ScheduledTasksEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.config.ScheduledTaskHolder;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.replacePattern;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for generating documentation describing the {@link ScheduledTasksEndpoint}.
 *
 * @author Andy Wilkinson
 */
public class ScheduledTasksEndpointDocumentationTests
		extends MockMvcEndpointDocumentationTests {

	@Test
	public void scheduledTasks() throws Exception {
		this.mockMvc.perform(get("/actuator/scheduledtasks")).andExpect(status().isOk())
				.andDo(document("scheduled-tasks",
						preprocessResponse(replacePattern(Pattern.compile(
								"org.*\\.ScheduledTasksEndpointDocumentationTests\\$"
										+ "TestConfiguration"),
								"com.example.Processor")),
						responseFields(
								fieldWithPath("cron").description("Cron tasks, if any."),
								targetFieldWithPrefix("cron.[]"),
								fieldWithPath("cron.[].expression")
										.description("Cron expression."),
								fieldWithPath("fixedDelay")
										.description("Fixed delay tasks, if any."),
								targetFieldWithPrefix("fixedDelay.[]"),
								initialDelayWithPrefix("fixedDelay.[]."),
								fieldWithPath("fixedDelay.[].interval").description(
										"Interval, in milliseconds, between the end of the last"
												+ " execution and the start of the next."),
								fieldWithPath("fixedRate")
										.description("Fixed rate tasks, if any."),
								targetFieldWithPrefix("fixedRate.[]."),
								fieldWithPath("fixedRate.[].interval").description(
										"Interval, in milliseconds, between the start of each execution."),
								initialDelayWithPrefix("fixedRate.[]."))));
	}

	private FieldDescriptor targetFieldWithPrefix(String prefix) {
		return fieldWithPath(prefix + "runnable.target")
				.description("Target that will be executed.");
	}

	private FieldDescriptor initialDelayWithPrefix(String prefix) {
		return fieldWithPath(prefix + "initialDelay")
				.description("Delay, in milliseconds, before first execution.");
	}

	@Configuration
	@EnableScheduling
	@Import(BaseDocumentationConfiguration.class)
	static class TestConfiguration {

		@Bean
		public ScheduledTasksEndpoint endpoint(Collection<ScheduledTaskHolder> holders) {
			return new ScheduledTasksEndpoint(holders);
		}

		@Scheduled(cron = "0 0 0/3 1/1 * ?")
		public void processOrders() {

		}

		@Scheduled(fixedDelay = 5000, initialDelay = 5000)
		public void purge() {

		}

		@Scheduled(fixedRate = 3000, initialDelay = 10000)
		public void retrieveIssues() {

		}

	}

}
