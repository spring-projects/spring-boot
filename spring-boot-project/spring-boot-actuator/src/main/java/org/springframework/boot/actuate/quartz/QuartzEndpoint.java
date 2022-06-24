/*
 * Copyright 2012-2022 the original author or authors.
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

import java.time.Duration;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.quartz.CalendarIntervalTrigger;
import org.quartz.CronTrigger;
import org.quartz.DailyTimeIntervalTrigger;
import org.quartz.DateBuilder.IntervalUnit;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.TimeOfDay;
import org.quartz.Trigger;
import org.quartz.Trigger.TriggerState;
import org.quartz.TriggerKey;
import org.quartz.impl.matchers.GroupMatcher;

import org.springframework.boot.actuate.endpoint.Sanitizer;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.util.Assert;

/**
 * {@link Endpoint} to expose Quartz Scheduler jobs and triggers.
 *
 * @author Vedran Pavic
 * @author Stephane Nicoll
 * @since 2.5.0
 */
@Endpoint(id = "quartz")
public class QuartzEndpoint {

	private static final Comparator<Trigger> TRIGGER_COMPARATOR = Comparator
			.comparing(Trigger::getNextFireTime, Comparator.nullsLast(Comparator.naturalOrder()))
			.thenComparing(Comparator.comparingInt(Trigger::getPriority).reversed());

	private final Scheduler scheduler;

	private final Sanitizer sanitizer;

	/**
	 * Create an instance for the specified {@link Scheduler} using a default
	 * {@link Sanitizer}.
	 * @param scheduler the scheduler to use to retrieve jobs and triggers details
	 */
	public QuartzEndpoint(Scheduler scheduler) {
		this(scheduler, new Sanitizer());
	}

	/**
	 * Create an instance for the specified {@link Scheduler} and {@link Sanitizer}.
	 * @param scheduler the scheduler to use to retrieve jobs and triggers details
	 * @param sanitizer the sanitizer to use to sanitize data maps
	 */
	public QuartzEndpoint(Scheduler scheduler, Sanitizer sanitizer) {
		Assert.notNull(scheduler, "Scheduler must not be null");
		Assert.notNull(sanitizer, "Sanitizer must not be null");
		this.scheduler = scheduler;
		this.sanitizer = sanitizer;
	}

	/**
	 * Return the available job and trigger group names.
	 * @return a report of the available group names
	 * @throws SchedulerException if retrieving the information from the scheduler failed
	 */
	@ReadOperation
	public QuartzReport quartzReport() throws SchedulerException {
		return new QuartzReport(new GroupNames(this.scheduler.getJobGroupNames()),
				new GroupNames(this.scheduler.getTriggerGroupNames()));
	}

	/**
	 * Return the available job names, identified by group name.
	 * @return the available job names
	 * @throws SchedulerException if retrieving the information from the scheduler failed
	 */
	public QuartzGroups quartzJobGroups() throws SchedulerException {
		Map<String, Object> result = new LinkedHashMap<>();
		for (String groupName : this.scheduler.getJobGroupNames()) {
			List<String> jobs = this.scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName)).stream()
					.map((key) -> key.getName()).collect(Collectors.toList());
			result.put(groupName, Collections.singletonMap("jobs", jobs));
		}
		return new QuartzGroups(result);
	}

	/**
	 * Return the available trigger names, identified by group name.
	 * @return the available trigger names
	 * @throws SchedulerException if retrieving the information from the scheduler failed
	 */
	public QuartzGroups quartzTriggerGroups() throws SchedulerException {
		Map<String, Object> result = new LinkedHashMap<>();
		Set<String> pausedTriggerGroups = this.scheduler.getPausedTriggerGroups();
		for (String groupName : this.scheduler.getTriggerGroupNames()) {
			Map<String, Object> groupDetails = new LinkedHashMap<>();
			groupDetails.put("paused", pausedTriggerGroups.contains(groupName));
			groupDetails.put("triggers", this.scheduler.getTriggerKeys(GroupMatcher.triggerGroupEquals(groupName))
					.stream().map((key) -> key.getName()).collect(Collectors.toList()));
			result.put(groupName, groupDetails);
		}
		return new QuartzGroups(result);
	}

	/**
	 * Return a summary of the jobs group with the specified name or {@code null} if no
	 * such group exists.
	 * @param group the name of a jobs group
	 * @return a summary of the jobs in the given {@code group}
	 * @throws SchedulerException if retrieving the information from the scheduler failed
	 */
	public QuartzJobGroupSummary quartzJobGroupSummary(String group) throws SchedulerException {
		List<JobDetail> jobs = findJobsByGroup(group);
		if (jobs.isEmpty() && !this.scheduler.getJobGroupNames().contains(group)) {
			return null;
		}
		Map<String, QuartzJobSummary> result = new LinkedHashMap<>();
		for (JobDetail job : jobs) {
			result.put(job.getKey().getName(), QuartzJobSummary.of(job));
		}
		return new QuartzJobGroupSummary(group, result);
	}

	private List<JobDetail> findJobsByGroup(String group) throws SchedulerException {
		List<JobDetail> jobs = new ArrayList<>();
		Set<JobKey> jobKeys = this.scheduler.getJobKeys(GroupMatcher.jobGroupEquals(group));
		for (JobKey jobKey : jobKeys) {
			jobs.add(this.scheduler.getJobDetail(jobKey));
		}
		return jobs;
	}

	/**
	 * Return a summary of the triggers group with the specified name or {@code null} if
	 * no such group exists.
	 * @param group the name of a triggers group
	 * @return a summary of the triggers in the given {@code group}
	 * @throws SchedulerException if retrieving the information from the scheduler failed
	 */
	public QuartzTriggerGroupSummary quartzTriggerGroupSummary(String group) throws SchedulerException {
		List<Trigger> triggers = findTriggersByGroup(group);
		if (triggers.isEmpty() && !this.scheduler.getTriggerGroupNames().contains(group)) {
			return null;
		}
		Map<TriggerType, Map<String, Object>> result = new LinkedHashMap<>();
		triggers.forEach((trigger) -> {
			TriggerDescription triggerDescription = TriggerDescription.of(trigger);
			Map<String, Object> triggerTypes = result.computeIfAbsent(triggerDescription.getType(),
					(key) -> new LinkedHashMap<>());
			triggerTypes.put(trigger.getKey().getName(), triggerDescription.buildSummary(true));
		});
		boolean paused = this.scheduler.getPausedTriggerGroups().contains(group);
		return new QuartzTriggerGroupSummary(group, paused, result);
	}

	private List<Trigger> findTriggersByGroup(String group) throws SchedulerException {
		List<Trigger> triggers = new ArrayList<>();
		Set<TriggerKey> triggerKeys = this.scheduler.getTriggerKeys(GroupMatcher.triggerGroupEquals(group));
		for (TriggerKey triggerKey : triggerKeys) {
			triggers.add(this.scheduler.getTrigger(triggerKey));
		}
		return triggers;
	}

	/**
	 * Return the {@link QuartzJobDetails details of the job} identified with the given
	 * group name and job name.
	 * @param groupName the name of the group
	 * @param jobName the name of the job
	 * @return the details of the job or {@code null} if such job does not exist
	 * @throws SchedulerException if retrieving the information from the scheduler failed
	 */
	public QuartzJobDetails quartzJob(String groupName, String jobName) throws SchedulerException {
		JobKey jobKey = JobKey.jobKey(jobName, groupName);
		JobDetail jobDetail = this.scheduler.getJobDetail(jobKey);
		if (jobDetail != null) {
			List<? extends Trigger> triggers = this.scheduler.getTriggersOfJob(jobKey);
			return new QuartzJobDetails(jobDetail.getKey().getGroup(), jobDetail.getKey().getName(),
					jobDetail.getDescription(), jobDetail.getJobClass().getName(), jobDetail.isDurable(),
					jobDetail.requestsRecovery(), sanitizeJobDataMap(jobDetail.getJobDataMap()),
					extractTriggersSummary(triggers));
		}
		return null;
	}

	private static List<Map<String, Object>> extractTriggersSummary(List<? extends Trigger> triggers) {
		List<Trigger> triggersToSort = new ArrayList<>(triggers);
		triggersToSort.sort(TRIGGER_COMPARATOR);
		List<Map<String, Object>> result = new ArrayList<>();
		triggersToSort.forEach((trigger) -> {
			Map<String, Object> triggerSummary = new LinkedHashMap<>();
			triggerSummary.put("group", trigger.getKey().getGroup());
			triggerSummary.put("name", trigger.getKey().getName());
			triggerSummary.putAll(TriggerDescription.of(trigger).buildSummary(false));
			result.add(triggerSummary);
		});
		return result;
	}

	/**
	 * Return the details of the trigger identified by the given group name and trigger
	 * name.
	 * @param groupName the name of the group
	 * @param triggerName the name of the trigger
	 * @return the details of the trigger or {@code null} if such trigger does not exist
	 * @throws SchedulerException if retrieving the information from the scheduler failed
	 */
	public Map<String, Object> quartzTrigger(String groupName, String triggerName) throws SchedulerException {
		TriggerKey triggerKey = TriggerKey.triggerKey(triggerName, groupName);
		Trigger trigger = this.scheduler.getTrigger(triggerKey);
		return (trigger != null) ? TriggerDescription.of(trigger).buildDetails(
				this.scheduler.getTriggerState(triggerKey), sanitizeJobDataMap(trigger.getJobDataMap())) : null;
	}

	private static Duration getIntervalDuration(long amount, IntervalUnit unit) {
		return temporalUnit(unit).getDuration().multipliedBy(amount);
	}

	private static LocalTime getLocalTime(TimeOfDay timeOfDay) {
		return (timeOfDay != null) ? LocalTime.of(timeOfDay.getHour(), timeOfDay.getMinute(), timeOfDay.getSecond())
				: null;
	}

	private Map<String, Object> sanitizeJobDataMap(JobDataMap dataMap) {
		if (dataMap != null) {
			Map<String, Object> map = new LinkedHashMap<>(dataMap.getWrappedMap());
			map.replaceAll(this.sanitizer::sanitize);
			return map;
		}
		return null;
	}

	private static TemporalUnit temporalUnit(IntervalUnit unit) {
		return switch (unit) {
			case DAY -> ChronoUnit.DAYS;
			case HOUR -> ChronoUnit.HOURS;
			case MINUTE -> ChronoUnit.MINUTES;
			case MONTH -> ChronoUnit.MONTHS;
			case SECOND -> ChronoUnit.SECONDS;
			case MILLISECOND -> ChronoUnit.MILLIS;
			case WEEK -> ChronoUnit.WEEKS;
			case YEAR -> ChronoUnit.YEARS;
		};
	}

	/**
	 * A report of available job and trigger group names, primarily intended for
	 * serialization to JSON.
	 */
	public static final class QuartzReport {

		private final GroupNames jobs;

		private final GroupNames triggers;

		QuartzReport(GroupNames jobs, GroupNames triggers) {
			this.jobs = jobs;
			this.triggers = triggers;
		}

		public GroupNames getJobs() {
			return this.jobs;
		}

		public GroupNames getTriggers() {
			return this.triggers;
		}

	}

	/**
	 * A set of group names, primarily intended for serialization to JSON.
	 */
	public static class GroupNames {

		private final Set<String> groups;

		public GroupNames(List<String> groups) {
			this.groups = new LinkedHashSet<>(groups);
		}

		public Set<String> getGroups() {
			return this.groups;
		}

	}

	/**
	 * A summary for each group identified by name, primarily intended for serialization
	 * to JSON.
	 */
	public static class QuartzGroups {

		private final Map<String, Object> groups;

		public QuartzGroups(Map<String, Object> groups) {
			this.groups = groups;
		}

		public Map<String, Object> getGroups() {
			return this.groups;
		}

	}

	/**
	 * A summary report of the {@link JobDetail jobs} in a given group.
	 */
	public static final class QuartzJobGroupSummary {

		private final String group;

		private final Map<String, QuartzJobSummary> jobs;

		private QuartzJobGroupSummary(String group, Map<String, QuartzJobSummary> jobs) {
			this.group = group;
			this.jobs = jobs;
		}

		public String getGroup() {
			return this.group;
		}

		public Map<String, QuartzJobSummary> getJobs() {
			return this.jobs;
		}

	}

	/**
	 * Details of a {@link Job Quartz Job}, primarily intended for serialization to JSON.
	 */
	public static final class QuartzJobSummary {

		private final String className;

		private QuartzJobSummary(JobDetail job) {
			this.className = job.getJobClass().getName();
		}

		private static QuartzJobSummary of(JobDetail job) {
			return new QuartzJobSummary(job);
		}

		public String getClassName() {
			return this.className;
		}

	}

	/**
	 * Details of a {@link Job Quartz Job}, primarily intended for serialization to JSON.
	 */
	public static final class QuartzJobDetails {

		private final String group;

		private final String name;

		private final String description;

		private final String className;

		private final boolean durable;

		private final boolean requestRecovery;

		private final Map<String, Object> data;

		private final List<Map<String, Object>> triggers;

		QuartzJobDetails(String group, String name, String description, String className, boolean durable,
				boolean requestRecovery, Map<String, Object> data, List<Map<String, Object>> triggers) {
			this.group = group;
			this.name = name;
			this.description = description;
			this.className = className;
			this.durable = durable;
			this.requestRecovery = requestRecovery;
			this.data = data;
			this.triggers = triggers;
		}

		public String getGroup() {
			return this.group;
		}

		public String getName() {
			return this.name;
		}

		public String getDescription() {
			return this.description;
		}

		public String getClassName() {
			return this.className;
		}

		public boolean isDurable() {
			return this.durable;
		}

		public boolean isRequestRecovery() {
			return this.requestRecovery;
		}

		public Map<String, Object> getData() {
			return this.data;
		}

		public List<Map<String, Object>> getTriggers() {
			return this.triggers;
		}

	}

	/**
	 * A summary report of the {@link Trigger triggers} in a given group.
	 */
	public static final class QuartzTriggerGroupSummary {

		private final String group;

		private final boolean paused;

		private final Triggers triggers;

		private QuartzTriggerGroupSummary(String group, boolean paused,
				Map<TriggerType, Map<String, Object>> descriptionsByType) {
			this.group = group;
			this.paused = paused;
			this.triggers = new Triggers(descriptionsByType);

		}

		public String getGroup() {
			return this.group;
		}

		public boolean isPaused() {
			return this.paused;
		}

		public Triggers getTriggers() {
			return this.triggers;
		}

		public static final class Triggers {

			private final Map<String, Object> cron;

			private final Map<String, Object> simple;

			private final Map<String, Object> dailyTimeInterval;

			private final Map<String, Object> calendarInterval;

			private final Map<String, Object> custom;

			private Triggers(Map<TriggerType, Map<String, Object>> descriptionsByType) {
				this.cron = descriptionsByType.getOrDefault(TriggerType.CRON, Collections.emptyMap());
				this.dailyTimeInterval = descriptionsByType.getOrDefault(TriggerType.DAILY_INTERVAL,
						Collections.emptyMap());
				this.calendarInterval = descriptionsByType.getOrDefault(TriggerType.CALENDAR_INTERVAL,
						Collections.emptyMap());
				this.simple = descriptionsByType.getOrDefault(TriggerType.SIMPLE, Collections.emptyMap());
				this.custom = descriptionsByType.getOrDefault(TriggerType.CUSTOM_TRIGGER, Collections.emptyMap());
			}

			public Map<String, Object> getCron() {
				return this.cron;
			}

			public Map<String, Object> getSimple() {
				return this.simple;
			}

			public Map<String, Object> getDailyTimeInterval() {
				return this.dailyTimeInterval;
			}

			public Map<String, Object> getCalendarInterval() {
				return this.calendarInterval;
			}

			public Map<String, Object> getCustom() {
				return this.custom;
			}

		}

	}

	private enum TriggerType {

		CRON("cron"),

		CUSTOM_TRIGGER("custom"),

		CALENDAR_INTERVAL("calendarInterval"),

		DAILY_INTERVAL("dailyTimeInterval"),

		SIMPLE("simple");

		private final String id;

		TriggerType(String id) {
			this.id = id;
		}

		public String getId() {
			return this.id;
		}

	}

	/**
	 * Base class for descriptions of a {@link Trigger}.
	 */
	public abstract static class TriggerDescription {

		private static final Map<Class<? extends Trigger>, Function<Trigger, TriggerDescription>> DESCRIBERS = new LinkedHashMap<>();

		static {
			DESCRIBERS.put(CronTrigger.class, (trigger) -> new CronTriggerDescription((CronTrigger) trigger));
			DESCRIBERS.put(SimpleTrigger.class, (trigger) -> new SimpleTriggerDescription((SimpleTrigger) trigger));
			DESCRIBERS.put(DailyTimeIntervalTrigger.class,
					(trigger) -> new DailyTimeIntervalTriggerDescription((DailyTimeIntervalTrigger) trigger));
			DESCRIBERS.put(CalendarIntervalTrigger.class,
					(trigger) -> new CalendarIntervalTriggerDescription((CalendarIntervalTrigger) trigger));
		}

		private final Trigger trigger;

		private final TriggerType type;

		private static TriggerDescription of(Trigger trigger) {
			return DESCRIBERS.entrySet().stream().filter((entry) -> entry.getKey().isInstance(trigger))
					.map((entry) -> entry.getValue().apply(trigger)).findFirst()
					.orElse(new CustomTriggerDescription(trigger));
		}

		protected TriggerDescription(Trigger trigger, TriggerType type) {
			this.trigger = trigger;
			this.type = type;
		}

		/**
		 * Build the summary of the trigger.
		 * @param addTriggerSpecificSummary whether to add trigger-implementation specific
		 * summary.
		 * @return basic properties of the trigger
		 */
		public Map<String, Object> buildSummary(boolean addTriggerSpecificSummary) {
			Map<String, Object> summary = new LinkedHashMap<>();
			putIfNoNull(summary, "previousFireTime", this.trigger.getPreviousFireTime());
			putIfNoNull(summary, "nextFireTime", this.trigger.getNextFireTime());
			summary.put("priority", this.trigger.getPriority());
			if (addTriggerSpecificSummary) {
				appendSummary(summary);
			}
			return summary;
		}

		/**
		 * Append trigger-implementation specific summary items to the specified
		 * {@code content}.
		 * @param content the summary of the trigger
		 */
		protected abstract void appendSummary(Map<String, Object> content);

		/**
		 * Build the full details of the trigger.
		 * @param triggerState the current state of the trigger
		 * @param sanitizedDataMap a sanitized data map or {@code null}
		 * @return all properties of the trigger
		 */
		public Map<String, Object> buildDetails(TriggerState triggerState, Map<String, Object> sanitizedDataMap) {
			Map<String, Object> details = new LinkedHashMap<>();
			details.put("group", this.trigger.getKey().getGroup());
			details.put("name", this.trigger.getKey().getName());
			putIfNoNull(details, "description", this.trigger.getDescription());
			details.put("state", triggerState);
			details.put("type", getType().getId());
			putIfNoNull(details, "calendarName", this.trigger.getCalendarName());
			putIfNoNull(details, "startTime", this.trigger.getStartTime());
			putIfNoNull(details, "endTime", this.trigger.getEndTime());
			putIfNoNull(details, "previousFireTime", this.trigger.getPreviousFireTime());
			putIfNoNull(details, "nextFireTime", this.trigger.getNextFireTime());
			putIfNoNull(details, "priority", this.trigger.getPriority());
			putIfNoNull(details, "finalFireTime", this.trigger.getFinalFireTime());
			putIfNoNull(details, "data", sanitizedDataMap);
			Map<String, Object> typeDetails = new LinkedHashMap<>();
			appendDetails(typeDetails);
			details.put(getType().getId(), typeDetails);
			return details;
		}

		/**
		 * Append trigger-implementation specific details to the specified
		 * {@code content}.
		 * @param content the details of the trigger
		 */
		protected abstract void appendDetails(Map<String, Object> content);

		protected void putIfNoNull(Map<String, Object> content, String key, Object value) {
			if (value != null) {
				content.put(key, value);
			}
		}

		protected Trigger getTrigger() {
			return this.trigger;
		}

		protected TriggerType getType() {
			return this.type;
		}

	}

	/**
	 * A description of a {@link CronTrigger}.
	 */
	public static final class CronTriggerDescription extends TriggerDescription {

		private final CronTrigger trigger;

		public CronTriggerDescription(CronTrigger trigger) {
			super(trigger, TriggerType.CRON);
			this.trigger = trigger;
		}

		@Override
		protected void appendSummary(Map<String, Object> content) {
			content.put("expression", this.trigger.getCronExpression());
			putIfNoNull(content, "timeZone", this.trigger.getTimeZone());
		}

		@Override
		protected void appendDetails(Map<String, Object> content) {
			appendSummary(content);
		}

	}

	/**
	 * A description of a {@link SimpleTrigger}.
	 */
	public static final class SimpleTriggerDescription extends TriggerDescription {

		private final SimpleTrigger trigger;

		public SimpleTriggerDescription(SimpleTrigger trigger) {
			super(trigger, TriggerType.SIMPLE);
			this.trigger = trigger;
		}

		@Override
		protected void appendSummary(Map<String, Object> content) {
			content.put("interval", this.trigger.getRepeatInterval());
		}

		@Override
		protected void appendDetails(Map<String, Object> content) {
			appendSummary(content);
			content.put("repeatCount", this.trigger.getRepeatCount());
			content.put("timesTriggered", this.trigger.getTimesTriggered());
		}

	}

	/**
	 * A description of a {@link DailyTimeIntervalTrigger}.
	 */
	public static final class DailyTimeIntervalTriggerDescription extends TriggerDescription {

		private final DailyTimeIntervalTrigger trigger;

		public DailyTimeIntervalTriggerDescription(DailyTimeIntervalTrigger trigger) {
			super(trigger, TriggerType.DAILY_INTERVAL);
			this.trigger = trigger;
		}

		@Override
		protected void appendSummary(Map<String, Object> content) {
			content.put("interval",
					getIntervalDuration(this.trigger.getRepeatInterval(), this.trigger.getRepeatIntervalUnit())
							.toMillis());
			putIfNoNull(content, "daysOfWeek", this.trigger.getDaysOfWeek());
			putIfNoNull(content, "startTimeOfDay", getLocalTime(this.trigger.getStartTimeOfDay()));
			putIfNoNull(content, "endTimeOfDay", getLocalTime(this.trigger.getEndTimeOfDay()));
		}

		@Override
		protected void appendDetails(Map<String, Object> content) {
			appendSummary(content);
			content.put("repeatCount", this.trigger.getRepeatCount());
			content.put("timesTriggered", this.trigger.getTimesTriggered());
		}

	}

	/**
	 * A description of a {@link CalendarIntervalTrigger}.
	 */
	public static final class CalendarIntervalTriggerDescription extends TriggerDescription {

		private final CalendarIntervalTrigger trigger;

		public CalendarIntervalTriggerDescription(CalendarIntervalTrigger trigger) {
			super(trigger, TriggerType.CALENDAR_INTERVAL);
			this.trigger = trigger;
		}

		@Override
		protected void appendSummary(Map<String, Object> content) {
			content.put("interval",
					getIntervalDuration(this.trigger.getRepeatInterval(), this.trigger.getRepeatIntervalUnit())
							.toMillis());
			putIfNoNull(content, "timeZone", this.trigger.getTimeZone());
		}

		@Override
		protected void appendDetails(Map<String, Object> content) {
			appendSummary(content);
			content.put("timesTriggered", this.trigger.getTimesTriggered());
			content.put("preserveHourOfDayAcrossDaylightSavings",
					this.trigger.isPreserveHourOfDayAcrossDaylightSavings());
			content.put("skipDayIfHourDoesNotExist", this.trigger.isSkipDayIfHourDoesNotExist());
		}

	}

	/**
	 * A description of a custom {@link Trigger}.
	 */
	public static final class CustomTriggerDescription extends TriggerDescription {

		public CustomTriggerDescription(Trigger trigger) {
			super(trigger, TriggerType.CUSTOM_TRIGGER);
		}

		@Override
		protected void appendSummary(Map<String, Object> content) {
			content.put("trigger", getTrigger().toString());
		}

		@Override
		protected void appendDetails(Map<String, Object> content) {
			appendSummary(content);
		}

	}

}
