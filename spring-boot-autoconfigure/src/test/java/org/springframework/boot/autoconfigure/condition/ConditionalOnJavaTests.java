/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.autoconfigure.condition;

import org.hamcrest.Matcher;
import org.junit.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnJava.JavaVersion;
import org.springframework.boot.autoconfigure.condition.ConditionalOnJava.Range;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link ConditionalOnJava}.
 *
 * @author Oliver Gierke
 * @author Phillip Webb
 */
public class ConditionalOnJavaTests {

	private final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	private final OnJavaCondition condition = new OnJavaCondition();

	@Test
	public void doesNotMatchIfBetterVersionIsRequired() {
		registerAndRefresh(Java9Required.class);
		assertPresent(false);
	}

	@Test
	public void doesNotMatchIfLowerIsRequired() {
		registerAndRefresh(Java5Required.class);
		assertPresent(false);
	}

	@Test
	public void matchesIfVersionIsInRange() {
		registerAndRefresh(Java6Required.class);
		assertPresent(true);
	}

	@Test
	public void boundsTests() throws Exception {
		testBounds(Range.EQUAL_OR_NEWER, JavaVersion.SEVEN, JavaVersion.SIX, true);
		testBounds(Range.EQUAL_OR_NEWER, JavaVersion.SEVEN, JavaVersion.SEVEN, true);
		testBounds(Range.EQUAL_OR_NEWER, JavaVersion.SEVEN, JavaVersion.EIGHT, false);
		testBounds(Range.OLDER_THAN, JavaVersion.SEVEN, JavaVersion.SIX, false);
		testBounds(Range.OLDER_THAN, JavaVersion.SEVEN, JavaVersion.SEVEN, false);
		testBounds(Range.OLDER_THAN, JavaVersion.SEVEN, JavaVersion.EIGHT, true);
	}

	@Test
	public void equalOrNewerMessage() throws Exception {
		ConditionOutcome outcome = this.condition.getMatchOutcome(Range.EQUAL_OR_NEWER,
				JavaVersion.SEVEN, JavaVersion.SIX);
		assertThat(outcome.getMessage(), equalTo("Required JVM version "
				+ "1.6 or newer found 1.7"));
	}

	@Test
	public void olderThanMessage() throws Exception {
		ConditionOutcome outcome = this.condition.getMatchOutcome(Range.OLDER_THAN,
				JavaVersion.SEVEN, JavaVersion.SIX);
		assertThat(outcome.getMessage(), equalTo("Required JVM version "
				+ "older than 1.6 found 1.7"));
	}

	private void testBounds(Range range, JavaVersion runningVersion, JavaVersion version,
			boolean expected) {
		ConditionOutcome outcome = this.condition.getMatchOutcome(range, runningVersion,
				version);
		assertThat(outcome.getMessage(), outcome.isMatch(), equalTo(expected));
	}

	private void registerAndRefresh(Class<?> annotatedClasses) {
		this.context.register(annotatedClasses);
		this.context.refresh();
	}

	private void assertPresent(boolean expected) {
		int expectedNumber = expected ? 1 : 0;
		Matcher<Iterable<String>> matcher = iterableWithSize(expectedNumber);
		assertThat(this.context.getBeansOfType(String.class).values(), is(matcher));
	}

	@Configuration
	@ConditionalOnJava(JavaVersion.NINE)
	static class Java9Required {
		@Bean
		String foo() {
			return "foo";
		}
	}

	@Configuration
	@ConditionalOnJava(range = Range.OLDER_THAN, value = JavaVersion.SIX)
	static class Java5Required {
		@Bean
		String foo() {
			return "foo";
		}
	}

	@Configuration
	@ConditionalOnJava(JavaVersion.SIX)
	static class Java6Required {
		@Bean
		String foo() {
			return "foo";
		}
	}

}
