/*
 * Copyright 2012-2020 the original author or authors.
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

import java.sql.Date;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.SimpleTrigger;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.scheduling.QuartzEndpoint;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation;
import org.springframework.restdocs.payload.FieldDescriptor;

import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for generating documentation describing the {@link QuartzEndpoint}
 *
 * @author Jordan Couret
 */
public class QuartzEndpointDocumentationTests extends MockMvcEndpointDocumentationTests {

	@Autowired
	private Scheduler scheduler;

	private static final JobKey jobKey = JobKey.jobKey("jobName", "jobGroup");

	private static final List<FieldDescriptor> fullFields = Arrays.asList(
			fieldWithPath("jobKey.name").description("Name of the job."),
			fieldWithPath("jobKey.group").description("Name of the job's group."),
			fieldWithPath("triggerKey.name").description("Name of the trigger."),
			fieldWithPath("triggerKey.group").description("Name of the trigger's group."),
			fieldWithPath("jobClass").description("Simple class name of the job."),
			fieldWithPath("data").description("Map that's represent the job's JobDataMap."),
			fieldWithPath("triggerState").description("State of the job."),
			fieldWithPath("nextFireDate").description("The next fire date of the job."));

	@Override
	@BeforeEach
	void setup(RestDocumentationContextProvider restDocumentation) {
		super.setup(restDocumentation);
		this.generateJob();
	}

	@AfterEach
	void clean() throws SchedulerException {
		this.cleanJob();
	}

	@Test
	void allJobs() throws Exception {
		this.mockMvc.perform(get("/actuator/quartz/jobs")).andExpect(status().isOk())
				.andDo(MockMvcRestDocumentation.document("quartz/jobs-all",
						responseFields(fieldWithPath("[].name").description("Name of the job."),
								fieldWithPath("[].group").description("Group of the job."))));
	}

	@Test
	void jobsForGroup() throws Exception {
		this.mockMvc.perform(get("/actuator/quartz/jobs/jobGroup")).andExpect(status().isOk())
				.andDo(MockMvcRestDocumentation.document("quartz/jobs-group",
						responseFields(fieldWithPath("[].name").description("Name of the job."),
								fieldWithPath("[].group").description("Group of the job."))));
	}

	@Test
	void jobsForGroupAndName() throws Exception {
		this.mockMvc.perform(get("/actuator/quartz/jobs/jobGroup/jobName")).andExpect(status().isOk())
				.andDo(MockMvcRestDocumentation.document("quartz/jobs-group-name", responseFields(fullFields)));
	}

	@Test
	void pauseOrResumeJob() throws Exception {
		this.mockMvc.perform(post("/actuator/quartz/jobs/jobGroup/jobName")).andExpect(status().isOk())
				.andDo(MockMvcRestDocumentation.document("quartz/jobs-pause-resume", responseFields(fullFields)));
	}

	@Test
	void deleteJob() throws Exception {
		this.mockMvc.perform(delete("/actuator/quartz/jobs/jobGroup/jobName")).andExpect(status().isOk())
				.andDo(MockMvcRestDocumentation.document("quartz/jobs-delete"));
	}

	@Test
	void allTriggers() throws Exception {
		this.mockMvc.perform(get("/actuator/quartz/triggers")).andExpect(status().isOk())
				.andDo(MockMvcRestDocumentation.document("quartz/triggers-all",
						responseFields(fieldWithPath("[].name").description("Name of the trigger."),
								fieldWithPath("[].group").description("Group of the trigger."))));
	}

	@Test
	void triggerssForGroup() throws Exception {
		this.mockMvc.perform(get("/actuator/quartz/triggers/triggerGroup")).andExpect(status().isOk())
				.andDo(MockMvcRestDocumentation.document("quartz/triggers-group",
						responseFields(fieldWithPath("[].name").description("Name of the trigger."),
								fieldWithPath("[].group").description("Group of the trigger."))));
	}

	@Test
	void triggersForGroupAndName() throws Exception {
		this.mockMvc.perform(get("/actuator/quartz/triggers/triggerGroup/triggerName")).andExpect(status().isOk())
				.andDo(MockMvcRestDocumentation.document("quartz/triggers-group-name", responseFields(fullFields)));
	}

	@Test
	void pauseOrResumeTrigger() throws Exception {
		this.mockMvc.perform(post("/actuator/quartz/triggers/triggerGroup/triggerName")).andExpect(status().isOk())
				.andDo(MockMvcRestDocumentation.document("quartz/triggers-pause-resume", responseFields(fullFields)));
	}

	@Test
	void deleteTrigger() throws Exception {
		this.mockMvc.perform(delete("/actuator/quartz/triggers/triggerGroup/triggerName")).andExpect(status().isOk())
				.andDo(MockMvcRestDocumentation.document("quartz/triggers-delete"));
	}

	private void generateJob() {
		try {
			JobDetail jobDetail = JobBuilder.newJob(SampleJob.class).withIdentity(jobKey).usingJobData(new JobDataMap())
					.build();

			java.util.Date start = Date
					.from(LocalDateTime.now().plusYears(1).atZone(ZoneId.systemDefault()).toInstant());

			SimpleTrigger sampleTrigger = TriggerBuilder.newTrigger().forJob(jobDetail)
					.withIdentity("triggerName", "triggerGroup").withSchedule(SimpleScheduleBuilder.simpleSchedule())
					.startAt(start).build();

			scheduler.scheduleJob(jobDetail, sampleTrigger);
		}
		catch (SchedulerException e) {
			throw new RuntimeException(e.getMessage(), e.getCause());
		}
	}

	private void cleanJob() throws SchedulerException {
		if (scheduler.checkExists(jobKey)) {
			scheduler.deleteJob(jobKey);
		}
	}

	@Configuration(proxyBeanMethods = false)
	@Import({ BaseDocumentationConfiguration.class, QuartzAutoConfiguration.class })
	@AutoConfigureAfter(QuartzAutoConfiguration.class)
	static class TestConfiguration {

		@Bean
		QuartzEndpoint endpoint(Scheduler scheduler) {
			return new QuartzEndpoint(scheduler);
		}

	}

	public static class SampleJob implements Job {

		@Override
		public void execute(JobExecutionContext context) {
			// Do nothing
		}

	}

}
