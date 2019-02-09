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

import org.junit.Test;

import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * Tests for {@link TaskSchedulerBuilder}.
 *
 * @author Stephane Nicoll
 */
public class TaskSchedulerBuilderTests {

	private TaskSchedulerBuilder builder = new TaskSchedulerBuilder();

	@Test
	public void poolSettingsShouldApply() {
		ThreadPoolTaskScheduler scheduler = this.builder.poolSize(4).build();
		assertThat(scheduler.getPoolSize()).isEqualTo(4);
	}

	@Test
	public void threadNamePrefixShouldApply() {
		ThreadPoolTaskScheduler scheduler = this.builder.threadNamePrefix("test-")
				.build();
		assertThat(scheduler.getThreadNamePrefix()).isEqualTo("test-");
	}

	@Test
	public void customizersWhenCustomizersAreNullShouldThrowException() {
		assertThatIllegalArgumentException()
				.isThrownBy(
						() -> this.builder.customizers((TaskSchedulerCustomizer[]) null))
				.withMessageContaining("Customizers must not be null");
	}

	@Test
	public void customizersCollectionWhenCustomizersAreNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> this.builder.customizers((Set<TaskSchedulerCustomizer>) null))
				.withMessageContaining("Customizers must not be null");
	}

	@Test
	public void customizersShouldApply() {
		TaskSchedulerCustomizer customizer = mock(TaskSchedulerCustomizer.class);
		ThreadPoolTaskScheduler scheduler = this.builder.customizers(customizer).build();
		verify(customizer).customize(scheduler);
	}

	@Test
	public void customizersShouldBeAppliedLast() {
		ThreadPoolTaskScheduler scheduler = spy(new ThreadPoolTaskScheduler());
		this.builder.poolSize(4).threadNamePrefix("test-")
				.additionalCustomizers((taskScheduler) -> {
					verify(taskScheduler).setPoolSize(4);
					verify(taskScheduler).setThreadNamePrefix("test-");
				});
		this.builder.configure(scheduler);
	}

	@Test
	public void customizersShouldReplaceExisting() {
		TaskSchedulerCustomizer customizer1 = mock(TaskSchedulerCustomizer.class);
		TaskSchedulerCustomizer customizer2 = mock(TaskSchedulerCustomizer.class);
		ThreadPoolTaskScheduler scheduler = this.builder.customizers(customizer1)
				.customizers(Collections.singleton(customizer2)).build();
		verifyZeroInteractions(customizer1);
		verify(customizer2).customize(scheduler);
	}

	@Test
	public void additionalCustomizersWhenCustomizersAreNullShouldThrowException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.builder
						.additionalCustomizers((TaskSchedulerCustomizer[]) null))
				.withMessageContaining("Customizers must not be null");
	}

	@Test
	public void additionalCustomizersCollectionWhenCustomizersAreNullShouldThrowException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.builder
						.additionalCustomizers((Set<TaskSchedulerCustomizer>) null))
				.withMessageContaining("Customizers must not be null");
	}

	@Test
	public void additionalCustomizersShouldAddToExisting() {
		TaskSchedulerCustomizer customizer1 = mock(TaskSchedulerCustomizer.class);
		TaskSchedulerCustomizer customizer2 = mock(TaskSchedulerCustomizer.class);
		ThreadPoolTaskScheduler scheduler = this.builder.customizers(customizer1)
				.additionalCustomizers(customizer2).build();
		verify(customizer1).customize(scheduler);
		verify(customizer2).customize(scheduler);
	}

}
