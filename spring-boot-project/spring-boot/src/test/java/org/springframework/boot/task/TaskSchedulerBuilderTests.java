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

package org.springframework.boot.task;

import java.util.Collections;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TaskSchedulerBuilder}.
 *
 * @author Stephane Nicoll
 */
public class TaskSchedulerBuilderTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private TaskSchedulerBuilder builder = new TaskSchedulerBuilder();

	@Test
	public void createWhenCustomizersAreNullShouldThrowException() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("TaskSchedulerCustomizers must not be null");
		new TaskSchedulerBuilder((TaskSchedulerCustomizer[]) null);
	}

	@Test
	public void poolSettingsShouldApply() {
		ThreadPoolTaskScheduler scheduler = this.builder.poolSize(4).build();
		assertThat(scheduler.getPoolSize()).isEqualTo(4);
	}

	@Test
	public void threadNamePrefixShouldApply() {
		ThreadPoolTaskScheduler executor = this.builder.threadNamePrefix("test-").build();
		assertThat(executor.getThreadNamePrefix()).isEqualTo("test-");
	}

	@Test
	public void customizersWhenCustomizersAreNullShouldThrowException() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("TaskSchedulerCustomizers must not be null");
		this.builder.customizers((TaskSchedulerCustomizer[]) null);
	}

	@Test
	public void customizersCollectionWhenCustomizersAreNullShouldThrowException() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("TaskSchedulerCustomizers must not be null");
		this.builder.customizers((Set<TaskSchedulerCustomizer>) null);
	}

	@Test
	public void customizersShouldApply() {
		TaskSchedulerCustomizer customizer = Mockito.mock(TaskSchedulerCustomizer.class);
		ThreadPoolTaskScheduler executor = this.builder.customizers(customizer).build();
		Mockito.verify(customizer).customize(executor);
	}

	@Test
	public void customizersShouldBeAppliedLast() {
		ThreadPoolTaskScheduler scheduler = Mockito.spy(new ThreadPoolTaskScheduler());
		this.builder.poolSize(4).threadNamePrefix("test-")
				.additionalCustomizers((taskScheduler) -> {
					Mockito.verify(taskScheduler).setPoolSize(4);
					Mockito.verify(taskScheduler).setThreadNamePrefix("test-");
				});
		this.builder.configure(scheduler);
	}

	@Test
	public void customizersShouldReplaceExisting() {
		TaskSchedulerCustomizer customizer1 = Mockito.mock(TaskSchedulerCustomizer.class);
		TaskSchedulerCustomizer customizer2 = Mockito.mock(TaskSchedulerCustomizer.class);
		ThreadPoolTaskScheduler executor = this.builder.customizers(customizer1)
				.customizers(Collections.singleton(customizer2)).build();
		Mockito.verifyZeroInteractions(customizer1);
		Mockito.verify(customizer2).customize(executor);
	}

	@Test
	public void additionalCustomizersWhenCustomizersAreNullShouldThrowException() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("TaskSchedulerCustomizers must not be null");
		this.builder.additionalCustomizers((TaskSchedulerCustomizer[]) null);
	}

	@Test
	public void additionalCustomizersCollectionWhenCustomizersAreNullShouldThrowException() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("TaskSchedulerCustomizers must not be null");
		this.builder.additionalCustomizers((Set<TaskSchedulerCustomizer>) null);
	}

	@Test
	public void additionalCustomizersShouldAddToExisting() {
		TaskSchedulerCustomizer customizer1 = Mockito.mock(TaskSchedulerCustomizer.class);
		TaskSchedulerCustomizer customizer2 = Mockito.mock(TaskSchedulerCustomizer.class);
		ThreadPoolTaskScheduler scheduler = this.builder.customizers(customizer1)
				.additionalCustomizers(customizer2).build();
		Mockito.verify(customizer1).customize(scheduler);
		Mockito.verify(customizer2).customize(scheduler);
	}

}
