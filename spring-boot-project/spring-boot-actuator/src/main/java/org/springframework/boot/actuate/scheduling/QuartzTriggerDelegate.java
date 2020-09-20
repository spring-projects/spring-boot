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
import java.util.stream.Collectors;

import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.Trigger.TriggerState;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.boot.actuate.scheduling.QuartzEndpoint.QuartzDescriptor;
import org.springframework.boot.actuate.scheduling.QuartzEndpoint.QuartzKey;

/**
 * Delegate that provides access to {@link Trigger} registered in the {@link Scheduler}.
 *
 * @author Jordan Couret
 * @since 2.4.0
 */
public class QuartzTriggerDelegate implements QuartzDelegate {

	private final Scheduler scheduler;

	public QuartzTriggerDelegate(Scheduler scheduler) {
		this.scheduler = scheduler;
	}

	@Override
	public List<String> getGroupName() throws SchedulerException {
		return this.scheduler.getTriggerGroupNames();
	}

	@Override
	public List<QuartzKey> getKeys(String groupName) {
		try {
			return this.scheduler.getTriggerKeys(GroupMatcher.groupEquals(groupName)).stream()
					.map(triggerKey -> new QuartzKey(triggerKey.getName(), triggerKey.getGroup()))
					.collect(Collectors.toList());
		}
		catch (SchedulerException ignored) {
			return Collections.emptyList();
		}
	}

	@Override
	public QuartzKey getKey(String group, String name) {
		return new QuartzKey(name, group);
	}

	@Override
	public QuartzDescriptor getDescriptor(QuartzKey key) throws SchedulerException {
		Trigger trigger = this.scheduler.getTrigger(key.toTriggerKey());
		JobDetail jobDetail = this.scheduler.getJobDetail(trigger.getJobKey());
		TriggerState triggerState = this.scheduler.getTriggerState(trigger.getKey());
		return new QuartzDescriptor(jobDetail, trigger, triggerState.name());
	}

	@Override
	public boolean exist(QuartzKey key) throws SchedulerException {
		return this.scheduler.checkExists(key.toTriggerKey());
	}

	@Override
	public boolean isInPausedMode(QuartzKey key) throws SchedulerException {
		return TriggerState.PAUSED.equals(this.scheduler.getTriggerState(key.toTriggerKey()));
	}

	@Override
	public void resume(QuartzKey key) throws SchedulerException {
		this.scheduler.resumeTrigger(key.toTriggerKey());
	}

	@Override
	public void pause(QuartzKey key) throws SchedulerException {
		this.scheduler.pauseTrigger(key.toTriggerKey());
	}

	@Override
	public boolean delete(QuartzKey key) throws SchedulerException {
		return this.scheduler.unscheduleJob(key.toTriggerKey());
	}

}
