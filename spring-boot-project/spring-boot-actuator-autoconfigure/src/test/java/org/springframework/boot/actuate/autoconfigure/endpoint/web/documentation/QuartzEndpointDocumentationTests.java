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

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.regex.Pattern;

import org.junit.Test;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.matchers.GroupMatcher;

import org.springframework.boot.actuate.quartz.QuartzEndpoint;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.replacePattern;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for generating documentation describing the {@link QuartzEndpoint}.
 *
 * @author Vedran Pavic
 */
public class QuartzEndpointDocumentationTests extends MockMvcEndpointDocumentationTests {

	private static final JobDetail jobOne = JobBuilder.newJob(Job.class).withIdentity("jobOne", "groupOne")
			.withDescription("My first job").build();

	private static final JobDetail jobTwo = JobBuilder.newJob(Job.class).withIdentity("jobTwo", "groupOne").build();

	private static final JobDetail jobThree = JobBuilder.newJob(Job.class).withIdentity("jobThree", "groupTwo").build();

	private static final Trigger triggerOne = TriggerBuilder.newTrigger().forJob(jobOne).withIdentity("triggerOne")
			.withDescription("My first trigger").modifiedByCalendar("myCalendar")
			.startAt(Date.from(Instant.parse("2017-12-01T12:00:00Z")))
			.endAt(Date.from(Instant.parse("2017-12-01T12:30:00Z")))
			.withSchedule(SimpleScheduleBuilder.repeatMinutelyForever()).build();

	private static final Trigger triggerTwo = TriggerBuilder.newTrigger().forJob(jobOne).withIdentity("triggerTwo")
			.withDescription("My second trigger").modifiedByCalendar("myCalendar")
			.startAt(Date.from(Instant.parse("2017-12-01T00:00:00Z")))
			.endAt(Date.from(Instant.parse("2017-12-10T00:00:00Z")))
			.withSchedule(SimpleScheduleBuilder.repeatHourlyForever()).build();

	@MockBean
	private Scheduler scheduler;

	@Test
	public void quartzReport() throws Exception {
		String groupOne = jobOne.getKey().getGroup();
		String groupTwo = jobThree.getKey().getGroup();
		given(this.scheduler.getJobGroupNames()).willReturn(Arrays.asList(groupOne, groupTwo));
		given(this.scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupOne)))
				.willReturn(new HashSet<>(Arrays.asList(jobOne.getKey(), jobTwo.getKey())));
		given(this.scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupTwo)))
				.willReturn(Collections.singleton(jobThree.getKey()));
		this.mockMvc.perform(get("/application/quartz")).andExpect(status().isOk()).andDo(document("quartz/report"));
	}

	@Test
	public void quartzJob() throws Exception {
		JobKey jobKey = jobOne.getKey();
		given(this.scheduler.getJobDetail(jobKey)).willReturn(jobOne);
		given(this.scheduler.getTriggersOfJob(jobKey)).willAnswer(invocation -> Arrays.asList(triggerOne, triggerTwo));
		this.mockMvc.perform(get("/application/quartz/groupOne/jobOne")).andExpect(status().isOk()).andDo(document(
				"quartz/job",
				preprocessResponse(replacePattern(Pattern.compile("org.quartz.Job"), "com.example.MyJob")),
				responseFields(fieldWithPath("jobGroup").description("Job group."),
						fieldWithPath("jobName").description("Job name."),
						fieldWithPath("description").description("Job description, if any."),
						fieldWithPath("className").description("Job class."),
						fieldWithPath("triggers.[].triggerGroup").description("Trigger group."),
						fieldWithPath("triggers.[].triggerName").description("Trigger name."),
						fieldWithPath("triggers.[].description").description("Trigger description, if any."),
						fieldWithPath("triggers.[].calendarName").description("Trigger's calendar name, if any."),
						fieldWithPath("triggers.[].startTime").description("Trigger's start time."),
						fieldWithPath("triggers.[].endTime").description("Trigger's end time."),
						fieldWithPath("triggers.[].nextFireTime").description("Trigger's next fire time."),
						fieldWithPath("triggers.[].previousFireTime")
								.description("Trigger's previous fire time, if any."),
						fieldWithPath("triggers.[].finalFireTime").description("Trigger's final fire time, if any."))));
	}

	@Configuration
	@Import(BaseDocumentationConfiguration.class)
	static class TestConfiguration {

		@Bean
		public QuartzEndpoint endpoint(Scheduler scheduler) {
			return new QuartzEndpoint(scheduler);
		}

	}

}
