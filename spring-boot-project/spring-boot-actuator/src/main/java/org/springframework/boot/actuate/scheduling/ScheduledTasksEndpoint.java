/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.scheduling;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.FixedDelayTask;
import org.springframework.scheduling.config.FixedRateTask;
import org.springframework.scheduling.config.IntervalTask;
import org.springframework.scheduling.config.ScheduledTask;
import org.springframework.scheduling.config.ScheduledTaskHolder;
import org.springframework.scheduling.config.Task;
import org.springframework.scheduling.config.TriggerTask;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.scheduling.support.ScheduledMethodRunnable;

/**
 * {@link Endpoint} to expose information about an application's scheduled tasks.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
@Endpoint(id = "scheduledtasks")
public class ScheduledTasksEndpoint {

	private final Collection<ScheduledTaskHolder> scheduledTaskHolders;

	public ScheduledTasksEndpoint(Collection<ScheduledTaskHolder> scheduledTaskHolders) {
		this.scheduledTaskHolders = scheduledTaskHolders;
	}

	@ReadOperation
	public ScheduledTasksReport scheduledTasks() {
		Map<TaskType, List<TaskDescription>> descriptionsByType = this.scheduledTaskHolders
				.stream().flatMap((holder) -> holder.getScheduledTasks().stream())
				.map(ScheduledTask::getTask).map(TaskDescription::of)
				.filter(Objects::nonNull)
				.collect(Collectors.groupingBy(TaskDescription::getType));
		return new ScheduledTasksReport(descriptionsByType);
	}

	/**
	 * A report of an application's scheduled {@link Task Tasks}, primarily intended for
	 * serialization to JSON.
	 */
	public static final class ScheduledTasksReport {

		private final List<TaskDescription> cron;

		private final List<TaskDescription> fixedDelay;

		private final List<TaskDescription> fixedRate;

		private ScheduledTasksReport(
				Map<TaskType, List<TaskDescription>> descriptionsByType) {
			this.cron = descriptionsByType.getOrDefault(TaskType.CRON,
					Collections.emptyList());
			this.fixedDelay = descriptionsByType.getOrDefault(TaskType.FIXED_DELAY,
					Collections.emptyList());
			this.fixedRate = descriptionsByType.getOrDefault(TaskType.FIXED_RATE,
					Collections.emptyList());
		}

		public List<TaskDescription> getCron() {
			return this.cron;
		}

		public List<TaskDescription> getFixedDelay() {
			return this.fixedDelay;
		}

		public List<TaskDescription> getFixedRate() {
			return this.fixedRate;
		}

	}

	/**
	 * Base class for descriptions of a {@link Task}.
	 */
	public abstract static class TaskDescription {

		private static final Map<Class<? extends Task>, Function<Task, TaskDescription>> DESCRIBERS = new LinkedHashMap<>();

		static {
			DESCRIBERS.put(FixedRateTask.class,
					(task) -> new FixedRateTaskDescription((FixedRateTask) task));
			DESCRIBERS.put(FixedDelayTask.class,
					(task) -> new FixedDelayTaskDescription((FixedDelayTask) task));
			DESCRIBERS.put(CronTask.class,
					(task) -> new CronTaskDescription((CronTask) task));
			DESCRIBERS.put(TriggerTask.class,
					(task) -> describeTriggerTask((TriggerTask) task));
		}

		private final TaskType type;

		private final RunnableDescription runnable;

		private static TaskDescription of(Task task) {
			return DESCRIBERS.entrySet().stream()
					.filter((entry) -> entry.getKey().isInstance(task))
					.map((entry) -> entry.getValue().apply(task)).findFirst()
					.orElse(null);
		}

		private static TaskDescription describeTriggerTask(TriggerTask triggerTask) {
			Trigger trigger = triggerTask.getTrigger();
			if (trigger instanceof CronTrigger) {
				return new CronTaskDescription(triggerTask, (CronTrigger) trigger);
			}
			if (trigger instanceof PeriodicTrigger) {
				PeriodicTrigger periodicTrigger = (PeriodicTrigger) trigger;
				if (periodicTrigger.isFixedRate()) {
					return new FixedRateTaskDescription(triggerTask, periodicTrigger);
				}
				return new FixedDelayTaskDescription(triggerTask, periodicTrigger);
			}
			return null;
		}

		protected TaskDescription(TaskType type, Runnable runnable) {
			this.type = type;
			this.runnable = new RunnableDescription(runnable);
		}

		private TaskType getType() {
			return this.type;
		}

		public final RunnableDescription getRunnable() {
			return this.runnable;
		}

	}

	/**
	 * A description of an {@link IntervalTask}.
	 */
	public static class IntervalTaskDescription extends TaskDescription {

		private final long initialDelay;

		private final long interval;

		protected IntervalTaskDescription(TaskType type, IntervalTask task) {
			super(type, task.getRunnable());
			this.initialDelay = task.getInitialDelay();
			this.interval = task.getInterval();
		}

		protected IntervalTaskDescription(TaskType type, TriggerTask task,
				PeriodicTrigger trigger) {
			super(type, task.getRunnable());
			this.initialDelay = trigger.getInitialDelay();
			this.interval = trigger.getPeriod();
		}

		public long getInitialDelay() {
			return this.initialDelay;
		}

		public long getInterval() {
			return this.interval;
		}

	}

	/**
	 * A description of a {@link FixedDelayTask} or a {@link TriggerTask} with a
	 * fixed-delay {@link PeriodicTrigger}.
	 */
	public static final class FixedDelayTaskDescription extends IntervalTaskDescription {

		private FixedDelayTaskDescription(FixedDelayTask task) {
			super(TaskType.FIXED_DELAY, task);
		}

		private FixedDelayTaskDescription(TriggerTask task, PeriodicTrigger trigger) {
			super(TaskType.FIXED_DELAY, task, trigger);
		}

	}

	/**
	 * A description of a {@link FixedRateTask} or a {@link TriggerTask} with a fixed-rate
	 * {@link PeriodicTrigger}.
	 */
	public static final class FixedRateTaskDescription extends IntervalTaskDescription {

		private FixedRateTaskDescription(FixedRateTask task) {
			super(TaskType.FIXED_RATE, task);
		}

		private FixedRateTaskDescription(TriggerTask task, PeriodicTrigger trigger) {
			super(TaskType.FIXED_RATE, task, trigger);
		}

	}

	/**
	 * A description of a {@link CronTask} or a {@link TriggerTask} with a
	 * {@link CronTrigger}.
	 */
	public static final class CronTaskDescription extends TaskDescription {

		private final String expression;

		private CronTaskDescription(CronTask task) {
			super(TaskType.CRON, task.getRunnable());
			this.expression = task.getExpression();
		}

		private CronTaskDescription(TriggerTask task, CronTrigger trigger) {
			super(TaskType.CRON, task.getRunnable());
			this.expression = trigger.getExpression();
		}

		public String getExpression() {
			return this.expression;
		}

	}

	/**
	 * A description of a {@link Task Task's} {@link Runnable}.
	 *
	 * @author Andy Wilkinson
	 */
	public static final class RunnableDescription {

		private final String target;

		private RunnableDescription(Runnable runnable) {
			if (runnable instanceof ScheduledMethodRunnable) {
				Method method = ((ScheduledMethodRunnable) runnable).getMethod();
				this.target = method.getDeclaringClass().getName() + "."
						+ method.getName();
			}
			else {
				this.target = runnable.getClass().getName();
			}
		}

		public String getTarget() {
			return this.target;
		}

	}

	private enum TaskType {

		CRON, FIXED_DELAY, FIXED_RATE

	}

}
