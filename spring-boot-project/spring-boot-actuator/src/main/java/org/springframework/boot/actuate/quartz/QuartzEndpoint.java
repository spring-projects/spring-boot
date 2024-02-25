/*
 * Copyright 2012-2023 the original author or authors.
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

import org.springframework.boot.actuate.endpoint.OperationResponseBody;
import org.springframework.boot.actuate.endpoint.SanitizableData;
import org.springframework.boot.actuate.endpoint.Sanitizer;
import org.springframework.boot.actuate.endpoint.SanitizingFunction;
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
	 * Constructs a new QuartzEndpoint with the specified scheduler and sanitizing
	 * functions.
	 * @param scheduler the scheduler to be used by the endpoint (must not be null)
	 * @param sanitizingFunctions the sanitizing functions to be used by the endpoint
	 * @throws IllegalArgumentException if the scheduler is null
	 */
	public QuartzEndpoint(Scheduler scheduler, Iterable<SanitizingFunction> sanitizingFunctions) {
		Assert.notNull(scheduler, "Scheduler must not be null");
		this.scheduler = scheduler;
		this.sanitizer = new Sanitizer(sanitizingFunctions);
	}

	/**
	 * Return the available job and trigger group names.
	 * @return a report of the available group names
	 * @throws SchedulerException if retrieving the information from the scheduler failed
	 */
	@ReadOperation
	public QuartzDescriptor quartzReport() throws SchedulerException {
		return new QuartzDescriptor(new GroupNamesDescriptor(this.scheduler.getJobGroupNames()),
				new GroupNamesDescriptor(this.scheduler.getTriggerGroupNames()));
	}

	/**
	 * Return the available job names, identified by group name.
	 * @return the available job names
	 * @throws SchedulerException if retrieving the information from the scheduler failed
	 */
	public QuartzGroupsDescriptor quartzJobGroups() throws SchedulerException {
		Map<String, Object> result = new LinkedHashMap<>();
		for (String groupName : this.scheduler.getJobGroupNames()) {
			List<String> jobs = this.scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName))
				.stream()
				.map((key) -> key.getName())
				.toList();
			result.put(groupName, Collections.singletonMap("jobs", jobs));
		}
		return new QuartzGroupsDescriptor(result);
	}

	/**
	 * Return the available trigger names, identified by group name.
	 * @return the available trigger names
	 * @throws SchedulerException if retrieving the information from the scheduler failed
	 */
	public QuartzGroupsDescriptor quartzTriggerGroups() throws SchedulerException {
		Map<String, Object> result = new LinkedHashMap<>();
		Set<String> pausedTriggerGroups = this.scheduler.getPausedTriggerGroups();
		for (String groupName : this.scheduler.getTriggerGroupNames()) {
			Map<String, Object> groupDetails = new LinkedHashMap<>();
			groupDetails.put("paused", pausedTriggerGroups.contains(groupName));
			groupDetails.put("triggers",
					this.scheduler.getTriggerKeys(GroupMatcher.triggerGroupEquals(groupName))
						.stream()
						.map((key) -> key.getName())
						.toList());
			result.put(groupName, groupDetails);
		}
		return new QuartzGroupsDescriptor(result);
	}

	/**
	 * Return a summary of the jobs group with the specified name or {@code null} if no
	 * such group exists.
	 * @param group the name of a jobs group
	 * @return a summary of the jobs in the given {@code group}
	 * @throws SchedulerException if retrieving the information from the scheduler failed
	 */
	public QuartzJobGroupSummaryDescriptor quartzJobGroupSummary(String group) throws SchedulerException {
		List<JobDetail> jobs = findJobsByGroup(group);
		if (jobs.isEmpty() && !this.scheduler.getJobGroupNames().contains(group)) {
			return null;
		}
		Map<String, QuartzJobSummaryDescriptor> result = new LinkedHashMap<>();
		for (JobDetail job : jobs) {
			result.put(job.getKey().getName(), QuartzJobSummaryDescriptor.of(job));
		}
		return new QuartzJobGroupSummaryDescriptor(group, result);
	}

	/**
	 * Finds jobs by group.
	 * @param group the group name of the jobs to find
	 * @return a list of JobDetail objects belonging to the specified group
	 * @throws SchedulerException if there is an error accessing the scheduler
	 */
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
	public QuartzTriggerGroupSummaryDescriptor quartzTriggerGroupSummary(String group) throws SchedulerException {
		List<Trigger> triggers = findTriggersByGroup(group);
		if (triggers.isEmpty() && !this.scheduler.getTriggerGroupNames().contains(group)) {
			return null;
		}
		Map<TriggerType, Map<String, Object>> result = new LinkedHashMap<>();
		triggers.forEach((trigger) -> {
			TriggerDescriptor triggerDescriptor = TriggerDescriptor.of(trigger);
			Map<String, Object> triggerTypes = result.computeIfAbsent(triggerDescriptor.getType(),
					(key) -> new LinkedHashMap<>());
			triggerTypes.put(trigger.getKey().getName(), triggerDescriptor.buildSummary(true));
		});
		boolean paused = this.scheduler.getPausedTriggerGroups().contains(group);
		return new QuartzTriggerGroupSummaryDescriptor(group, paused, result);
	}

	/**
	 * Finds triggers by group.
	 * @param group the group name of the triggers to find
	 * @return a list of triggers belonging to the specified group
	 * @throws SchedulerException if there is an error accessing the scheduler
	 */
	private List<Trigger> findTriggersByGroup(String group) throws SchedulerException {
		List<Trigger> triggers = new ArrayList<>();
		Set<TriggerKey> triggerKeys = this.scheduler.getTriggerKeys(GroupMatcher.triggerGroupEquals(group));
		for (TriggerKey triggerKey : triggerKeys) {
			triggers.add(this.scheduler.getTrigger(triggerKey));
		}
		return triggers;
	}

	/**
	 * Return the {@link QuartzJobDetailsDescriptor details of the job} identified with
	 * the given group name and job name.
	 * @param groupName the name of the group
	 * @param jobName the name of the job
	 * @param showUnsanitized whether to sanitize values in data map
	 * @return the details of the job or {@code null} if such job does not exist
	 * @throws SchedulerException if retrieving the information from the scheduler failed
	 */
	public QuartzJobDetailsDescriptor quartzJob(String groupName, String jobName, boolean showUnsanitized)
			throws SchedulerException {
		JobKey jobKey = JobKey.jobKey(jobName, groupName);
		JobDetail jobDetail = this.scheduler.getJobDetail(jobKey);
		if (jobDetail != null) {
			List<? extends Trigger> triggers = this.scheduler.getTriggersOfJob(jobKey);
			return new QuartzJobDetailsDescriptor(jobDetail.getKey().getGroup(), jobDetail.getKey().getName(),
					jobDetail.getDescription(), jobDetail.getJobClass().getName(), jobDetail.isDurable(),
					jobDetail.requestsRecovery(), sanitizeJobDataMap(jobDetail.getJobDataMap(), showUnsanitized),
					extractTriggersSummary(triggers));
		}
		return null;
	}

	/**
	 * Extracts the summary information of the given list of triggers. The triggers are
	 * sorted based on the trigger comparator. The summary information includes the
	 * trigger's group, name, and additional details provided by the trigger descriptor.
	 * @param triggers the list of triggers to extract the summary information from
	 * @return a list of maps containing the summary information of the triggers
	 */
	private static List<Map<String, Object>> extractTriggersSummary(List<? extends Trigger> triggers) {
		List<Trigger> triggersToSort = new ArrayList<>(triggers);
		triggersToSort.sort(TRIGGER_COMPARATOR);
		List<Map<String, Object>> result = new ArrayList<>();
		triggersToSort.forEach((trigger) -> {
			Map<String, Object> triggerSummary = new LinkedHashMap<>();
			triggerSummary.put("group", trigger.getKey().getGroup());
			triggerSummary.put("name", trigger.getKey().getName());
			triggerSummary.putAll(TriggerDescriptor.of(trigger).buildSummary(false));
			result.add(triggerSummary);
		});
		return result;
	}

	/**
	 * Return the details of the trigger identified by the given group name and trigger
	 * name.
	 * @param groupName the name of the group
	 * @param triggerName the name of the trigger
	 * @param showUnsanitized whether to sanitize values in data map
	 * @return the details of the trigger or {@code null} if such trigger does not exist
	 * @throws SchedulerException if retrieving the information from the scheduler failed
	 */
	Map<String, Object> quartzTrigger(String groupName, String triggerName, boolean showUnsanitized)
			throws SchedulerException {
		TriggerKey triggerKey = TriggerKey.triggerKey(triggerName, groupName);
		Trigger trigger = this.scheduler.getTrigger(triggerKey);
		if (trigger == null) {
			return null;
		}
		TriggerState triggerState = this.scheduler.getTriggerState(triggerKey);
		TriggerDescriptor triggerDescriptor = TriggerDescriptor.of(trigger);
		Map<String, Object> jobDataMap = sanitizeJobDataMap(trigger.getJobDataMap(), showUnsanitized);
		return OperationResponseBody.of(triggerDescriptor.buildDetails(triggerState, jobDataMap));
	}

	/**
	 * Returns the duration of the interval based on the specified amount and unit.
	 * @param amount the amount of the interval
	 * @param unit the unit of the interval
	 * @return the duration of the interval
	 */
	private static Duration getIntervalDuration(long amount, IntervalUnit unit) {
		return temporalUnit(unit).getDuration().multipliedBy(amount);
	}

	/**
	 * Returns the LocalTime representation of the given TimeOfDay object.
	 * @param timeOfDay the TimeOfDay object to convert
	 * @return the LocalTime representation of the given TimeOfDay object, or null if the
	 * input is null
	 */
	private static LocalTime getLocalTime(TimeOfDay timeOfDay) {
		return (timeOfDay != null) ? LocalTime.of(timeOfDay.getHour(), timeOfDay.getMinute(), timeOfDay.getSecond())
				: null;
	}

	/**
	 * Sanitizes the job data map by replacing sensitive values with sanitized values.
	 * @param dataMap the job data map to be sanitized
	 * @param showUnsanitized flag indicating whether unsanitized values should be shown
	 * @return a sanitized job data map with sensitive values replaced by sanitized values
	 */
	private Map<String, Object> sanitizeJobDataMap(JobDataMap dataMap, boolean showUnsanitized) {
		if (dataMap != null) {
			Map<String, Object> map = new LinkedHashMap<>(dataMap.getWrappedMap());
			map.replaceAll((key, value) -> getSanitizedValue(showUnsanitized, key, value));
			return map;
		}
		return null;
	}

	/**
	 * Returns the sanitized value of the given key-value pair.
	 * @param showUnsanitized a boolean indicating whether to show unsanitized values
	 * @param key the key of the data to be sanitized
	 * @param value the value of the data to be sanitized
	 * @return the sanitized value of the given key-value pair
	 */
	private Object getSanitizedValue(boolean showUnsanitized, String key, Object value) {
		SanitizableData data = new SanitizableData(null, key, value);
		return this.sanitizer.sanitize(data, showUnsanitized);
	}

	/**
	 * Returns the corresponding TemporalUnit for the given IntervalUnit.
	 * @param unit the IntervalUnit to get the corresponding TemporalUnit for
	 * @return the corresponding TemporalUnit for the given IntervalUnit
	 */
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
	 * Description of available job and trigger group names.
	 */
	public static final class QuartzDescriptor implements OperationResponseBody {

		private final GroupNamesDescriptor jobs;

		private final GroupNamesDescriptor triggers;

		/**
		 * Constructs a new QuartzDescriptor object with the specified jobs and triggers.
		 * @param jobs the descriptor for the group names of jobs
		 * @param triggers the descriptor for the group names of triggers
		 */
		QuartzDescriptor(GroupNamesDescriptor jobs, GroupNamesDescriptor triggers) {
			this.jobs = jobs;
			this.triggers = triggers;
		}

		/**
		 * Returns the GroupNamesDescriptor object representing the jobs.
		 * @return the GroupNamesDescriptor object representing the jobs
		 */
		public GroupNamesDescriptor getJobs() {
			return this.jobs;
		}

		/**
		 * Returns the triggers associated with this QuartzDescriptor.
		 * @return the triggers associated with this QuartzDescriptor
		 */
		public GroupNamesDescriptor getTriggers() {
			return this.triggers;
		}

	}

	/**
	 * Description of group names.
	 */
	public static class GroupNamesDescriptor {

		private final Set<String> groups;

		/**
		 * Constructs a new GroupNamesDescriptor object with the specified list of groups.
		 * @param groups the list of groups to be used for constructing the
		 * GroupNamesDescriptor object
		 */
		public GroupNamesDescriptor(List<String> groups) {
			this.groups = new LinkedHashSet<>(groups);
		}

		/**
		 * Returns the set of groups.
		 * @return the set of groups
		 */
		public Set<String> getGroups() {
			return this.groups;
		}

	}

	/**
	 * Description of each group identified by name.
	 */
	public static class QuartzGroupsDescriptor implements OperationResponseBody {

		private final Map<String, Object> groups;

		/**
		 * Constructs a new QuartzGroupsDescriptor with the specified groups.
		 * @param groups a map containing the groups
		 */
		public QuartzGroupsDescriptor(Map<String, Object> groups) {
			this.groups = groups;
		}

		/**
		 * Returns the groups associated with the QuartzGroupsDescriptor.
		 * @return a Map containing the groups as keys and associated objects as values
		 */
		public Map<String, Object> getGroups() {
			return this.groups;
		}

	}

	/**
	 * Description of the {@link JobDetail jobs} in a given group.
	 */
	public static final class QuartzJobGroupSummaryDescriptor implements OperationResponseBody {

		private final String group;

		private final Map<String, QuartzJobSummaryDescriptor> jobs;

		/**
		 * Constructs a new QuartzJobGroupSummaryDescriptor with the specified group and
		 * jobs.
		 * @param group the name of the job group
		 * @param jobs a map containing job names as keys and QuartzJobSummaryDescriptor
		 * objects as values
		 */
		private QuartzJobGroupSummaryDescriptor(String group, Map<String, QuartzJobSummaryDescriptor> jobs) {
			this.group = group;
			this.jobs = jobs;
		}

		/**
		 * Returns the group associated with this QuartzJobGroupSummaryDescriptor.
		 * @return the group associated with this QuartzJobGroupSummaryDescriptor
		 */
		public String getGroup() {
			return this.group;
		}

		/**
		 * Returns a map of job names to their corresponding QuartzJobSummaryDescriptor
		 * objects.
		 * @return the map of job names to QuartzJobSummaryDescriptor objects
		 */
		public Map<String, QuartzJobSummaryDescriptor> getJobs() {
			return this.jobs;
		}

	}

	/**
	 * Description of a {@link Job Quartz Job}.
	 */
	public static final class QuartzJobSummaryDescriptor {

		private final String className;

		/**
		 * Constructs a new QuartzJobSummaryDescriptor object with the given JobDetail.
		 * @param job the JobDetail object to create the descriptor from
		 */
		private QuartzJobSummaryDescriptor(JobDetail job) {
			this.className = job.getJobClass().getName();
		}

		/**
		 * Creates a new QuartzJobSummaryDescriptor object based on the provided
		 * JobDetail.
		 * @param job the JobDetail object to create the QuartzJobSummaryDescriptor from
		 * @return a new QuartzJobSummaryDescriptor object
		 */
		private static QuartzJobSummaryDescriptor of(JobDetail job) {
			return new QuartzJobSummaryDescriptor(job);
		}

		/**
		 * Returns the name of the class.
		 * @return the name of the class
		 */
		public String getClassName() {
			return this.className;
		}

	}

	/**
	 * Description of a {@link Job Quartz Job}.
	 */
	public static final class QuartzJobDetailsDescriptor implements OperationResponseBody {

		private final String group;

		private final String name;

		private final String description;

		private final String className;

		private final boolean durable;

		private final boolean requestRecovery;

		private final Map<String, Object> data;

		private final List<Map<String, Object>> triggers;

		/**
		 * Constructs a new QuartzJobDetailsDescriptor with the specified parameters.
		 * @param group the group of the job
		 * @param name the name of the job
		 * @param description the description of the job
		 * @param className the fully qualified class name of the job
		 * @param durable true if the job should be persisted, false otherwise
		 * @param requestRecovery true if the job should be re-executed if a 'recovery' or
		 * 'fail-over' situation is encountered, false otherwise
		 * @param data a map of additional data associated with the job
		 * @param triggers a list of triggers associated with the job
		 */
		QuartzJobDetailsDescriptor(String group, String name, String description, String className, boolean durable,
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

		/**
		 * Returns the group of the QuartzJobDetailsDescriptor.
		 * @return the group of the QuartzJobDetailsDescriptor
		 */
		public String getGroup() {
			return this.group;
		}

		/**
		 * Returns the name of the QuartzJobDetailsDescriptor.
		 * @return the name of the QuartzJobDetailsDescriptor
		 */
		public String getName() {
			return this.name;
		}

		/**
		 * Returns the description of the QuartzJobDetailsDescriptor.
		 * @return the description of the QuartzJobDetailsDescriptor
		 */
		public String getDescription() {
			return this.description;
		}

		/**
		 * Returns the name of the class.
		 * @return the name of the class
		 */
		public String getClassName() {
			return this.className;
		}

		/**
		 * Returns a boolean value indicating if the job is durable.
		 * @return true if the job is durable, false otherwise
		 */
		public boolean isDurable() {
			return this.durable;
		}

		/**
		 * Returns whether the job should be re-executed if a 'recovery' or 'fail-over'
		 * situation is encountered.
		 * @return true if the job should be re-executed in case of recovery or fail-over,
		 * false otherwise
		 */
		public boolean isRequestRecovery() {
			return this.requestRecovery;
		}

		/**
		 * Returns the data associated with the QuartzJobDetailsDescriptor.
		 * @return a Map containing the data
		 */
		public Map<String, Object> getData() {
			return this.data;
		}

		/**
		 * Returns the list of triggers associated with this QuartzJobDetailsDescriptor.
		 * @return the list of triggers as a List of Map objects, where each Map
		 * represents a trigger with its properties
		 */
		public List<Map<String, Object>> getTriggers() {
			return this.triggers;
		}

	}

	/**
	 * Description of the {@link Trigger triggers} in a given group.
	 */
	public static final class QuartzTriggerGroupSummaryDescriptor implements OperationResponseBody {

		private final String group;

		private final boolean paused;

		private final Triggers triggers;

		/**
		 * Constructs a new instance of the {@code QuartzTriggerGroupSummaryDescriptor}
		 * class with the specified parameters.
		 * @param group the name of the trigger group
		 * @param paused {@code true} if the trigger group is paused, {@code false}
		 * otherwise
		 * @param descriptionsByType a map containing trigger type descriptions for each
		 * trigger type in the group
		 */
		private QuartzTriggerGroupSummaryDescriptor(String group, boolean paused,
				Map<TriggerType, Map<String, Object>> descriptionsByType) {
			this.group = group;
			this.paused = paused;
			this.triggers = new Triggers(descriptionsByType);

		}

		/**
		 * Returns the group of the QuartzTriggerGroupSummaryDescriptor.
		 * @return the group of the QuartzTriggerGroupSummaryDescriptor
		 */
		public String getGroup() {
			return this.group;
		}

		/**
		 * Returns a boolean value indicating whether the trigger group is paused.
		 * @return true if the trigger group is paused, false otherwise
		 */
		public boolean isPaused() {
			return this.paused;
		}

		/**
		 * Returns the triggers associated with this QuartzTriggerGroupSummaryDescriptor.
		 * @return the triggers associated with this QuartzTriggerGroupSummaryDescriptor
		 */
		public Triggers getTriggers() {
			return this.triggers;
		}

		/**
		 * Triggers class.
		 */
		public static final class Triggers {

			private final Map<String, Object> cron;

			private final Map<String, Object> simple;

			private final Map<String, Object> dailyTimeInterval;

			private final Map<String, Object> calendarInterval;

			private final Map<String, Object> custom;

			/**
			 * Constructs a new Triggers object with the provided descriptions by type.
			 * @param descriptionsByType a map containing trigger descriptions categorized
			 * by trigger type
			 * @see TriggerType
			 */
			private Triggers(Map<TriggerType, Map<String, Object>> descriptionsByType) {
				this.cron = descriptionsByType.getOrDefault(TriggerType.CRON, Collections.emptyMap());
				this.dailyTimeInterval = descriptionsByType.getOrDefault(TriggerType.DAILY_INTERVAL,
						Collections.emptyMap());
				this.calendarInterval = descriptionsByType.getOrDefault(TriggerType.CALENDAR_INTERVAL,
						Collections.emptyMap());
				this.simple = descriptionsByType.getOrDefault(TriggerType.SIMPLE, Collections.emptyMap());
				this.custom = descriptionsByType.getOrDefault(TriggerType.CUSTOM_TRIGGER, Collections.emptyMap());
			}

			/**
			 * Returns the cron map.
			 * @return the cron map
			 */
			public Map<String, Object> getCron() {
				return this.cron;
			}

			/**
			 * Retrieves the simple map.
			 * @return the simple map containing string keys and object values
			 */
			public Map<String, Object> getSimple() {
				return this.simple;
			}

			/**
			 * Returns the daily time interval for the trigger.
			 * @return the daily time interval as a Map<String, Object>
			 */
			public Map<String, Object> getDailyTimeInterval() {
				return this.dailyTimeInterval;
			}

			/**
			 * Returns the calendar interval of the trigger.
			 * @return the calendar interval as a Map<String, Object>
			 */
			public Map<String, Object> getCalendarInterval() {
				return this.calendarInterval;
			}

			/**
			 * Returns the custom map.
			 * @return the custom map
			 */
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

		/**
		 * Constructs a new instance of the TriggerType class with the specified ID.
		 * @param id the ID of the trigger type
		 */
		TriggerType(String id) {
			this.id = id;
		}

		/**
		 * Returns the ID of the QuartzEndpoint.
		 * @return the ID of the QuartzEndpoint
		 */
		public String getId() {
			return this.id;
		}

	}

	/**
	 * Base class for descriptions of a {@link Trigger}.
	 */
	public abstract static class TriggerDescriptor {

		private static final Map<Class<? extends Trigger>, Function<Trigger, TriggerDescriptor>> DESCRIBERS = new LinkedHashMap<>();

		static {
			DESCRIBERS.put(CronTrigger.class, (trigger) -> new CronTriggerDescriptor((CronTrigger) trigger));
			DESCRIBERS.put(SimpleTrigger.class, (trigger) -> new SimpleTriggerDescriptor((SimpleTrigger) trigger));
			DESCRIBERS.put(DailyTimeIntervalTrigger.class,
					(trigger) -> new DailyTimeIntervalTriggerDescriptor((DailyTimeIntervalTrigger) trigger));
			DESCRIBERS.put(CalendarIntervalTrigger.class,
					(trigger) -> new CalendarIntervalTriggerDescriptor((CalendarIntervalTrigger) trigger));
		}

		private final Trigger trigger;

		private final TriggerType type;

		/**
		 * Creates a TriggerDescriptor object based on the given Trigger.
		 * @param trigger the Trigger object to create the TriggerDescriptor from
		 * @return the TriggerDescriptor object created from the given Trigger
		 */
		private static TriggerDescriptor of(Trigger trigger) {
			return DESCRIBERS.entrySet()
				.stream()
				.filter((entry) -> entry.getKey().isInstance(trigger))
				.map((entry) -> entry.getValue().apply(trigger))
				.findFirst()
				.orElse(new CustomTriggerDescriptor(trigger));
		}

		/**
		 * Constructs a new TriggerDescriptor with the specified trigger and type.
		 * @param trigger the trigger associated with the descriptor
		 * @param type the type of the trigger
		 */
		protected TriggerDescriptor(Trigger trigger, TriggerType type) {
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

		/**
		 * Puts the specified key-value pair into the given map if the value is not null.
		 * @param content the map to put the key-value pair into
		 * @param key the key to be associated with the value
		 * @param value the value to be put into the map
		 */
		protected void putIfNoNull(Map<String, Object> content, String key, Object value) {
			if (value != null) {
				content.put(key, value);
			}
		}

		/**
		 * Returns the trigger associated with this TriggerDescriptor.
		 * @return the trigger associated with this TriggerDescriptor
		 */
		protected Trigger getTrigger() {
			return this.trigger;
		}

		/**
		 * Returns the type of the trigger.
		 * @return the type of the trigger
		 */
		protected TriggerType getType() {
			return this.type;
		}

	}

	/**
	 * Description of a {@link CronTrigger}.
	 */
	public static final class CronTriggerDescriptor extends TriggerDescriptor {

		private final CronTrigger trigger;

		/**
		 * Constructs a new CronTriggerDescriptor object with the specified CronTrigger.
		 * @param trigger the CronTrigger object to be used
		 */
		public CronTriggerDescriptor(CronTrigger trigger) {
			super(trigger, TriggerType.CRON);
			this.trigger = trigger;
		}

		/**
		 * Appends the summary of the cron trigger descriptor to the given content map.
		 * @param content the map to append the summary to
		 */
		@Override
		protected void appendSummary(Map<String, Object> content) {
			content.put("expression", this.trigger.getCronExpression());
			putIfNoNull(content, "timeZone", this.trigger.getTimeZone());
		}

		/**
		 * Appends details to the given content map.
		 * @param content the map to append details to
		 */
		@Override
		protected void appendDetails(Map<String, Object> content) {
			appendSummary(content);
		}

	}

	/**
	 * Description of a {@link SimpleTrigger}.
	 */
	public static final class SimpleTriggerDescriptor extends TriggerDescriptor {

		private final SimpleTrigger trigger;

		/**
		 * Constructs a new SimpleTriggerDescriptor with the specified SimpleTrigger.
		 * @param trigger the SimpleTrigger to be associated with the descriptor
		 */
		public SimpleTriggerDescriptor(SimpleTrigger trigger) {
			super(trigger, TriggerType.SIMPLE);
			this.trigger = trigger;
		}

		/**
		 * Appends the summary of the SimpleTriggerDescriptor to the provided content map.
		 * @param content the map to append the summary to
		 */
		@Override
		protected void appendSummary(Map<String, Object> content) {
			content.put("interval", this.trigger.getRepeatInterval());
		}

		/**
		 * Appends additional details to the given content map.
		 * @param content the content map to append details to
		 */
		@Override
		protected void appendDetails(Map<String, Object> content) {
			appendSummary(content);
			content.put("repeatCount", this.trigger.getRepeatCount());
			content.put("timesTriggered", this.trigger.getTimesTriggered());
		}

	}

	/**
	 * Description of a {@link DailyTimeIntervalTrigger}.
	 */
	public static final class DailyTimeIntervalTriggerDescriptor extends TriggerDescriptor {

		private final DailyTimeIntervalTrigger trigger;

		/**
		 * Constructs a new DailyTimeIntervalTriggerDescriptor with the specified
		 * DailyTimeIntervalTrigger.
		 * @param trigger the DailyTimeIntervalTrigger to be associated with this
		 * descriptor
		 */
		public DailyTimeIntervalTriggerDescriptor(DailyTimeIntervalTrigger trigger) {
			super(trigger, TriggerType.DAILY_INTERVAL);
			this.trigger = trigger;
		}

		/**
		 * Appends the summary information of the DailyTimeIntervalTrigger to the given
		 * content map.
		 * @param content the map to append the summary information to
		 */
		@Override
		protected void appendSummary(Map<String, Object> content) {
			content.put("interval",
					getIntervalDuration(this.trigger.getRepeatInterval(), this.trigger.getRepeatIntervalUnit())
						.toMillis());
			putIfNoNull(content, "daysOfWeek", this.trigger.getDaysOfWeek());
			putIfNoNull(content, "startTimeOfDay", getLocalTime(this.trigger.getStartTimeOfDay()));
			putIfNoNull(content, "endTimeOfDay", getLocalTime(this.trigger.getEndTimeOfDay()));
		}

		/**
		 * Appends the details of the DailyTimeIntervalTrigger to the given content map.
		 * @param content the map to append the details to
		 */
		@Override
		protected void appendDetails(Map<String, Object> content) {
			appendSummary(content);
			content.put("repeatCount", this.trigger.getRepeatCount());
			content.put("timesTriggered", this.trigger.getTimesTriggered());
		}

	}

	/**
	 * Description of a {@link CalendarIntervalTrigger}.
	 */
	public static final class CalendarIntervalTriggerDescriptor extends TriggerDescriptor {

		private final CalendarIntervalTrigger trigger;

		/**
		 * Constructs a new CalendarIntervalTriggerDescriptor with the specified
		 * CalendarIntervalTrigger.
		 * @param trigger the CalendarIntervalTrigger to be used
		 */
		public CalendarIntervalTriggerDescriptor(CalendarIntervalTrigger trigger) {
			super(trigger, TriggerType.CALENDAR_INTERVAL);
			this.trigger = trigger;
		}

		/**
		 * Appends the summary information of the CalendarIntervalTriggerDescriptor to the
		 * given content map.
		 * @param content the map to append the summary information to
		 */
		@Override
		protected void appendSummary(Map<String, Object> content) {
			content.put("interval",
					getIntervalDuration(this.trigger.getRepeatInterval(), this.trigger.getRepeatIntervalUnit())
						.toMillis());
			putIfNoNull(content, "timeZone", this.trigger.getTimeZone());
		}

		/**
		 * Appends the details of the CalendarIntervalTriggerDescriptor to the given
		 * content map.
		 * @param content the map to append the details to
		 */
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
	 * Description of a custom {@link Trigger}.
	 */
	public static final class CustomTriggerDescriptor extends TriggerDescriptor {

		/**
		 * Constructs a new CustomTriggerDescriptor with the specified trigger.
		 * @param trigger the trigger associated with this descriptor
		 */
		public CustomTriggerDescriptor(Trigger trigger) {
			super(trigger, TriggerType.CUSTOM_TRIGGER);
		}

		/**
		 * Appends the summary of the CustomTriggerDescriptor to the given content map.
		 * @param content the map to append the summary to
		 */
		@Override
		protected void appendSummary(Map<String, Object> content) {
			content.put("trigger", getTrigger().toString());
		}

		/**
		 * Appends details to the given content map.
		 * @param content the map to append details to
		 */
		@Override
		protected void appendDetails(Map<String, Object> content) {
			appendSummary(content);
		}

	}

}
