/*
 * Copyright 2012-2017 the original author or authors.
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

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.ServiceLoader;
import java.util.function.Function;

import org.junit.Test;

import org.springframework.boot.autoconfigure.condition.ConditionalOnJava.Range;
import org.springframework.boot.system.JavaVersion;
import org.springframework.boot.test.context.HideClassesClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testsupport.Assume;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConditionalOnJava}.
 *
 * @author Oliver Gierke
 * @author Phillip Webb
 */
public class ConditionalOnJavaTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

	private final OnJavaCondition condition = new OnJavaCondition();

	@Test
	public void doesNotMatchIfBetterVersionIsRequired() {
		Assume.javaEight();
		this.contextRunner.withUserConfiguration(Java9Required.class)
				.run((context) -> assertThat(context).doesNotHaveBean(String.class));
	}

	@Test
	public void doesNotMatchIfLowerIsRequired() {
		this.contextRunner.withUserConfiguration(Java7Required.class)
				.run((context) -> assertThat(context).doesNotHaveBean(String.class));
	}

	@Test
	public void matchesIfVersionIsInRange() {
		this.contextRunner.withUserConfiguration(Java8Required.class)
				.run((context) -> assertThat(context).hasSingleBean(String.class));
	}

	@Test
	public void boundsTests() throws Exception {
		testBounds(Range.EQUAL_OR_NEWER, JavaVersion.NINE, JavaVersion.EIGHT, true);
		testBounds(Range.EQUAL_OR_NEWER, JavaVersion.EIGHT, JavaVersion.EIGHT, true);
		testBounds(Range.EQUAL_OR_NEWER, JavaVersion.EIGHT, JavaVersion.NINE, false);
		testBounds(Range.OLDER_THAN, JavaVersion.NINE, JavaVersion.EIGHT, false);
		testBounds(Range.OLDER_THAN, JavaVersion.EIGHT, JavaVersion.EIGHT, false);
		testBounds(Range.OLDER_THAN, JavaVersion.EIGHT, JavaVersion.NINE, true);
	}

	@Test
	public void equalOrNewerMessage() throws Exception {
		ConditionOutcome outcome = this.condition.getMatchOutcome(Range.EQUAL_OR_NEWER,
				JavaVersion.NINE, JavaVersion.EIGHT);
		assertThat(outcome.getMessage())
				.isEqualTo("@ConditionalOnJava (1.8 or newer) found 1.9");
	}

	@Test
	public void olderThanMessage() throws Exception {
		ConditionOutcome outcome = this.condition.getMatchOutcome(Range.OLDER_THAN,
				JavaVersion.NINE, JavaVersion.EIGHT);
		assertThat(outcome.getMessage())
				.isEqualTo("@ConditionalOnJava (older than 1.8) found 1.9");
	}

	@Test
	public void java8IsDetected() throws Exception {
		Assume.javaEight();
		assertThat(getJavaVersion()).isEqualTo("1.8");
	}

	@Test
	public void java8IsTheFallback() throws Exception {
		Assume.javaEight();
		assertThat(getJavaVersion(Function.class, Files.class, ServiceLoader.class))
				.isEqualTo("1.8");
	}

	private String getJavaVersion(Class<?>... hiddenClasses) throws Exception {
		HideClassesClassLoader classLoader = new HideClassesClassLoader(hiddenClasses);
		Class<?> javaVersionClass = classLoader.loadClass(JavaVersion.class.getName());
		Method getJavaVersionMethod = ReflectionUtils.findMethod(javaVersionClass,
				"getJavaVersion");
		Object javaVersion = ReflectionUtils.invokeMethod(getJavaVersionMethod, null);
		classLoader.close();
		return javaVersion.toString();
	}

	private void testBounds(Range range, JavaVersion runningVersion, JavaVersion version,
			boolean expected) {
		ConditionOutcome outcome = this.condition.getMatchOutcome(range, runningVersion,
				version);
		assertThat(outcome.isMatch()).as(outcome.getMessage()).isEqualTo(expected);
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
	@ConditionalOnJava(range = Range.OLDER_THAN, value = JavaVersion.EIGHT)
	static class Java7Required {

		@Bean
		String foo() {
			return "foo";
		}

	}

	@Configuration
	@ConditionalOnJava(JavaVersion.EIGHT)
	static class Java8Required {

		@Bean
		String foo() {
			return "foo";
		}

	}

}
