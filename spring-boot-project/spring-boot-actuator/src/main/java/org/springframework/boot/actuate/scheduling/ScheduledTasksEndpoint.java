/*
 * Copyright 2012-2024 the original author or authors.
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

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.springframework.aot.hint.BindingReflectionHintsRegistrar;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.boot.actuate.endpoint.OperationResponseBody;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.scheduling.ScheduledTasksEndpoint.ScheduledTasksEndpointRuntimeHints;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.FixedDelayTask;
import org.springframework.scheduling.config.FixedRateTask;
import org.springframework.scheduling.config.IntervalTask;
import org.springframework.scheduling.config.ScheduledTask;
import org.springframework.scheduling.config.ScheduledTaskHolder;
import org.springframework.scheduling.config.Task;
import org.springframework.scheduling.config.TaskExecutionOutcome;
import org.springframework.scheduling.config.TaskExecutionOutcome.Status;
import org.springframework.scheduling.config.TriggerTask;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * {@link Endpoint @Endpoint} to expose information about an application's scheduled
 * tasks.
 *
 * @author Andy Wilkinson
 * @author Brian Clozel
 * @since 2.0.0
 */
@Endpoint(id = "scheduledtasks")
@ImportRuntimeHints(ScheduledTasksEndpointRuntimeHints.class)
public class ScheduledTasksEndpoint {

	private final Collection<ScheduledTaskHolder> scheduledTaskHolders;

	public ScheduledTasksEndpoint(Collection<ScheduledTaskHolder> scheduledTaskHolders) {
		this.scheduledTaskHolders = scheduledTaskHolders;
	}

	@ReadOperation
	public ScheduledTasksDescriptor scheduledTasks() {
		MultiValueMap<TaskType, TaskDescriptor> descriptionsByType = new LinkedMultiValueMap<>();
		for (ScheduledTaskHolder holder : this.scheduledTaskHolders) {
			for (ScheduledTask scheduledTask : holder.getScheduledTasks()) {
				TaskType taskType = TaskType.forTask(scheduledTask);
				if (taskType != null) {
					TaskDescriptor descriptor = taskType.createDescriptor(scheduledTask);
					descriptionsByType.add(descriptor.getType(), descriptor);
				}
			}
		}
		return new ScheduledTasksDescriptor(descriptionsByType);
	}

	/**
	 * Description of an application's scheduled {@link Task Tasks}.
	 */
	public static final class ScheduledTasksDescriptor implements OperationResponseBody {

		private final List<TaskDescriptor> cron;

		private final List<TaskDescriptor> fixedDelay;

		private final List<TaskDescriptor> fixedRate;

		private final List<TaskDescriptor> custom;

		private ScheduledTasksDescriptor(Map<TaskType, List<TaskDescriptor>> descriptionsByType) {
			this.cron = descriptionsByType.getOrDefault(TaskType.CRON, Collections.emptyList());
			this.fixedDelay = descriptionsByType.getOrDefault(TaskType.FIXED_DELAY, Collections.emptyList());
			this.fixedRate = descriptionsByType.getOrDefault(TaskType.FIXED_RATE, Collections.emptyList());
			this.custom = descriptionsByType.getOrDefault(TaskType.CUSTOM_TRIGGER, Collections.emptyList());
		}

		public List<TaskDescriptor> getCron() {
			return this.cron;
		}

		public List<TaskDescriptor> getFixedDelay() {
			return this.fixedDelay;
		}

		public List<TaskDescriptor> getFixedRate() {
			return this.fixedRate;
		}

		public List<TaskDescriptor> getCustom() {
			return this.custom;
		}

	}

	/**
	 * Base class for descriptions of a {@link Task}.
	 */
	public abstract static class TaskDescriptor {

		private final TaskType type;

		private final ScheduledTask scheduledTask;

		private final RunnableDescriptor runnable;

		protected TaskDescriptor(ScheduledTask scheduledTask, TaskType type) {
			this.scheduledTask = scheduledTask;
			this.type = type;
			this.runnable = new RunnableDescriptor(scheduledTask.getTask().getRunnable());
		}

		private TaskType getType() {
			return this.type;
		}

		public final RunnableDescriptor getRunnable() {
			return this.runnable;
		}

		public final NextExecution getNextExecution() {
			Instant nextExecution = this.scheduledTask.nextExecution();
			if (nextExecution != null) {
				return new NextExecution(nextExecution);
			}
			return null;
		}

		public final LastExecution getLastExecution() {
			TaskExecutionOutcome lastExecutionOutcome = this.scheduledTask.getTask().getLastExecutionOutcome();
			if (lastExecutionOutcome.status() != Status.NONE) {
				return new LastExecution(lastExecutionOutcome);
			}
			return null;
		}

	}

	public static final class NextExecution {

		private final Instant time;

		public NextExecution(Instant time) {
			this.time = time;
		}

		public Instant getTime() {
			return this.time;
		}

	}

	public static final class LastExecution {

		private final TaskExecutionOutcome lastExecutionOutcome;

		private LastExecution(TaskExecutionOutcome lastExecutionOutcome) {
			this.lastExecutionOutcome = lastExecutionOutcome;
		}

		public Status getStatus() {
			return this.lastExecutionOutcome.status();
		}

		public Instant getTime() {
			return this.lastExecutionOutcome.executionTime();
		}

		public ExceptionInfo getException() {
			Throwable throwable = this.lastExecutionOutcome.throwable();
			if (throwable != null) {
				return new ExceptionInfo(throwable);
			}
			return null;
		}

	}

	public static final class ExceptionInfo {

		private final Throwable throwable;

		private ExceptionInfo(Throwable throwable) {
			this.throwable = throwable;
		}

		public String getType() {
			return this.throwable.getClass().getName();
		}

		public String getMessage() {
			return this.throwable.getMessage();
		}

	}

	private enum TaskType {

		CRON(CronTask.class,
				(scheduledTask) -> new CronTaskDescriptor(scheduledTask, (CronTask) scheduledTask.getTask())),
		FIXED_DELAY(FixedDelayTask.class,
				(scheduledTask) -> new FixedDelayTaskDescriptor(scheduledTask,
						(FixedDelayTask) scheduledTask.getTask())),
		FIXED_RATE(FixedRateTask.class,
				(scheduledTask) -> new FixedRateTaskDescriptor(scheduledTask, (FixedRateTask) scheduledTask.getTask())),
		CUSTOM_TRIGGER(TriggerTask.class, TaskType::describeTriggerTask);

		final Class<?> taskClass;

		final Function<ScheduledTask, TaskDescriptor> describer;

		TaskType(Class<?> taskClass, Function<ScheduledTask, TaskDescriptor> describer) {
			this.taskClass = taskClass;
			this.describer = describer;
		}

		static TaskType forTask(ScheduledTask scheduledTask) {
			for (TaskType taskType : TaskType.values()) {
				if (taskType.taskClass.isInstance(scheduledTask.getTask())) {
					return taskType;
				}
			}
			return null;
		}

		TaskDescriptor createDescriptor(ScheduledTask scheduledTask) {
			return this.describer.apply(scheduledTask);
		}

		private static TaskDescriptor describeTriggerTask(ScheduledTask scheduledTask) {
			TriggerTask triggerTask = (TriggerTask) scheduledTask.getTask();
			Trigger trigger = triggerTask.getTrigger();
			if (trigger instanceof CronTrigger cronTrigger) {
				return new CronTaskDescriptor(scheduledTask, triggerTask, cronTrigger);
			}
			if (trigger instanceof PeriodicTrigger periodicTrigger) {
				if (periodicTrigger.isFixedRate()) {
					return new FixedRateTaskDescriptor(scheduledTask, triggerTask, periodicTrigger);
				}
				return new FixedDelayTaskDescriptor(scheduledTask, triggerTask, periodicTrigger);
			}
			return new CustomTriggerTaskDescriptor(scheduledTask);
		}

	}

	/**
	 * Description of an {@link IntervalTask}.
	 */
	public static class IntervalTaskDescriptor extends TaskDescriptor {

		private final long initialDelay;

		private final long interval;

		protected IntervalTaskDescriptor(ScheduledTask scheduledTask, TaskType type, IntervalTask intervalTask) {
			super(scheduledTask, type);
			this.initialDelay = intervalTask.getInitialDelayDuration().toMillis();
			this.interval = intervalTask.getIntervalDuration().toMillis();
		}

		protected IntervalTaskDescriptor(ScheduledTask scheduledTask, TaskType type, TriggerTask task,
				PeriodicTrigger trigger) {
			super(scheduledTask, type);
			Duration initialDelayDuration = trigger.getInitialDelayDuration();
			this.initialDelay = (initialDelayDuration != null) ? initialDelayDuration.toMillis() : 0;
			this.interval = trigger.getPeriodDuration().toMillis();
		}

		public long getInitialDelay() {
			return this.initialDelay;
		}

		public long getInterval() {
			return this.interval;
		}

	}

	/**
	 * Description of a {@link FixedDelayTask} or a {@link TriggerTask} with a fixed-delay
	 * {@link PeriodicTrigger}.
	 */
	public static final class FixedDelayTaskDescriptor extends IntervalTaskDescriptor {

		private FixedDelayTaskDescriptor(ScheduledTask scheduledTask, FixedDelayTask task) {
			super(scheduledTask, TaskType.FIXED_DELAY, task);
		}

		private FixedDelayTaskDescriptor(ScheduledTask scheduledTask, TriggerTask task, PeriodicTrigger trigger) {
			super(scheduledTask, TaskType.FIXED_DELAY, task, trigger);
		}

	}

	/**
	 * Description of a {@link FixedRateTask} or a {@link TriggerTask} with a fixed-rate
	 * {@link PeriodicTrigger}.
	 */
	public static final class FixedRateTaskDescriptor extends IntervalTaskDescriptor {

		private FixedRateTaskDescriptor(ScheduledTask scheduledTask, FixedRateTask task) {
			super(scheduledTask, TaskType.FIXED_RATE, task);
		}

		private FixedRateTaskDescriptor(ScheduledTask scheduledTask, TriggerTask task, PeriodicTrigger trigger) {
			super(scheduledTask, TaskType.FIXED_RATE, task, trigger);
		}

	}

	/**
	 * Description of a {@link CronTask} or a {@link TriggerTask} with a
	 * {@link CronTrigger}.
	 */
	public static final class CronTaskDescriptor extends TaskDescriptor {

		private final String expression;

		private CronTaskDescriptor(ScheduledTask scheduledTask, CronTask cronTask) {
			super(scheduledTask, TaskType.CRON);
			this.expression = cronTask.getExpression();
		}

		private CronTaskDescriptor(ScheduledTask scheduledTask, TriggerTask triggerTask, CronTrigger trigger) {
			super(scheduledTask, TaskType.CRON);
			this.expression = trigger.getExpression();
		}

		public String getExpression() {
			return this.expression;
		}

	}

	/**
	 * Description of a {@link TriggerTask} with a custom {@link Trigger}.
	 */
	public static final class CustomTriggerTaskDescriptor extends TaskDescriptor {

		private final String trigger;

		private CustomTriggerTaskDescriptor(ScheduledTask scheduledTask) {
			super(scheduledTask, TaskType.CUSTOM_TRIGGER);
			TriggerTask triggerTask = (TriggerTask) scheduledTask.getTask();
			this.trigger = triggerTask.getTrigger().toString();
		}

		public String getTrigger() {
			return this.trigger;
		}

	}

	/**
	 * Description of a {@link Task Task's} {@link Runnable}.
	 */
	public static final class RunnableDescriptor {

		private final String target;

		private RunnableDescriptor(Runnable runnable) {
			this.target = runnable.toString();
		}

		public String getTarget() {
			return this.target;
		}

	}

	static class ScheduledTasksEndpointRuntimeHints implements RuntimeHintsRegistrar {

		private final BindingReflectionHintsRegistrar bindingRegistrar = new BindingReflectionHintsRegistrar();

		@Override
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
			this.bindingRegistrar.registerReflectionHints(hints.reflection(), FixedRateTaskDescriptor.class,
					FixedDelayTaskDescriptor.class, CronTaskDescriptor.class, CustomTriggerTaskDescriptor.class);
		}

	}

}
