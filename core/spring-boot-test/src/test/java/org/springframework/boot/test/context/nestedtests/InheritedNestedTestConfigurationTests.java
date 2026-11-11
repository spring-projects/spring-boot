/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.context.nestedtests;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.nestedtests.InheritedNestedTestConfigurationTests.ActionPerformer;
import org.springframework.boot.test.context.nestedtests.InheritedNestedTestConfigurationTests.AppConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

/**
 * Tests for nested test configuration when the configuration is inherited from the
 * enclosing class (the default behaviour).
 *
 * @author Andy Wilkinson
 */
@SpringBootTest(classes = AppConfiguration.class)
@Import(ActionPerformer.class)
class InheritedNestedTestConfigurationTests {

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
	class InnerTests {

		@Test
		void mockWasInvokedOnce() {
			InheritedNestedTestConfigurationTests.this.performer.run();
			then(InheritedNestedTestConfigurationTests.this.action).should().perform();
		}

		@Test
		void mockWasInvokedTwice() {
			InheritedNestedTestConfigurationTests.this.performer.run();
			InheritedNestedTestConfigurationTests.this.performer.run();
			then(InheritedNestedTestConfigurationTests.this.action).should(times(2)).perform();
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

}
