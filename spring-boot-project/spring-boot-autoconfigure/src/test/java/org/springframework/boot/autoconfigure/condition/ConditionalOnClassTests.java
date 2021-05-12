/*
 * Copyright 2012-2019 the original author or authors.
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

import java.util.Collection;

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConditionalOnClass @ConditionalOnClass}.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 */
class ConditionalOnClassTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

	@Test
	void testVanillaOnClassCondition() {
		this.contextRunner.withUserConfiguration(BasicConfiguration.class, FooConfiguration.class)
				.run(this::hasBarBean);
	}

	@Test
	void testMissingOnClassCondition() {
		this.contextRunner.withUserConfiguration(MissingConfiguration.class, FooConfiguration.class).run((context) -> {
			assertThat(context).doesNotHaveBean("bar");
			assertThat(context).hasBean("foo");
			assertThat(context.getBean("foo")).isEqualTo("foo");
		});
	}

	@Test
	void testOnClassConditionWithXml() {
		this.contextRunner.withUserConfiguration(BasicConfiguration.class, XmlConfiguration.class)
				.run(this::hasBarBean);
	}

	@Test
	void testOnClassConditionWithCombinedXml() {
		this.contextRunner.withUserConfiguration(CombinedXmlConfiguration.class).run(this::hasBarBean);
	}

	@Test
	void onClassConditionOutputShouldNotContainConditionalOnMissingClassInMessage() {
		this.contextRunner.withUserConfiguration(BasicConfiguration.class).run((context) -> {
			Collection<ConditionEvaluationReport.ConditionAndOutcomes> conditionAndOutcomes = ConditionEvaluationReport
					.get(context.getSourceApplicationContext().getBeanFactory()).getConditionAndOutcomesBySource()
					.values();
			String message = conditionAndOutcomes.iterator().next().iterator().next().getOutcome().getMessage();
			assertThat(message).doesNotContain("@ConditionalOnMissingClass did not find unwanted class");
		});
	}

	private void hasBarBean(AssertableApplicationContext context) {
		assertThat(context).hasBean("bar");
		assertThat(context.getBean("bar")).isEqualTo("bar");
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(ConditionalOnClassTests.class)
	static class BasicConfiguration {

		@Bean
		String bar() {
			return "bar";
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(name = "FOO")
	static class MissingConfiguration {

		@Bean
		String bar() {
			return "bar";
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class FooConfiguration {

		@Bean
		String foo() {
			return "foo";
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ImportResource("org/springframework/boot/autoconfigure/condition/foo.xml")
	static class XmlConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@Import(BasicConfiguration.class)
	@ImportResource("org/springframework/boot/autoconfigure/condition/foo.xml")
	static class CombinedXmlConfiguration {

	}

}
