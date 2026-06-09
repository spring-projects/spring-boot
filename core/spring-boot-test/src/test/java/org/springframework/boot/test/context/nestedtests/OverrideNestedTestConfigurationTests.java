/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.test.context.nestedtests;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;
import org.springframework.test.context.NestedTestConfiguration;
import org.springframework.test.context.NestedTestConfiguration.EnclosingConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

/**
 * Tests for nested test configuration when the configuration is overridden in the nested
 * class using {@link NestedTestConfiguration @NestedTestConfiguration(OVERRIDE)}.
 *
 * @author Muhammadjon Saidov
 */
@SpringBootTest(classes = OverrideNestedTestConfigurationTests.AppConfiguration.class)
@Import(OverrideNestedTestConfigurationTests.ActionPerformer.class)
class OverrideNestedTestConfigurationTests {

	@MockitoBean
	Action action;

	@Autowired
	ActionPerformer performer;

	@Test
	void mockWasInvokedOnce() {
		this.performer.run();
		then(this.action).should().perform();
	}

	@Test
	void mockWasInvokedTwice() {
		this.performer.run();
		this.performer.run();
		then(this.action).should(times(2)).perform();
	}

	@Nested
	@NestedTestConfiguration(EnclosingConfiguration.OVERRIDE)
	@SpringBootTest(classes = OverrideNestedTestConfigurationTests.InnerAppConfiguration.class)
	@Import(OverrideNestedTestConfigurationTests.ActionPerformer.class)
	class InnerTests {

		@MockitoBean
		Action action;

		@Autowired
		ActionPerformer performer;

		@Test
		void mockWasInvokedOnce() {
			this.performer.run();
			then(this.action).should().perform();
		}

		@Test
		void mockWasInvokedTwice() {
			this.performer.run();
			this.performer.run();
			then(this.action).should(times(2)).perform();
		}

	}

	@Component
	static class ActionPerformer {

		private final Action action;

		ActionPerformer(Action action) {
			this.action = action;
		}

		void run() {
			this.action.perform();
		}

	}

	public interface Action {

		void perform();

	}

	@SpringBootConfiguration
	static class AppConfiguration {

	}

	@SpringBootConfiguration
	static class InnerAppConfiguration {

	}

}
