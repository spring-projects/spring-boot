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

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.Trigger.TriggerState;
import org.quartz.TriggerKey;
import org.quartz.impl.matchers.GroupMatcher;

import org.springframework.boot.actuate.scheduling.QuartzEndpoint.QuartzDescriptor;
import org.springframework.boot.actuate.scheduling.QuartzEndpoint.QuartzKey;

/**
 * Delegate that provides access to {@link Job} registered in the {@link Scheduler}.
 *
 * @author Jordan Couret
 * @since 2.4.0
 */
public class QuartzJobDelegate implements QuartzDelegate {

	private final Scheduler scheduler;

	public QuartzJobDelegate(Scheduler scheduler) {
		this.scheduler = scheduler;
	}

	@Override
	public List<String> getGroupName() throws SchedulerException {
		return this.scheduler.getJobGroupNames();
	}

	@Override
	public List<QuartzKey> getKeys(String groupName) {
		try {
			return this.scheduler.getJobKeys(GroupMatcher.groupEquals(groupName)).stream()
					.map(jobKey -> new QuartzKey(jobKey.getName(), jobKey.getGroup())).collect(Collectors.toList());
		}
		catch (SchedulerException e) {
			return Collections.emptyList();
		}
	}

	@Override
	public QuartzKey getKey(String group, String name) {
		return new QuartzKey(name, group);
	}

	@Override
	public QuartzDescriptor getDescriptor(QuartzKey key) throws SchedulerException {
		JobDetail jobDetail = this.scheduler.getJobDetail(key.toJobKey());
		List<? extends Trigger> triggers = this.scheduler.getTriggersOfJob(key.toJobKey());
		if (triggers != null && triggers.size() >= 1) {
			Trigger trigger = triggers.get(0);
			String triggerState = this.scheduler.getTriggerState(trigger.getKey()).name();
			return new QuartzDescriptor(jobDetail, trigger, triggerState);
		}
		else {
			return new QuartzDescriptor(jobDetail);
		}
	}

	@Override
	public boolean exist(QuartzKey key) throws SchedulerException {
		return this.scheduler.checkExists(key.toJobKey());
	}

	@Override
	public boolean isInPausedMode(QuartzKey key) throws SchedulerException {
		List<? extends Trigger> triggersOfJob = this.scheduler.getTriggersOfJob(key.toJobKey());
		return triggersOfJob.stream().map(Trigger::getKey).map(getGetTriggerState())
				.filter(Objects::nonNull).allMatch(TriggerState.PAUSED::equals);
	}

	@Override
	public void resume(QuartzKey key) throws SchedulerException {
		this.scheduler.resumeJob(key.toJobKey());
	}

	@Override
	public void pause(QuartzKey key) throws SchedulerException {
		this.scheduler.pauseJob(key.toJobKey());
	}

	@Override
	public boolean delete(QuartzKey key) throws SchedulerException {
		return this.scheduler.deleteJob(key.toJobKey());
	}

	private Function<TriggerKey, TriggerState> getGetTriggerState() {
		return (triggerKey) -> {
			try {
				return this.scheduler.getTriggerState(triggerKey);
			}
			catch (SchedulerException ignored) {
				return null;
			}
		};
	}
}
