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

package org.springframework.boot.actuate.endpoint;


import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.IntervalTask;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.config.Task;
import org.springframework.scheduling.config.TriggerTask;
import org.springframework.scheduling.support.ScheduledMethodRunnable;

/**
 * {@link Endpoint} to expose scheduled task information.
 *
 * @author Yunkun Huang
 * @since 2.0.0
 */
@ConfigurationProperties(prefix = "endpoints.schedules")
public class ScheduledTaskEndpoint
		extends AbstractEndpoint<List<ScheduledTaskEndpoint.ScheduledTaskInformation>>
		implements SchedulingConfigurer {
	private List<ScheduledTaskInformation> list = new ArrayList<>();

	public ScheduledTaskEndpoint() {
		super("schedules");
	}

	/**
	 * Create a new {@link ScheduledTaskEndpoint} instance.
	 */
	@Override
	public List<ScheduledTaskInformation> invoke() {
		return this.list;
	}

	@Override
	public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
		this.list.addAll(build(taskRegistrar.getFixedRateTaskList(), ScheduledType.FIXEDRATE));
		this.list.addAll(build(taskRegistrar.getCronTaskList(), ScheduledType.CRON));
		this.list.addAll(build(taskRegistrar.getFixedDelayTaskList(), ScheduledType.FIXEDDELAY));
		this.list.addAll(build(taskRegistrar.getTriggerTaskList(), ScheduledType.TRIGGER));
	}

	private List<ScheduledTaskInformation> build(final List<? extends Task> tasks, final ScheduledType type) {
		return tasks.stream().map(task -> {
			ScheduledTaskInformation scheduledTaskInformation = new ScheduledTaskInformation();
			scheduledTaskInformation.setType(type);
			scheduledTaskInformation.setName(getGenericName(task.getRunnable()));
			if (task instanceof IntervalTask) {
				IntervalTask intervalTask = (IntervalTask) task;
				scheduledTaskInformation.setInitialDelay(intervalTask.getInitialDelay());
				scheduledTaskInformation.setInterval(intervalTask.getInterval());
			}
			if (task instanceof CronTask) {
				CronTask cronTask = (CronTask) task;
				scheduledTaskInformation.setExpression(cronTask.getExpression());
			}
			if (task instanceof TriggerTask) {
				TriggerTask triggerTask = (TriggerTask) task;
				scheduledTaskInformation.setTrigger(triggerTask.getTrigger().toString());
			}
			return scheduledTaskInformation;
		}).collect(Collectors.toList());
	}

	private String getGenericName(Runnable runnable) {
		if (runnable instanceof ScheduledMethodRunnable) {
			return ((ScheduledMethodRunnable) runnable).getMethod().toGenericString();
		}
		return runnable.toString();
	}

	/**
	 * Information for one scheduled task.
	 */
	@JsonInclude(Include.NON_EMPTY)
	public class ScheduledTaskInformation {
		private ScheduledType type;
		private long interval;
		private long initialDelay;
		private String name;
		private String expression;
		private String trigger;

		public ScheduledType getType() {
			return this.type;
		}

		public void setType(ScheduledType type) {
			this.type = type;
		}

		public long getInterval() {
			return this.interval;
		}

		public void setInterval(long interval) {
			this.interval = interval;
		}

		public long getInitialDelay() {
			return this.initialDelay;
		}

		public void setInitialDelay(long initialDelay) {
			this.initialDelay = initialDelay;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getName() {
			return this.name;
		}

		public void setExpression(String expression) {
			this.expression = expression;
		}

		public String getExpression() {
			return this.expression;
		}

		public void setTrigger(String trigger) {
			this.trigger = trigger;
		}

		public String getTrigger() {
			return this.trigger;
		}
	}

	/**
	 * Four different scheduled task type.
	 */
	public enum ScheduledType {
		/**
		 * Execute with a fixed period in milliseconds between invocations.
		 */
		FIXEDRATE,
		/**
		 * Execute with a cron-like expression.
		 */
		CRON,
		/**
		 * Execute with a fixed period in milliseconds between the end of the last invocation and the start of the next.
		 */
		FIXEDDELAY,
		/**
		 * Execute by a trigger.
		 */
		TRIGGER
	}
}
