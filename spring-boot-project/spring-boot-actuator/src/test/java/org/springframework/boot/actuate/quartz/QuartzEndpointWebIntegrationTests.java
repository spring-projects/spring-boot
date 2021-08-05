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

package org.springframework.boot.actuate.quartz;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;

import net.minidev.json.JSONArray;
import org.quartz.CalendarIntervalScheduleBuilder;
import org.quartz.CalendarIntervalTrigger;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.Trigger.TriggerState;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.matchers.GroupMatcher;

import org.springframework.boot.actuate.endpoint.web.test.WebEndpointTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.DelegatingJob;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Integration tests for {@link QuartzEndpoint} exposed by Jersey, Spring MVC, and
 * WebFlux.
 *
 * @author Stephane Nicoll
 */
class QuartzEndpointWebIntegrationTests {

	private static final JobDetail jobOne = JobBuilder.newJob(Job.class).withIdentity("jobOne", "samples")
			.usingJobData(new JobDataMap(Collections.singletonMap("name", "test"))).withDescription("A sample job")
			.build();

	private static final JobDetail jobTwo = JobBuilder.newJob(DelegatingJob.class).withIdentity("jobTwo", "samples")
			.build();

	private static final JobDetail jobThree = JobBuilder.newJob(Job.class).withIdentity("jobThree").build();

	private static final CronTrigger triggerOne = TriggerBuilder.newTrigger().withDescription("Once a day 3AM")
			.withIdentity("triggerOne").withSchedule(CronScheduleBuilder.dailyAtHourAndMinute(3, 0)).build();

	private static final SimpleTrigger triggerTwo = TriggerBuilder.newTrigger().withDescription("Once a day")
			.withIdentity("triggerTwo", "tests").withSchedule(SimpleScheduleBuilder.repeatHourlyForever(24)).build();

	private static final CalendarIntervalTrigger triggerThree = TriggerBuilder.newTrigger()
			.withDescription("Once a week").withIdentity("triggerThree", "tests")
			.withSchedule(CalendarIntervalScheduleBuilder.calendarIntervalSchedule().withIntervalInWeeks(1)).build();

	@WebEndpointTest
	void quartzReport(WebTestClient client) {
		client.get().uri("/actuator/quartz").exchange().expectStatus().isOk().expectBody().jsonPath("jobs.groups")
				.isEqualTo(new JSONArray().appendElement("samples").appendElement("DEFAULT"))
				.jsonPath("triggers.groups").isEqualTo(new JSONArray().appendElement("DEFAULT").appendElement("tests"));
	}

	@WebEndpointTest
	void quartzJobNames(WebTestClient client) {
		client.get().uri("/actuator/quartz/jobs").exchange().expectStatus().isOk().expectBody()
				.jsonPath("groups.samples.jobs")
				.isEqualTo(new JSONArray().appendElement("jobOne").appendElement("jobTwo"))
				.jsonPath("groups.DEFAULT.jobs").isEqualTo(new JSONArray().appendElement("jobThree"));
	}

	@WebEndpointTest
	void quartzTriggerNames(WebTestClient client) {
		client.get().uri("/actuator/quartz/triggers").exchange().expectStatus().isOk().expectBody()
				.jsonPath("groups.DEFAULT.paused").isEqualTo(false).jsonPath("groups.DEFAULT.triggers")
				.isEqualTo(new JSONArray().appendElement("triggerOne")).jsonPath("groups.tests.paused").isEqualTo(false)
				.jsonPath("groups.tests.triggers")
				.isEqualTo(new JSONArray().appendElement("triggerTwo").appendElement("triggerThree"));
	}

	@WebEndpointTest
	void quartzTriggersOrJobsAreAllowed(WebTestClient client) {
		client.get().uri("/actuator/quartz/something-elese").exchange().expectStatus().isBadRequest();
	}

	@WebEndpointTest
	void quartzJobGroupSummary(WebTestClient client) {
		client.get().uri("/actuator/quartz/jobs/samples").exchange().expectStatus().isOk().expectBody()
				.jsonPath("group").isEqualTo("samples").jsonPath("jobs.jobOne.className").isEqualTo(Job.class.getName())
				.jsonPath("jobs.jobTwo.className").isEqualTo(DelegatingJob.class.getName());
	}

	@WebEndpointTest
	void quartzJobGroupSummaryWithUnknownGroup(WebTestClient client) {
		client.get().uri("/actuator/quartz/jobs/does-not-exist").exchange().expectStatus().isNotFound();
	}

	@WebEndpointTest
	void quartzTriggerGroupSummary(WebTestClient client) {
		client.get().uri("/actuator/quartz/triggers/tests").exchange().expectStatus().isOk().expectBody()
				.jsonPath("group").isEqualTo("tests").jsonPath("paused").isEqualTo("false").jsonPath("triggers.cron")
				.isEmpty().jsonPath("triggers.simple.triggerTwo.interval").isEqualTo(86400000)
				.jsonPath("triggers.dailyTimeInterval").isEmpty()
				.jsonPath("triggers.calendarInterval.triggerThree.interval").isEqualTo(604800000)
				.jsonPath("triggers.custom").isEmpty();
	}

	@WebEndpointTest
	void quartzTriggerGroupSummaryWithUnknownGroup(WebTestClient client) {
		client.get().uri("/actuator/quartz/triggers/does-not-exist").exchange().expectStatus().isNotFound();
	}

	@WebEndpointTest
	void quartzJobDetail(WebTestClient client) {
		client.get().uri("/actuator/quartz/jobs/samples/jobOne").exchange().expectStatus().isOk().expectBody()
				.jsonPath("group").isEqualTo("samples").jsonPath("name").isEqualTo("jobOne").jsonPath("data.name")
				.isEqualTo("test");
	}

	@WebEndpointTest
	void quartzJobDetailWithUnknownKey(WebTestClient client) {
		client.get().uri("/actuator/quartz/jobs/samples/does-not-exist").exchange().expectStatus().isNotFound();
	}

	@WebEndpointTest
	void quartzTriggerDetail(WebTestClient client) {
		client.get().uri("/actuator/quartz/triggers/DEFAULT/triggerOne").exchange().expectStatus().isOk().expectBody()
				.jsonPath("group").isEqualTo("DEFAULT").jsonPath("name").isEqualTo("triggerOne").jsonPath("description")
				.isEqualTo("Once a day 3AM").jsonPath("state").isEqualTo("NORMAL").jsonPath("type").isEqualTo("cron")
				.jsonPath("simple").doesNotExist().jsonPath("calendarInterval").doesNotExist().jsonPath("dailyInterval")
				.doesNotExist().jsonPath("custom").doesNotExist().jsonPath("cron.expression").isEqualTo("0 0 3 ? * *");
	}

	@WebEndpointTest
	void quartzTriggerDetailWithUnknownKey(WebTestClient client) {
		client.get().uri("/actuator/quartz/triggers/tests/does-not-exist").exchange().expectStatus().isNotFound();
	}

	@Configuration(proxyBeanMethods = false)
	static class TestConfiguration {

		@Bean
		Scheduler scheduler() throws SchedulerException {
			Scheduler scheduler = mock(Scheduler.class);
			mockJobs(scheduler, jobOne, jobTwo, jobThree);
			mockTriggers(scheduler, triggerOne, triggerTwo, triggerThree);
			return scheduler;
		}

		@Bean
		QuartzEndpoint endpoint(Scheduler scheduler) {
			return new QuartzEndpoint(scheduler);
		}

		@Bean
		QuartzEndpointWebExtension quartzEndpointWebExtension(QuartzEndpoint endpoint) {
			return new QuartzEndpointWebExtension(endpoint);
		}

		private void mockJobs(Scheduler scheduler, JobDetail... jobs) throws SchedulerException {
			MultiValueMap<String, JobKey> jobKeys = new LinkedMultiValueMap<>();
			for (JobDetail jobDetail : jobs) {
				JobKey key = jobDetail.getKey();
				given(scheduler.getJobDetail(key)).willReturn(jobDetail);
				jobKeys.add(key.getGroup(), key);
			}
			given(scheduler.getJobGroupNames()).willReturn(new ArrayList<>(jobKeys.keySet()));
			for (Entry<String, List<JobKey>> entry : jobKeys.entrySet()) {
				given(scheduler.getJobKeys(GroupMatcher.jobGroupEquals(entry.getKey())))
						.willReturn(new LinkedHashSet<>(entry.getValue()));
			}
		}

		void mockTriggers(Scheduler scheduler, Trigger... triggers) throws SchedulerException {
			MultiValueMap<String, TriggerKey> triggerKeys = new LinkedMultiValueMap<>();
			for (Trigger trigger : triggers) {
				TriggerKey key = trigger.getKey();
				given(scheduler.getTrigger(key)).willReturn(trigger);
				given(scheduler.getTriggerState(key)).willReturn(TriggerState.NORMAL);
				triggerKeys.add(key.getGroup(), key);
			}
			given(scheduler.getTriggerGroupNames()).willReturn(new ArrayList<>(triggerKeys.keySet()));
			for (Entry<String, List<TriggerKey>> entry : triggerKeys.entrySet()) {
				given(scheduler.getTriggerKeys(GroupMatcher.triggerGroupEquals(entry.getKey())))
						.willReturn(new LinkedHashSet<>(entry.getValue()));
			}
		}

	}

}
