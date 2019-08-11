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

package org.springframework.boot.actuate.scheduling;

import java.sql.Date;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger.TriggerState;
import org.quartz.TriggerBuilder;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.scheduling.QuartzEndpoint.QuartzDescriptor;
import org.springframework.boot.actuate.scheduling.QuartzEndpoint.QuartzKey;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Tests for {@link QuartzEndpoint}.
 *
 * @author Jordan Couret
 * @since 2.4.0
 */

class QuartzEndpointTests {
	private final static String JOB_NAME = "JOB_NAME";

	private final static String JOB_GROUP_NAME = "JOB_GROUP_NAME";

	private final static String TRIGGER_NAME = "TRIGGER_NAME";

	private final static String TRIGGER_GROUP_NAME = "TRIGGER_GROUP_NAME";

	private final static String JOB_DELEGATE = "jobs";

	private final static String TRIGGER_DELEGATE = "triggers";

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(QuartzAutoConfiguration.class, BaseConfiguration.class));

	@Test
	void shouldThrowExceptionIfUnrecognizedUrl() {
		this.contextRunner.run((context) -> {
			Assertions.assertThat(context).hasSingleBean(QuartzEndpoint.class);
			QuartzEndpoint endpoint = context.getBean(QuartzEndpoint.class);
			Assertions.assertThatThrownBy(() -> endpoint.findAll("INVALID"))
					.isInstanceOf(SpringQuartzException.class).hasMessage("URL should be /quartz/job OR /quartz/triggers");
		});
	}

	@Test
	void shouldFindAllJob() {
		this.contextRunner.run((context) -> {
			this.createJob(context);

			Assertions.assertThat(context).hasSingleBean(QuartzEndpoint.class);
			QuartzEndpoint endpoint = context.getBean(QuartzEndpoint.class);
			Assertions.assertThat(endpoint.findAll(JOB_DELEGATE)).hasSize(1);
		});
	}

	@Test
	void shouldFindJobByGroup() {
		this.contextRunner.run((context) -> {
			this.createJob(context);

			Assertions.assertThat(context).hasSingleBean(QuartzEndpoint.class);
			QuartzEndpoint endpoint = context.getBean(QuartzEndpoint.class);
			List<QuartzKey> searchByGroup = endpoint.findByGroup(JOB_DELEGATE, JOB_GROUP_NAME);
			Assertions.assertThat(searchByGroup).hasSize(1);
			searchByGroup.forEach((jobKey) -> Assertions.assertThat(jobKey.getGroup()).isEqualTo(JOB_GROUP_NAME));
		});
	}

	@Test
	void shouldFindJobByGroupAndName() {
		this.contextRunner.run((context) -> {
			this.createJob(context);

			Assertions.assertThat(context).hasSingleBean(QuartzEndpoint.class);
			QuartzEndpoint endpoint = context.getBean(QuartzEndpoint.class);
			QuartzDescriptor searchByGroupAndName = endpoint.findByGroupAndName(JOB_DELEGATE, JOB_GROUP_NAME, JOB_NAME);
			Assertions.assertThat(searchByGroupAndName.getJobKey().getName()).isEqualTo(JOB_NAME);
			Assertions.assertThat(searchByGroupAndName.getJobKey().getGroup()).isEqualTo(JOB_GROUP_NAME);
		});
	}

	@Test
	void shouldDeleteJob() {
		this.contextRunner.run((context) -> {
			Assertions.assertThat(context).hasSingleBean(Scheduler.class);
			Scheduler scheduler = context.getBean(Scheduler.class);
			this.createJob(context);

			Assertions.assertThat(context).hasSingleBean(QuartzEndpoint.class);
			QuartzEndpoint endpoint = context.getBean(QuartzEndpoint.class);
			endpoint.deleteKey(JOB_DELEGATE, JOB_GROUP_NAME, JOB_NAME);
			JobDetail jobDetail = scheduler.getJobDetail(JobKey.jobKey(JOB_NAME, JOB_GROUP_NAME));
			Assertions.assertThat(jobDetail).isNull();
		});
	}

	@Test
	void shouldPutInPauseThenResumeJob() {
		this.contextRunner.run((context) -> {
			Assertions.assertThat(context).hasSingleBean(Scheduler.class);
			Scheduler scheduler = context.getBean(Scheduler.class);
			SimpleTrigger simpleTrigger = this.createJob(context);

			// In pause
			Assertions.assertThat(context).hasSingleBean(QuartzEndpoint.class);
			QuartzEndpoint endpoint = context.getBean(QuartzEndpoint.class);
			Assertions.assertThat(endpoint.pauseOrResumeKey(JOB_DELEGATE, JOB_GROUP_NAME, JOB_NAME)).isNotNull();
			Assertions.assertThat(scheduler.getTriggerState(simpleTrigger.getKey())).isEqualTo(TriggerState.PAUSED);

			// Resume
			Assertions.assertThat(endpoint.pauseOrResumeKey(JOB_DELEGATE, JOB_GROUP_NAME, JOB_NAME)).isNotNull();
			Assertions.assertThat(scheduler.getTriggerState(simpleTrigger.getKey())).isNotEqualTo(TriggerState.PAUSED);
		});
	}

	@Test
	void shouldFindAllTrigger() {
		this.contextRunner.run((context) -> {
			this.createJob(context);

			Assertions.assertThat(context).hasSingleBean(QuartzEndpoint.class);
			QuartzEndpoint endpoint = context.getBean(QuartzEndpoint.class);
			Assertions.assertThat(endpoint.findAll(TRIGGER_DELEGATE)).hasSize(1);
		});
	}

	@Test
	void shouldFindTriggerByGroup() {
		this.contextRunner.run((context) -> {
			this.createJob(context);

			Assertions.assertThat(context).hasSingleBean(QuartzEndpoint.class);
			QuartzEndpoint endpoint = context.getBean(QuartzEndpoint.class);
			List<QuartzKey> searchByGroup = endpoint.findByGroup(TRIGGER_DELEGATE, TRIGGER_GROUP_NAME);
			Assertions.assertThat(searchByGroup).hasSize(1);
			searchByGroup.forEach(
					(triggerKey) -> Assertions.assertThat(triggerKey.getGroup()).isEqualTo(TRIGGER_GROUP_NAME));
		});
	}

	@Test
	void shouldFindTriggerByGroupAndName() {
		this.contextRunner.run((context) -> {
			this.createJob(context);

			Assertions.assertThat(context).hasSingleBean(QuartzEndpoint.class);
			QuartzEndpoint endpoint = context.getBean(QuartzEndpoint.class);
			QuartzDescriptor searchByGroupAndName = endpoint.findByGroupAndName(TRIGGER_DELEGATE,
					TRIGGER_GROUP_NAME, TRIGGER_NAME);
			Assertions.assertThat(searchByGroupAndName.getTriggerKey().getName()).isEqualTo(TRIGGER_NAME);
			Assertions.assertThat(searchByGroupAndName.getTriggerKey().getGroup()).isEqualTo(TRIGGER_GROUP_NAME);
		});
	}

	@Test
	void shouldPutInPauseThenResumeTrigger() {
		this.contextRunner.run((context) -> {
			Assertions.assertThat(context).hasSingleBean(Scheduler.class);
			Scheduler scheduler = context.getBean(Scheduler.class);
			SimpleTrigger simpleTrigger = this.createJob(context);

			// In pause
			Assertions.assertThat(context).hasSingleBean(QuartzEndpoint.class);
			QuartzEndpoint endpoint = context.getBean(QuartzEndpoint.class);
			Assertions.assertThat(
					endpoint.pauseOrResumeKey(TRIGGER_DELEGATE, TRIGGER_GROUP_NAME, TRIGGER_NAME)).isNotNull();
			Assertions.assertThat(scheduler.getTriggerState(simpleTrigger.getKey())).isEqualTo(TriggerState.PAUSED);

			// Resume
			Assertions.assertThat(endpoint.pauseOrResumeKey(TRIGGER_DELEGATE, TRIGGER_GROUP_NAME, TRIGGER_NAME))
					.isNotNull();
			Assertions.assertThat(scheduler.getTriggerState(simpleTrigger.getKey())).isNotEqualTo(TriggerState.PAUSED);
		});
	}

	@Test
	void shouldDeleteTrigger() {
		this.contextRunner.run((context) -> {
			Assertions.assertThat(context).hasSingleBean(Scheduler.class);
			Scheduler scheduler = context.getBean(Scheduler.class);
			this.createJob(context);

			Assertions.assertThat(context).hasSingleBean(QuartzEndpoint.class);
			QuartzEndpoint endpoint = context.getBean(QuartzEndpoint.class);
			endpoint.deleteKey(TRIGGER_DELEGATE, TRIGGER_GROUP_NAME, TRIGGER_NAME);

			JobDetail jobDetail = scheduler.getJobDetail(JobKey.jobKey(JOB_NAME, JOB_GROUP_NAME));
			Assertions.assertThat(jobDetail).isNull();
		});
	}

	private SimpleTrigger createJob(AssertableApplicationContext context) throws SchedulerException {
		Assertions.assertThat(context).hasSingleBean(Scheduler.class);
		Scheduler scheduler = context.getBean(Scheduler.class);

		JobDataMap map = new JobDataMap();
		map.put("name", JOB_NAME);
		JobDetail jobDetail = JobBuilder.newJob(SampleJob.class).withIdentity(JOB_NAME, JOB_GROUP_NAME)
				.usingJobData(map).build();

		java.util.Date start = Date
				.from(LocalDateTime.now().plusSeconds(15).atZone(ZoneId.systemDefault()).toInstant());

		SimpleTrigger sampleTrigger = TriggerBuilder.newTrigger().forJob(jobDetail)
				.withIdentity(TRIGGER_NAME, TRIGGER_GROUP_NAME)
				.withSchedule(SimpleScheduleBuilder.simpleSchedule()).startAt(start).build();

		scheduler.scheduleJob(jobDetail, sampleTrigger);
		return sampleTrigger;
	}

	@Configuration(proxyBeanMethods = false)
	@AutoConfigureAfter(QuartzAutoConfiguration.class)
	public static class BaseConfiguration {

		@Bean
		public QuartzEndpoint quartzEndpoint(ObjectProvider<Scheduler> scheduler) {
			return new QuartzEndpoint(scheduler.getIfAvailable());

		}
	}

	public static class SampleJob implements Job {
		@Override
		public void execute(JobExecutionContext context) throws JobExecutionException {
			// Do nothing
		}
	}
}
