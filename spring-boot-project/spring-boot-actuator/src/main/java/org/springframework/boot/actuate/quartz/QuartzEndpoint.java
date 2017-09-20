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

package org.springframework.boot.actuate.quartz;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.matchers.GroupMatcher;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.util.Assert;

/**
 * {@link Endpoint} to expose Quartz Scheduler info.
 *
 * @author Vedran Pavic
 * @since 2.0.0
 */
@Endpoint(id = "quartz")
public class QuartzEndpoint {

	private final Scheduler scheduler;

	public QuartzEndpoint(Scheduler scheduler) {
		Assert.notNull(scheduler, "Scheduler must not be null");
		this.scheduler = scheduler;
	}

	@ReadOperation
	public Map<String, Object> quartzReport() {
		Map<String, Object> result = new LinkedHashMap<>();
		try {
			for (String groupName : this.scheduler.getJobGroupNames()) {
				List<String> jobs = this.scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName)).stream()
						.map(JobKey::getName).collect(Collectors.toList());
				result.put(groupName, jobs);
			}
		}
		catch (SchedulerException ignored) {
		}
		return result;
	}

	@ReadOperation
	public QuartzJob quartzJob(@Selector String groupName, @Selector String jobName) {
		try {
			JobKey jobKey = JobKey.jobKey(jobName, groupName);
			JobDetail jobDetail = this.scheduler.getJobDetail(jobKey);
			List<? extends Trigger> triggers = this.scheduler.getTriggersOfJob(jobKey);
			return new QuartzJob(jobDetail, triggers);
		}
		catch (SchedulerException e) {
			return null;
		}
	}

	/**
	 * Details of a {@link Job Quartz Job}.
	 */
	public static final class QuartzJob {

		private final String jobGroup;

		private final String jobName;

		private final String description;

		private final String className;

		private final List<QuartzTrigger> triggers = new ArrayList<>();

		QuartzJob(JobDetail jobDetail, List<? extends Trigger> triggers) {
			this.jobGroup = jobDetail.getKey().getGroup();
			this.jobName = jobDetail.getKey().getName();
			this.description = jobDetail.getDescription();
			this.className = jobDetail.getJobClass().getName();
			triggers.forEach(trigger -> this.triggers.add(new QuartzTrigger(trigger)));
		}

		public String getJobGroup() {
			return this.jobGroup;
		}

		public String getJobName() {
			return this.jobName;
		}

		public String getDescription() {
			return this.description;
		}

		public String getClassName() {
			return this.className;
		}

		public List<QuartzTrigger> getTriggers() {
			return this.triggers;
		}

	}

	/**
	 * Details of a {@link Trigger Quartz Trigger}.
	 */
	public static final class QuartzTrigger {

		private final String triggerGroup;

		private final String triggerName;

		private final String description;

		private final String calendarName;

		private final Date startTime;

		private final Date endTime;

		private final Date previousFireTime;

		private final Date nextFireTime;

		private final Date finalFireTime;

		QuartzTrigger(Trigger trigger) {
			this.triggerGroup = trigger.getKey().getGroup();
			this.triggerName = trigger.getKey().getName();
			this.description = trigger.getDescription();
			this.calendarName = trigger.getCalendarName();
			this.startTime = trigger.getStartTime();
			this.endTime = trigger.getEndTime();
			this.previousFireTime = trigger.getPreviousFireTime();
			this.nextFireTime = trigger.getNextFireTime();
			this.finalFireTime = trigger.getFinalFireTime();
		}

		public String getTriggerGroup() {
			return this.triggerGroup;
		}

		public String getTriggerName() {
			return this.triggerName;
		}

		public String getDescription() {
			return this.description;
		}

		public String getCalendarName() {
			return this.calendarName;
		}

		public Date getStartTime() {
			return this.startTime;
		}

		public Date getEndTime() {
			return this.endTime;
		}

		public Date getPreviousFireTime() {
			return this.previousFireTime;
		}

		public Date getNextFireTime() {
			return this.nextFireTime;
		}

		public Date getFinalFireTime() {
			return this.finalFireTime;
		}

	}

}
