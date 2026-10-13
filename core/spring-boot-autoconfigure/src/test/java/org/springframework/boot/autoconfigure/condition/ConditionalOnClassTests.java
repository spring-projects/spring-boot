/*
 * Copyright 2012-present the original author or authors.
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
				.get(context.getSourceApplicationContext().getBeanFactory())
				.getConditionAndOutcomesBySource()
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
