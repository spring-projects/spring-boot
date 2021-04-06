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

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;
import org.quartz.CalendarIntervalScheduleBuilder;
import org.quartz.CalendarIntervalTrigger;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.DailyTimeIntervalScheduleBuilder;
import org.quartz.DailyTimeIntervalTrigger;
import org.quartz.DateBuilder.IntervalUnit;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.SimpleTrigger;
import org.quartz.TimeOfDay;
import org.quartz.Trigger;
import org.quartz.Trigger.TriggerState;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.spi.OperableTrigger;

import org.springframework.boot.actuate.quartz.QuartzEndpoint;
import org.springframework.boot.actuate.quartz.QuartzEndpointWebExtension;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.scheduling.quartz.DelegatingJob;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.relaxedResponseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for generating documentation describing the {@link QuartzEndpoint}.
 *
 * @author Vedran Pavic
 * @author Stephane Nicoll
 */
class QuartzEndpointDocumentationTests extends MockMvcEndpointDocumentationTests {

	private static final TimeZone timeZone = TimeZone.getTimeZone("Europe/Paris");

	private static final JobDetail jobOne = JobBuilder.newJob(DelegatingJob.class).withIdentity("jobOne", "samples")
			.withDescription("A sample job").usingJobData("user", "admin").usingJobData("password", "secret").build();

	private static final JobDetail jobTwo = JobBuilder.newJob(Job.class).withIdentity("jobTwo", "samples").build();

	private static final JobDetail jobThree = JobBuilder.newJob(Job.class).withIdentity("jobThree", "tests").build();

	private static final CronTrigger cronTrigger = TriggerBuilder.newTrigger().forJob(jobOne).withPriority(3)
			.withDescription("3AM on weekdays").withIdentity("3am-weekdays", "samples")
			.withSchedule(
					CronScheduleBuilder.atHourAndMinuteOnGivenDaysOfWeek(3, 0, 1, 2, 3, 4, 5).inTimeZone(timeZone))
			.build();

	private static final SimpleTrigger simpleTrigger = TriggerBuilder.newTrigger().forJob(jobOne).withPriority(7)
			.withDescription("Once a day").withIdentity("every-day", "samples")
			.withSchedule(SimpleScheduleBuilder.repeatHourlyForever(24)).build();

	private static final CalendarIntervalTrigger calendarIntervalTrigger = TriggerBuilder.newTrigger().forJob(jobTwo)
			.withDescription("Once a week").withIdentity("once-a-week", "samples")
			.withSchedule(CalendarIntervalScheduleBuilder.calendarIntervalSchedule().withIntervalInWeeks(1)
					.inTimeZone(timeZone))
			.build();

	private static final DailyTimeIntervalTrigger dailyTimeIntervalTrigger = TriggerBuilder.newTrigger()
			.forJob(jobThree).withDescription("Every hour between 9AM and 6PM on Tuesday and Thursday")
			.withIdentity("every-hour-tue-thu")
			.withSchedule(DailyTimeIntervalScheduleBuilder.dailyTimeIntervalSchedule()
					.onDaysOfTheWeek(Calendar.TUESDAY, Calendar.THURSDAY)
					.startingDailyAt(TimeOfDay.hourAndMinuteOfDay(9, 0))
					.endingDailyAt(TimeOfDay.hourAndMinuteOfDay(18, 0)).withInterval(1, IntervalUnit.HOUR))
			.build();

	private static final List<FieldDescriptor> triggerSummary = Arrays.asList(previousFireTime(""), nextFireTime(""),
			priority(""));

	private static final List<FieldDescriptor> cronTriggerSummary = Arrays.asList(
			fieldWithPath("expression").description("Cron expression to use."),
			fieldWithPath("timeZone").type(JsonFieldType.STRING).optional()
					.description("Time zone for which the expression will be resolved, if any."));

	private static final List<FieldDescriptor> simpleTriggerSummary = Collections
			.singletonList(fieldWithPath("interval").description("Interval, in milliseconds, between two executions."));

	private static final List<FieldDescriptor> dailyTimeIntervalTriggerSummary = Arrays.asList(
			fieldWithPath("interval").description(
					"Interval, in milliseconds, added to the fire time in order to calculate the time of the next trigger repeat."),
			fieldWithPath("daysOfWeek").type(JsonFieldType.ARRAY)
					.description("An array of days of the week upon which to fire."),
			fieldWithPath("startTimeOfDay").type(JsonFieldType.STRING)
					.description("Time of day to start firing at the given interval, if any."),
			fieldWithPath("endTimeOfDay").type(JsonFieldType.STRING)
					.description("Time of day to complete firing at the given interval, if any."));

	private static final List<FieldDescriptor> calendarIntervalTriggerSummary = Arrays.asList(
			fieldWithPath("interval").description(
					"Interval, in milliseconds, added to the fire time in order to calculate the time of the next trigger repeat."),
			fieldWithPath("timeZone").type(JsonFieldType.STRING)
					.description("Time zone within which time calculations will be performed, if any."));

	private static final List<FieldDescriptor> customTriggerSummary = Collections.singletonList(
			fieldWithPath("trigger").description("A toString representation of the custom trigger instance."));

	private static final FieldDescriptor[] commonCronDetails = new FieldDescriptor[] {
			fieldWithPath("group").description("Name of the group."),
			fieldWithPath("name").description("Name of the trigger."),
			fieldWithPath("description").description("Description of the trigger, if any."),
			fieldWithPath("state")
					.description("State of the trigger (" + describeEnumValues(TriggerState.class) + ")."),
			fieldWithPath("type").description(
					"Type of the trigger (`calendarInterval`, `cron`, `custom`, `dailyTimeInterval`, `simple`). "
							+ "Determines the key of the object containing type-specific details."),
			fieldWithPath("calendarName").description("Name of the Calendar associated with this Trigger, if any."),
			startTime(""), endTime(""), previousFireTime(""), nextFireTime(""), priority(""),
			fieldWithPath("finalFireTime").optional().type(JsonFieldType.STRING)
					.description("Last time at which the Trigger will fire, if any."),
			fieldWithPath("data").optional().type(JsonFieldType.OBJECT)
					.description("Job data map keyed by name, if any.") };

	@MockBean
	private Scheduler scheduler;

	@Test
	void quartzReport() throws Exception {
		mockJobs(jobOne, jobTwo, jobThree);
		mockTriggers(cronTrigger, simpleTrigger, calendarIntervalTrigger, dailyTimeIntervalTrigger);
		this.mockMvc.perform(get("/actuator/quartz")).andExpect(status().isOk())
				.andDo(document("quartz/report",
						responseFields(fieldWithPath("jobs.groups").description("An array of job group names."),
								fieldWithPath("triggers.groups").description("An array of trigger group names."))));
	}

	@Test
	void quartzJobs() throws Exception {
		mockJobs(jobOne, jobTwo, jobThree);
		this.mockMvc.perform(get("/actuator/quartz/jobs")).andExpect(status().isOk()).andDo(
				document("quartz/jobs", responseFields(fieldWithPath("groups").description("Job groups keyed by name."),
						fieldWithPath("groups.*.jobs").description("An array of job names."))));
	}

	@Test
	void quartzTriggers() throws Exception {
		mockTriggers(cronTrigger, simpleTrigger, calendarIntervalTrigger, dailyTimeIntervalTrigger);
		this.mockMvc.perform(get("/actuator/quartz/triggers")).andExpect(status().isOk())
				.andDo(document("quartz/triggers",
						responseFields(fieldWithPath("groups").description("Trigger groups keyed by name."),
								fieldWithPath("groups.*.paused").description("Whether this trigger group is paused."),
								fieldWithPath("groups.*.triggers").description("An array of trigger names."))));
	}

	@Test
	void quartzJobGroup() throws Exception {
		mockJobs(jobOne, jobTwo, jobThree);
		this.mockMvc.perform(get("/actuator/quartz/jobs/samples")).andExpect(status().isOk())
				.andDo(document("quartz/job-group",
						responseFields(fieldWithPath("group").description("Name of the group."),
								fieldWithPath("jobs").description("Job details keyed by name."),
								fieldWithPath("jobs.*.className")
										.description("Fully qualified name of the job implementation."))));
	}

	@Test
	void quartzTriggerGroup() throws Exception {
		CronTrigger cron = cronTrigger.getTriggerBuilder().startAt(fromUtc("2020-11-30T17:00:00Z"))
				.endAt(fromUtc("2020-12-30T03:00:00Z")).withIdentity("3am-week", "tests").build();
		setPreviousNextFireTime(cron, "2020-12-04T03:00:00Z", "2020-12-07T03:00:00Z");
		SimpleTrigger simple = simpleTrigger.getTriggerBuilder().withIdentity("every-day", "tests").build();
		setPreviousNextFireTime(simple, null, "2020-12-04T12:00:00Z");
		CalendarIntervalTrigger calendarInterval = calendarIntervalTrigger.getTriggerBuilder()
				.withIdentity("once-a-week", "tests").startAt(fromUtc("2019-07-10T14:00:00Z"))
				.endAt(fromUtc("2023-01-01T12:00:00Z")).build();
		setPreviousNextFireTime(calendarInterval, "2020-12-02T14:00:00Z", "2020-12-08T14:00:00Z");
		DailyTimeIntervalTrigger tueThuTrigger = dailyTimeIntervalTrigger.getTriggerBuilder()
				.withIdentity("tue-thu", "tests").build();
		Trigger customTrigger = mock(Trigger.class);
		given(customTrigger.getKey()).willReturn(TriggerKey.triggerKey("once-a-year-custom", "tests"));
		given(customTrigger.toString()).willReturn("com.example.CustomTrigger@fdsfsd");
		given(customTrigger.getPriority()).willReturn(10);
		given(customTrigger.getPreviousFireTime()).willReturn(fromUtc("2020-07-14T16:00:00Z"));
		given(customTrigger.getNextFireTime()).willReturn(fromUtc("2021-07-14T16:00:00Z"));
		mockTriggers(cron, simple, calendarInterval, tueThuTrigger, customTrigger);
		this.mockMvc.perform(get("/actuator/quartz/triggers/tests")).andExpect(status().isOk()).andDo(document(
				"quartz/trigger-group",
				responseFields(fieldWithPath("group").description("Name of the group."),
						fieldWithPath("paused").description("Whether the group is paused."),
						fieldWithPath("triggers.cron").description("Cron triggers keyed by name, if any."),
						fieldWithPath("triggers.simple").description("Simple triggers keyed by name, if any."),
						fieldWithPath("triggers.dailyTimeInterval")
								.description("Daily time interval triggers keyed by name, if any."),
						fieldWithPath("triggers.calendarInterval")
								.description("Calendar interval triggers keyed by name, if any."),
						fieldWithPath("triggers.custom").description("Any other triggers keyed by name, if any."))
								.andWithPrefix("triggers.cron.*.", concat(triggerSummary, cronTriggerSummary))
								.andWithPrefix("triggers.simple.*.", concat(triggerSummary, simpleTriggerSummary))
								.andWithPrefix("triggers.dailyTimeInterval.*.",
										concat(triggerSummary, dailyTimeIntervalTriggerSummary))
								.andWithPrefix("triggers.calendarInterval.*.",
										concat(triggerSummary, calendarIntervalTriggerSummary))
								.andWithPrefix("triggers.custom.*.", concat(triggerSummary, customTriggerSummary))));
	}

	@Test
	void quartzJob() throws Exception {
		mockJobs(jobOne);
		CronTrigger firstTrigger = cronTrigger.getTriggerBuilder().build();
		setPreviousNextFireTime(firstTrigger, null, "2020-12-07T03:00:00Z");
		SimpleTrigger secondTrigger = simpleTrigger.getTriggerBuilder().build();
		setPreviousNextFireTime(secondTrigger, "2020-12-04T03:00:00Z", "2020-12-04T12:00:00Z");
		mockTriggers(firstTrigger, secondTrigger);
		given(this.scheduler.getTriggersOfJob(jobOne.getKey()))
				.willAnswer((invocation) -> Arrays.asList(firstTrigger, secondTrigger));
		this.mockMvc.perform(get("/actuator/quartz/jobs/samples/jobOne")).andExpect(status().isOk()).andDo(document(
				"quartz/job-details",
				responseFields(fieldWithPath("group").description("Name of the group."),
						fieldWithPath("name").description("Name of the job."),
						fieldWithPath("description").description("Description of the job, if any."),
						fieldWithPath("className").description("Fully qualified name of the job implementation."),
						fieldWithPath("durable")
								.description("Whether the job should remain stored after it is orphaned."),
						fieldWithPath("requestRecovery").description(
								"Whether the job should be re-executed if a 'recovery' or 'fail-over' situation is encountered."),
						fieldWithPath("data.*").description("Job data map as key/value pairs, if any."),
						fieldWithPath("triggers").description("An array of triggers associated to the job, if any."),
						fieldWithPath("triggers.[].group").description("Name of the the trigger group."),
						fieldWithPath("triggers.[].name").description("Name of the the trigger."),
						previousFireTime("triggers.[]."), nextFireTime("triggers.[]."), priority("triggers.[]."))));
	}

	@Test
	void quartzTriggerCommon() throws Exception {
		setupTriggerDetails(cronTrigger.getTriggerBuilder(), TriggerState.NORMAL);
		this.mockMvc.perform(get("/actuator/quartz/triggers/samples/example")).andExpect(status().isOk())
				.andDo(document("quartz/trigger-details-common", responseFields(commonCronDetails).and(
						subsectionWithPath("calendarInterval").description(
								"Calendar time interval trigger details, if any. Present when `type` is `calendarInterval`.")
								.optional().type(JsonFieldType.OBJECT),
						subsectionWithPath("custom")
								.description("Custom trigger details, if any. Present when `type` is `custom`.")
								.optional().type(JsonFieldType.OBJECT),
						subsectionWithPath("cron")
								.description("Cron trigger details, if any. Present when `type` is `cron`.").optional()
								.type(JsonFieldType.OBJECT),
						subsectionWithPath("dailyTimeInterval").description(
								"Daily time interval trigger details, if any. Present when `type` is `dailyTimeInterval`.")
								.optional().type(JsonFieldType.OBJECT),
						subsectionWithPath("simple")
								.description("Simple trigger details, if any. Present when `type` is `simple`.")
								.optional().type(JsonFieldType.OBJECT))));
	}

	@Test
	void quartzTriggerCron() throws Exception {
		setupTriggerDetails(cronTrigger.getTriggerBuilder(), TriggerState.NORMAL);
		this.mockMvc.perform(get("/actuator/quartz/triggers/samples/example")).andExpect(status().isOk())
				.andDo(document("quartz/trigger-details-cron",
						relaxedResponseFields(fieldWithPath("cron").description("Cron trigger specific details."))
								.andWithPrefix("cron.", cronTriggerSummary)));
	}

	@Test
	void quartzTriggerSimple() throws Exception {
		setupTriggerDetails(simpleTrigger.getTriggerBuilder(), TriggerState.NORMAL);
		this.mockMvc.perform(get("/actuator/quartz/triggers/samples/example")).andExpect(status().isOk())
				.andDo(document("quartz/trigger-details-simple",
						relaxedResponseFields(fieldWithPath("simple").description("Simple trigger specific details."))
								.andWithPrefix("simple.", simpleTriggerSummary)
								.and(repeatCount("simple."), timesTriggered("simple."))));
	}

	@Test
	void quartzTriggerCalendarInterval() throws Exception {
		setupTriggerDetails(calendarIntervalTrigger.getTriggerBuilder(), TriggerState.NORMAL);
		this.mockMvc.perform(get("/actuator/quartz/triggers/samples/example")).andExpect(status().isOk())
				.andDo(document("quartz/trigger-details-calendar-interval", relaxedResponseFields(
						fieldWithPath("calendarInterval").description("Calendar interval trigger specific details."))
								.andWithPrefix("calendarInterval.", calendarIntervalTriggerSummary)
								.and(timesTriggered("calendarInterval."), fieldWithPath(
										"calendarInterval.preserveHourOfDayAcrossDaylightSavings").description(
												"Whether to fire the trigger at the same time of day, regardless of daylight "
														+ "saving time transitions."),
										fieldWithPath("calendarInterval.skipDayIfHourDoesNotExist").description(
												"Whether to skip if the hour of the day does not exist on a given day."))));
	}

	@Test
	void quartzTriggerDailyTimeInterval() throws Exception {
		setupTriggerDetails(dailyTimeIntervalTrigger.getTriggerBuilder(), TriggerState.PAUSED);
		this.mockMvc.perform(get("/actuator/quartz/triggers/samples/example")).andExpect(status().isOk())
				.andDo(document("quartz/trigger-details-daily-time-interval",
						relaxedResponseFields(fieldWithPath("dailyTimeInterval")
								.description("Daily time interval trigger specific details."))
										.andWithPrefix("dailyTimeInterval.", dailyTimeIntervalTriggerSummary)
										.and(repeatCount("dailyTimeInterval."), timesTriggered("dailyTimeInterval."))));
	}

	@Test
	void quartzTriggerCustom() throws Exception {
		Trigger trigger = mock(Trigger.class);
		given(trigger.getKey()).willReturn(TriggerKey.triggerKey("example", "samples"));
		given(trigger.getDescription()).willReturn("Example trigger.");
		given(trigger.toString()).willReturn("com.example.CustomTrigger@fdsfsd");
		given(trigger.getPriority()).willReturn(10);
		given(trigger.getStartTime()).willReturn(fromUtc("2020-11-30T17:00:00Z"));
		given(trigger.getEndTime()).willReturn(fromUtc("2020-12-30T03:00:00Z"));
		given(trigger.getCalendarName()).willReturn("bankHolidays");
		given(trigger.getPreviousFireTime()).willReturn(fromUtc("2020-12-04T03:00:00Z"));
		given(trigger.getNextFireTime()).willReturn(fromUtc("2020-12-07T03:00:00Z"));
		given(this.scheduler.getTriggerState(trigger.getKey())).willReturn(TriggerState.NORMAL);
		mockTriggers(trigger);
		this.mockMvc.perform(get("/actuator/quartz/triggers/samples/example")).andExpect(status().isOk())
				.andDo(document("quartz/trigger-details-custom",
						relaxedResponseFields(fieldWithPath("custom").description("Custom trigger specific details."))
								.andWithPrefix("custom.", customTriggerSummary)));
	}

	private <T extends Trigger> T setupTriggerDetails(TriggerBuilder<T> builder, TriggerState state)
			throws SchedulerException {
		T trigger = builder.withIdentity("example", "samples").withDescription("Example trigger")
				.startAt(fromUtc("2020-11-30T17:00:00Z")).modifiedByCalendar("bankHolidays")
				.endAt(fromUtc("2020-12-30T03:00:00Z")).build();
		setPreviousNextFireTime(trigger, "2020-12-04T03:00:00Z", "2020-12-07T03:00:00Z");
		given(this.scheduler.getTriggerState(trigger.getKey())).willReturn(state);
		mockTriggers(trigger);
		return trigger;
	}

	private static FieldDescriptor startTime(String prefix) {
		return fieldWithPath(prefix + "startTime").description("Time at which the Trigger should take effect, if any.");
	}

	private static FieldDescriptor endTime(String prefix) {
		return fieldWithPath(prefix + "endTime").description(
				"Time at which the Trigger should quit repeating, regardless of any remaining repeats, if any.");
	}

	private static FieldDescriptor previousFireTime(String prefix) {
		return fieldWithPath(prefix + "previousFireTime").optional().type(JsonFieldType.STRING)
				.description("Last time the trigger fired, if any.");
	}

	private static FieldDescriptor nextFireTime(String prefix) {
		return fieldWithPath(prefix + "nextFireTime").optional().type(JsonFieldType.STRING)
				.description("Next time at which the Trigger is scheduled to fire, if any.");
	}

	private static FieldDescriptor priority(String prefix) {
		return fieldWithPath(prefix + "priority")
				.description("Priority to use if two triggers have the same scheduled fire time.");
	}

	private static FieldDescriptor repeatCount(String prefix) {
		return fieldWithPath(prefix + "repeatCount")
				.description("Number of times the trigger should repeat, or -1 to repeat indefinitely.");
	}

	private static FieldDescriptor timesTriggered(String prefix) {
		return fieldWithPath(prefix + "timesTriggered").description("Number of times the trigger has already fired.");
	}

	private static List<FieldDescriptor> concat(List<FieldDescriptor> initial, List<FieldDescriptor> additionalFields) {
		List<FieldDescriptor> result = new ArrayList<>(initial);
		result.addAll(additionalFields);
		return result;
	}

	private void mockJobs(JobDetail... jobs) throws SchedulerException {
		MultiValueMap<String, JobKey> jobKeys = new LinkedMultiValueMap<>();
		for (JobDetail jobDetail : jobs) {
			JobKey key = jobDetail.getKey();
			given(this.scheduler.getJobDetail(key)).willReturn(jobDetail);
			jobKeys.add(key.getGroup(), key);
		}
		given(this.scheduler.getJobGroupNames()).willReturn(new ArrayList<>(jobKeys.keySet()));
		for (Entry<String, List<JobKey>> entry : jobKeys.entrySet()) {
			given(this.scheduler.getJobKeys(GroupMatcher.jobGroupEquals(entry.getKey())))
					.willReturn(new LinkedHashSet<>(entry.getValue()));
		}
	}

	private void mockTriggers(Trigger... triggers) throws SchedulerException {
		MultiValueMap<String, TriggerKey> triggerKeys = new LinkedMultiValueMap<>();
		for (Trigger trigger : triggers) {
			TriggerKey key = trigger.getKey();
			given(this.scheduler.getTrigger(key)).willReturn(trigger);
			triggerKeys.add(key.getGroup(), key);
		}
		given(this.scheduler.getTriggerGroupNames()).willReturn(new ArrayList<>(triggerKeys.keySet()));
		for (Entry<String, List<TriggerKey>> entry : triggerKeys.entrySet()) {
			given(this.scheduler.getTriggerKeys(GroupMatcher.triggerGroupEquals(entry.getKey())))
					.willReturn(new LinkedHashSet<>(entry.getValue()));
		}
	}

	private <T extends Trigger> T setPreviousNextFireTime(T trigger, String previousFireTime, String nextFireTime) {
		OperableTrigger operableTrigger = (OperableTrigger) trigger;
		if (previousFireTime != null) {
			operableTrigger.setPreviousFireTime(fromUtc(previousFireTime));
		}
		if (nextFireTime != null) {
			operableTrigger.setNextFireTime(fromUtc(nextFireTime));
		}
		return trigger;
	}

	private static Date fromUtc(String utcTime) {
		return Date.from(Instant.parse(utcTime));
	}

	@Configuration(proxyBeanMethods = false)
	@Import(BaseDocumentationConfiguration.class)
	static class TestConfiguration {

		@Bean
		QuartzEndpoint endpoint(Scheduler scheduler) {
			return new QuartzEndpoint(scheduler);
		}

		@Bean
		QuartzEndpointWebExtension endpointWebExtension(QuartzEndpoint endpoint) {
			return new QuartzEndpointWebExtension(endpoint);
		}

	}

}
