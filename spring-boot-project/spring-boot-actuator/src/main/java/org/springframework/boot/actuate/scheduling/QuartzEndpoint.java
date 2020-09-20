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

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.springframework.boot.actuate.endpoint.annotation.DeleteOperation;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.util.Assert;

/**
 * {@link Endpoint} that provides access to {@link Scheduler} information.
 *
 * @author Jordan Couret
 * @author Philipp Tölle
 * @since 2.4.0
 */
@Endpoint(id = "quartz")
public class QuartzEndpoint {

	private final Map<String, QuartzDelegate> delegateMap = new HashMap<>();

	public QuartzEndpoint(Scheduler scheduler) {
		Assert.notNull(scheduler, "Scheduler must not be null");
		this.delegateMap.put("jobs", new QuartzJobDelegate(scheduler));
		this.delegateMap.put("triggers", new QuartzTriggerDelegate(scheduler));
	}

	/**
	 * Retrieve all {@link Job}'s key OR {@link Trigger}'s key registered
	 * @param delegateName : define the target (should be 'jobs' or 'triggers')
	 * @throws SpringQuartzException if invalid delegateName or if SchedulerException was
	 * thrown during scheduler access
	 * @return list of key
	 */
	@ReadOperation
	public List<QuartzKey> findAll(@Selector String delegateName) {
		try {

			QuartzDelegate delegate = this.getDelegate(delegateName);
			return delegate.getGroupName().stream().map(delegate::getKeys).flatMap(List::stream)
					.collect(Collectors.toList());
		}
		catch (SchedulerException schedulerException) {
			throw new SpringQuartzException(schedulerException);
		}
	}

	/**
	 * Retrieve all {@link Job}'s key OR {@link Trigger}'s key registered with a specific
	 * group.
	 * @param delegateName : define the target (should be 'jobs' or 'triggers')
	 * @param group : the given group
	 * @return list of key
	 */
	@ReadOperation
	public List<QuartzKey> findByGroup(@Selector String delegateName, @Selector String group) {
		return this.getDelegate(delegateName).getKeys(group);
	}

	/**
	 * Return a {@link QuartzDescriptor} with details information about the {@link Job} or
	 * {@link Trigger}.
	 * @param delegateName : define the target (should be 'jobs' or 'triggers')
	 * @param group : the given group
	 * @param name : the given name
	 * @throws SpringQuartzException if invalid delegateName or if
	 * {@link SchedulerException} was thrown during scheduler access
	 * @return a quartz descriptor
	 */
	@ReadOperation
	public QuartzDescriptor findByGroupAndName(@Selector String delegateName, @Selector String group,
			@Selector String name) {
		try {

			QuartzDelegate delegate = this.getDelegate(delegateName);
			QuartzKey key = delegate.getKey(group, name);
			return delegate.exist(key) ? delegate.getDescriptor(key) : null;
		}
		catch (SchedulerException schedulerException) {
			throw new SpringQuartzException(schedulerException);
		}
	}

	/**
	 * According to the current state of {@link Job} or {@link Trigger}, it will pause or
	 * resume the given item.
	 * @param delegateName : define the target (should be 'jobs' or 'triggers')
	 * @param group : the given group
	 * @param name : the given name
	 * @throws SpringQuartzException if invalid delegateName or if
	 * {@link SchedulerException} was thrown during scheduler access
	 * @return a quartz descriptor with the new state
	 */
	@WriteOperation
	public QuartzDescriptor pauseOrResumeKey(@Selector String delegateName, @Selector String group,
			@Selector String name) {
		QuartzDelegate delegate = this.getDelegate(delegateName);
		QuartzKey key = delegate.getKey(group, name);

		try {
			if (delegate.exist(key)) {
				if (delegate.isInPausedMode(key)) {
					delegate.resume(key);
				}
				else {
					delegate.pause(key);
				}
				return delegate.getDescriptor(key);
			}
		}
		catch (SchedulerException schedulerException) {
			throw new SpringQuartzException(schedulerException);
		}

		return null;
	}

	/**
	 * Delete a {@link Job} or a {@link Trigger}.
	 * @param delegateName : define the target (should be 'jobs' or 'triggers')
	 * @param group : the given group
	 * @param name : the given name
	 * @throws SpringQuartzException if invalid delegateName or if
	 * {@link SchedulerException} was thrown during scheduler access
	 * @return a quartz descriptor with the new state
	 */
	@DeleteOperation
	public boolean deleteKey(@Selector String delegateName, @Selector String group, @Selector String name) {
		try {
			QuartzDelegate delegate = this.getDelegate(delegateName);
			QuartzKey key = delegate.getKey(group, name);
			return delegate.exist(key) && delegate.delete(key);
		}
		catch (SchedulerException schedulerException) {
			throw new SpringQuartzException(schedulerException);
		}
	}

	private QuartzDelegate getDelegate(String delegateName) {
		return Optional.ofNullable(this.delegateMap.get(delegateName))
				.orElseThrow(() -> new SpringQuartzException("URL should be /quartz/job OR /quartz/triggers"));
	}

	/**
	 * Generic object to retrieve detailed information about a {@link Job} or a
	 * {@link Trigger}, primarily intended for serialization to JSON.
	 */
	public static class QuartzDescriptor {

		private final QuartzKey jobKey;

		private QuartzKey triggerKey;

		private final String description;

		private final Class<? extends Job> jobClass;

		private final Map<String, Object> data;

		private LocalDateTime nextFireDate;

		private String triggerState;

		public QuartzDescriptor(JobDetail jobDetail, Trigger trigger, String triggerState) {
			this.jobKey = new QuartzKey(jobDetail.getKey().getName(), jobDetail.getKey().getGroup());
			this.description = jobDetail.getDescription();
			this.jobClass = jobDetail.getJobClass();
			this.data = Optional.ofNullable(jobDetail.getJobDataMap()).orElse(new JobDataMap()).getWrappedMap();
			this.nextFireDate = trigger.getNextFireTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
			this.triggerKey = new QuartzKey(trigger.getKey().getName(), trigger.getKey().getGroup());
			this.triggerState = triggerState;
		}

		public QuartzDescriptor(JobDetail jobDetail) {
			this.jobKey = new QuartzKey(jobDetail.getKey().getName(), jobDetail.getKey().getGroup());
			this.description = jobDetail.getDescription();
			this.jobClass = jobDetail.getJobClass();
			this.data = Optional.ofNullable(jobDetail.getJobDataMap()).orElse(new JobDataMap()).getWrappedMap();
		}

		public QuartzKey getJobKey() {
			return jobKey;
		}

		public QuartzKey getTriggerKey() {
			return triggerKey;
		}

		public String getDescription() {
			return description;
		}

		public Class<? extends Job> getJobClass() {
			return jobClass;
		}

		public Map<String, Object> getData() {
			return data;
		}

		public LocalDateTime getNextFireDate() {
			return nextFireDate;
		}

		public String getTriggerState() {
			return triggerState;
		}

	}

	/**
	 * A representation of {@link org.quartz.utils.Key}, primarily intended for
	 * serialization to JSON.
	 */
	public static class QuartzKey {

		private final String name;

		private final String group;

		public QuartzKey(String name, String group) {
			this.name = name;
			this.group = group;
		}

		public String getName() {
			return this.name;
		}

		public String getGroup() {
			return this.group;
		}

		public JobKey toJobKey() {
			return new JobKey(this.getName(), this.getGroup());
		}

		public TriggerKey toTriggerKey() {
			return new TriggerKey(this.getName(), this.getGroup());
		}

	}

}
