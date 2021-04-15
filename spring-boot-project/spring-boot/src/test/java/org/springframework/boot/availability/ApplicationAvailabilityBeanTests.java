/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.availability;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.testsupport.system.CapturedOutput;
import org.springframework.boot.testsupport.system.OutputCaptureExtension;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ApplicationAvailabilityBean}
 *
 * @author Brian Clozel
 * @author Phillip Webb
 */
@ExtendWith(OutputCaptureExtension.class)
class ApplicationAvailabilityBeanTests {

	private AnnotationConfigApplicationContext context;

	private ApplicationAvailabilityBean availability;

	@BeforeEach
	void setup() {
		this.context = new AnnotationConfigApplicationContext(ApplicationAvailabilityBean.class);
		this.availability = this.context.getBean(ApplicationAvailabilityBean.class);
	}

	@Test
	void getLivenessStateWhenNoEventHasBeenPublishedReturnsDefaultState() {
		assertThat(this.availability.getLivenessState()).isEqualTo(LivenessState.BROKEN);
	}

	@Test
	void getLivenessStateWhenEventHasBeenPublishedReturnsPublishedState() {
		AvailabilityChangeEvent.publish(this.context, LivenessState.CORRECT);
		assertThat(this.availability.getLivenessState()).isEqualTo(LivenessState.CORRECT);
	}

	@Test
	void getReadinessStateWhenNoEventHasBeenPublishedReturnsDefaultState() {
		assertThat(this.availability.getReadinessState()).isEqualTo(ReadinessState.REFUSING_TRAFFIC);
	}

	@Test
	void getReadinessStateWhenEventHasBeenPublishedReturnsPublishedState() {
		AvailabilityChangeEvent.publish(this.context, ReadinessState.ACCEPTING_TRAFFIC);
		assertThat(this.availability.getReadinessState()).isEqualTo(ReadinessState.ACCEPTING_TRAFFIC);
	}

	@Test
	void getStateWhenNoEventHasBeenPublishedReturnsDefaultState() {
		assertThat(this.availability.getState(TestState.class)).isNull();
		assertThat(this.availability.getState(TestState.class, TestState.ONE)).isEqualTo(TestState.ONE);
	}

	@Test
	void getStateWhenEventHasBeenPublishedReturnsPublishedState() {
		AvailabilityChangeEvent.publish(this.context, TestState.TWO);
		assertThat(this.availability.getState(TestState.class)).isEqualTo(TestState.TWO);
		assertThat(this.availability.getState(TestState.class, TestState.ONE)).isEqualTo(TestState.TWO);
	}

	@Test
	void getLastChangeEventWhenNoEventHasBeenPublishedReturnsDefaultState() {
		assertThat(this.availability.getLastChangeEvent(TestState.class)).isNull();
	}

	@Test
	void getLastChangeEventWhenEventHasBeenPublishedReturnsPublishedState() {
		AvailabilityChangeEvent.publish(this.context, TestState.TWO);
		assertThat(this.availability.getLastChangeEvent(TestState.class)).isNotNull();
	}

	@Test
	void stateChangesAreLogged(CapturedOutput output) {
		AvailabilityChangeEvent.publish(this.context, LivenessState.CORRECT);
		assertThat(output).contains("Application availability state LivenessState changed to CORRECT\n");
		AvailabilityChangeEvent.publish(this.context, LivenessState.BROKEN);
		assertThat(output).contains("Application availability state LivenessState changed from CORRECT to BROKEN\n");
	}

	@Test
	void stateChangesAreLoggedWithExceptionSource(CapturedOutput output) {
		AvailabilityChangeEvent.publish(this.context, new IOException("connection error"), LivenessState.BROKEN);
		assertThat(output).contains("Application availability state LivenessState changed to BROKEN: "
				+ "java.io.IOException: connection error\n");
	}

	@Test
	void stateChangesAreLoggedWithOtherSource(CapturedOutput output) {
		AvailabilityChangeEvent.publish(this.context, new CustomEventSource(), LivenessState.BROKEN);
		assertThat(output).contains("Application availability state LivenessState changed to BROKEN: "
				+ CustomEventSource.class.getName() + "\n");
	}

	enum TestState implements AvailabilityState {

		ONE {
			@Override
			public String test() {
				return "spring";
			}
		},

		TWO {
			@Override
			public String test() {
				return "boot";
			}
		};

		abstract String test();

	}

	static class CustomEventSource {

	}

}
