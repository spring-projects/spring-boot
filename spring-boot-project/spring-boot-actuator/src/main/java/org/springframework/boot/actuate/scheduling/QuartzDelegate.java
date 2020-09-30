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

import java.util.List;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.boot.actuate.scheduling.QuartzEndpoint.QuartzDescriptor;
import org.springframework.boot.actuate.scheduling.QuartzEndpoint.QuartzKey;

/**
 * Delegate to retrieve informations about {@link Scheduler} object.
 *
 * @author Jordan Couret
 * @since 2.4.0
 */
public interface QuartzDelegate {

	List<String> getGroupName() throws SchedulerException;

	List<QuartzKey> getKeys(String groupName);

	QuartzKey getKey(String group, String name);

	QuartzDescriptor getDescriptor(QuartzKey key) throws SchedulerException;

	boolean exist(QuartzKey key) throws SchedulerException;

	boolean isInPausedMode(QuartzKey key) throws SchedulerException;

	void resume(QuartzKey key) throws SchedulerException;

	void pause(QuartzKey key) throws SchedulerException;

	boolean delete(QuartzKey key) throws SchedulerException;

}
