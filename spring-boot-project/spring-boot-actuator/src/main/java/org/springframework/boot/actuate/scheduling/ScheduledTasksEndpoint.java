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

package org.springframework.boot.actuate.scheduling;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

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
import org.springframework.scheduling.config.TriggerTask;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.scheduling.support.ScheduledMethodRunnable;

/**
 * {@link Endpoint @Endpoint} to expose information about an application's scheduled
 * tasks.
 *
 * @author Andy Wilkinson
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
		Map<TaskType, List<TaskDescriptor>> descriptionsByType = this.scheduledTaskHolders.stream()
			.flatMap((holder) -> holder.getScheduledTasks().stream())
			.map(ScheduledTask::getTask)
			.map(TaskDescriptor::of)
			.filter(Objects::nonNull)
			.collect(Collectors.groupingBy(TaskDescriptor::getType));
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

		private static final Map<Class<? extends Task>, Function<Task, TaskDescriptor>> DESCRIBERS = new LinkedHashMap<>();

		static {
			DESCRIBERS.put(FixedRateTask.class, (task) -> new FixedRateTaskDescriptor((FixedRateTask) task));
			DESCRIBERS.put(FixedDelayTask.class, (task) -> new FixedDelayTaskDescriptor((FixedDelayTask) task));
			DESCRIBERS.put(CronTask.class, (task) -> new CronTaskDescriptor((CronTask) task));
			DESCRIBERS.put(TriggerTask.class, (task) -> describeTriggerTask((TriggerTask) task));
		}

		private final TaskType type;

		private final RunnableDescriptor runnable;

		private static TaskDescriptor of(Task task) {
			return DESCRIBERS.entrySet()
				.stream()
				.filter((entry) -> entry.getKey().isInstance(task))
				.map((entry) -> entry.getValue().apply(task))
				.findFirst()
				.orElse(null);
		}

		private static TaskDescriptor describeTriggerTask(TriggerTask triggerTask) {
			Trigger trigger = triggerTask.getTrigger();
			if (trigger instanceof CronTrigger cronTrigger) {
				return new CronTaskDescriptor(triggerTask, cronTrigger);
			}
			if (trigger instanceof PeriodicTrigger periodicTrigger) {
				if (periodicTrigger.isFixedRate()) {
					return new FixedRateTaskDescriptor(triggerTask, periodicTrigger);
				}
				return new FixedDelayTaskDescriptor(triggerTask, periodicTrigger);
			}
			return new CustomTriggerTaskDescriptor(triggerTask);
		}

		protected TaskDescriptor(TaskType type, Runnable runnable) {
			this.type = type;
			this.runnable = new RunnableDescriptor(runnable);
		}

		private TaskType getType() {
			return this.type;
		}

		public final RunnableDescriptor getRunnable() {
			return this.runnable;
		}

	}

	/**
	 * Description of an {@link IntervalTask}.
	 */
	public static class IntervalTaskDescriptor extends TaskDescriptor {

		private final long initialDelay;

		private final long interval;

		protected IntervalTaskDescriptor(TaskType type, IntervalTask task) {
			super(type, task.getRunnable());
			this.initialDelay = task.getInitialDelayDuration().toMillis();
			this.interval = task.getIntervalDuration().toMillis();
		}

		protected IntervalTaskDescriptor(TaskType type, TriggerTask task, PeriodicTrigger trigger) {
			super(type, task.getRunnable());
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

		private FixedDelayTaskDescriptor(FixedDelayTask task) {
			super(TaskType.FIXED_DELAY, task);
		}

		private FixedDelayTaskDescriptor(TriggerTask task, PeriodicTrigger trigger) {
			super(TaskType.FIXED_DELAY, task, trigger);
		}

	}

	/**
	 * Description of a {@link FixedRateTask} or a {@link TriggerTask} with a fixed-rate
	 * {@link PeriodicTrigger}.
	 */
	public static final class FixedRateTaskDescriptor extends IntervalTaskDescriptor {

		private FixedRateTaskDescriptor(FixedRateTask task) {
			super(TaskType.FIXED_RATE, task);
		}

		private FixedRateTaskDescriptor(TriggerTask task, PeriodicTrigger trigger) {
			super(TaskType.FIXED_RATE, task, trigger);
		}

	}

	/**
	 * Description of a {@link CronTask} or a {@link TriggerTask} with a
	 * {@link CronTrigger}.
	 */
	public static final class CronTaskDescriptor extends TaskDescriptor {

		private final String expression;

		private CronTaskDescriptor(CronTask task) {
			super(TaskType.CRON, task.getRunnable());
			this.expression = task.getExpression();
		}

		private CronTaskDescriptor(TriggerTask task, CronTrigger trigger) {
			super(TaskType.CRON, task.getRunnable());
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

		private CustomTriggerTaskDescriptor(TriggerTask task) {
			super(TaskType.CUSTOM_TRIGGER, task.getRunnable());
			this.trigger = task.getTrigger().toString();
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
			if (runnable instanceof ScheduledMethodRunnable scheduledMethodRunnable) {
				Method method = scheduledMethodRunnable.getMethod();
				this.target = method.getDeclaringClass().getName() + "." + method.getName();
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

		CRON, CUSTOM_TRIGGER, FIXED_DELAY, FIXED_RATE

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
