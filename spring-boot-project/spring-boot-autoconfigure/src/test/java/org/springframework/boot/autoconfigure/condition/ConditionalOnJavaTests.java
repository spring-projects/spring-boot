/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.autoconfigure.condition;

import java.io.Console;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnJre;
import org.junit.jupiter.api.condition.JRE;

import org.springframework.boot.autoconfigure.condition.ConditionalOnJava.Range;
import org.springframework.boot.system.JavaVersion;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConditionalOnJava @ConditionalOnJava}.
 *
 * @author Oliver Gierke
 * @author Phillip Webb
 */
class ConditionalOnJavaTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

	private final OnJavaCondition condition = new OnJavaCondition();

	@Test
	@EnabledOnJre(JRE.JAVA_17)
	void doesNotMatchIfBetterVersionIsRequired() {
		this.contextRunner.withUserConfiguration(Java18Required.class)
				.run((context) -> assertThat(context).doesNotHaveBean(String.class));
	}

	@Test
	@EnabledOnJre(JRE.JAVA_18)
	void doesNotMatchIfLowerIsRequired() {
		this.contextRunner.withUserConfiguration(OlderThan18Required.class)
				.run((context) -> assertThat(context).doesNotHaveBean(String.class));
	}

	@Test
	void matchesIfVersionIsInRange() {
		this.contextRunner.withUserConfiguration(Java17Required.class)
				.run((context) -> assertThat(context).hasSingleBean(String.class));
	}

	@Test
	void boundsTests() {
		testBounds(Range.EQUAL_OR_NEWER, JavaVersion.EIGHTEEN, JavaVersion.SEVENTEEN, true);
		testBounds(Range.EQUAL_OR_NEWER, JavaVersion.SEVENTEEN, JavaVersion.SEVENTEEN, true);
		testBounds(Range.EQUAL_OR_NEWER, JavaVersion.SEVENTEEN, JavaVersion.EIGHTEEN, false);
		testBounds(Range.OLDER_THAN, JavaVersion.EIGHTEEN, JavaVersion.SEVENTEEN, false);
		testBounds(Range.OLDER_THAN, JavaVersion.SEVENTEEN, JavaVersion.SEVENTEEN, false);
		testBounds(Range.OLDER_THAN, JavaVersion.SEVENTEEN, JavaVersion.EIGHTEEN, true);
	}

	@Test
	void equalOrNewerMessage() {
		ConditionOutcome outcome = this.condition.getMatchOutcome(Range.EQUAL_OR_NEWER, JavaVersion.EIGHTEEN,
				JavaVersion.SEVENTEEN);
		assertThat(outcome.getMessage()).isEqualTo("@ConditionalOnJava (17 or newer) found 18");
	}

	@Test
	void olderThanMessage() {
		ConditionOutcome outcome = this.condition.getMatchOutcome(Range.OLDER_THAN, JavaVersion.EIGHTEEN,
				JavaVersion.SEVENTEEN);
		assertThat(outcome.getMessage()).isEqualTo("@ConditionalOnJava (older than 17) found 18");
	}

	@Test
	@EnabledOnJre(JRE.JAVA_17)
	void java17IsDetected() throws Exception {
		assertThat(getJavaVersion()).isEqualTo("17");
	}

	@Test
	@EnabledOnJre(JRE.JAVA_17)
	void java17IsTheFallback() throws Exception {
		assertThat(getJavaVersion(Console.class)).isEqualTo("17");
	}

	private String getJavaVersion(Class<?>... hiddenClasses) throws Exception {
		FilteredClassLoader classLoader = new FilteredClassLoader(hiddenClasses);
		Class<?> javaVersionClass = Class.forName(JavaVersion.class.getName(), false, classLoader);
		Method getJavaVersionMethod = ReflectionUtils.findMethod(javaVersionClass, "getJavaVersion");
		Object javaVersion = ReflectionUtils.invokeMethod(getJavaVersionMethod, null);
		classLoader.close();
		return javaVersion.toString();
	}

	private void testBounds(Range range, JavaVersion runningVersion, JavaVersion version, boolean expected) {
		ConditionOutcome outcome = this.condition.getMatchOutcome(range, runningVersion, version);
		assertThat(outcome.isMatch()).as(outcome.getMessage()).isEqualTo(expected);
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnJava(JavaVersion.SEVENTEEN)
	static class Java17Required {

		@Bean
		String foo() {
			return "foo";
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnJava(range = Range.OLDER_THAN, value = JavaVersion.EIGHTEEN)
	static class OlderThan18Required {

		@Bean
		String foo() {
			return "foo";
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnJava(JavaVersion.EIGHTEEN)
	static class Java18Required {

		@Bean
		String foo() {
			return "foo";
		}

	}

}
