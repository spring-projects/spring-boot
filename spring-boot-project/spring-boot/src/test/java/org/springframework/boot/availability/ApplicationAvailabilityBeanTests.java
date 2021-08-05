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
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ApplicationAvailabilityBean}
 *
 * @author Brian Clozel
 * @author Phillip Webb
 */
class ApplicationAvailabilityBeanTests {

	private AnnotationConfigApplicationContext context;

	private ApplicationAvailabilityBean availability;

	private MockLog log;

	@BeforeEach
	void setup() {
		this.context = new AnnotationConfigApplicationContext(TestConfiguration.class);
		this.availability = this.context.getBean(ApplicationAvailabilityBean.class);
		this.log = this.context.getBean(MockLog.class);
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
	void stateChangesAreLogged() {
		AvailabilityChangeEvent.publish(this.context, LivenessState.CORRECT);
		assertThat(this.log.getLogged()).contains("Application availability state LivenessState changed to CORRECT");
		AvailabilityChangeEvent.publish(this.context, LivenessState.BROKEN);
		assertThat(this.log.getLogged())
				.contains("Application availability state LivenessState changed from CORRECT to BROKEN");
	}

	@Test
	void stateChangesAreLoggedWithExceptionSource() {
		AvailabilityChangeEvent.publish(this.context, new IOException("connection error"), LivenessState.BROKEN);
		assertThat(this.log.getLogged()).contains("Application availability state LivenessState changed to BROKEN: "
				+ "java.io.IOException: connection error");
	}

	@Test
	void stateChangesAreLoggedWithOtherSource() {
		AvailabilityChangeEvent.publish(this.context, new CustomEventSource(), LivenessState.BROKEN);
		assertThat(this.log.getLogged()).contains(
				"Application availability state LivenessState changed to BROKEN: " + CustomEventSource.class.getName());
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

	@Configuration
	static class TestConfiguration {

		@Bean
		MockLog mockLog() {
			List<String> logged = new ArrayList<>();
			MockLog log = mock(MockLog.class);
			given(log.isDebugEnabled()).willReturn(true);
			given(log.getLogged()).willReturn(logged);
			willAnswer((invocation) -> logged.add("" + invocation.getArguments()[0])).given(log).debug(any());
			return log;
		}

		@Bean
		ApplicationAvailabilityBean applicationAvailabilityBean(MockLog log) {
			return new ApplicationAvailabilityBean(log);
		}

	}

	interface MockLog extends Log {

		List<String> getLogged();

	}

}
