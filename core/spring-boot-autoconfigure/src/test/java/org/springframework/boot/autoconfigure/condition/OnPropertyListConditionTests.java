/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure.condition;

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link OnPropertyListCondition}.
 *
 * @author Stephane Nicoll
 */
class OnPropertyListConditionTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withUserConfiguration(TestConfig.class);

	@Test
	void propertyNotDefined() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean("foo"));
	}

	@Test
	void propertyDefinedAsCommaSeparated() {
		this.contextRunner.withPropertyValues("spring.test.my-list=value1")
			.run((context) -> assertThat(context).hasBean("foo"));
	}

	@Test
	void propertyDefinedAsList() {
		this.contextRunner.withPropertyValues("spring.test.my-list[0]=value1")
			.run((context) -> assertThat(context).hasBean("foo"));
	}

	@Test
	void propertyDefinedAsCommaSeparatedRelaxed() {
		this.contextRunner.withPropertyValues("spring.test.myList=value1")
			.run((context) -> assertThat(context).hasBean("foo"));
	}

	@Test
	void propertyDefinedAsListRelaxed() {
		this.contextRunner.withPropertyValues("spring.test.myList[0]=value1")
			.run((context) -> assertThat(context).hasBean("foo"));
	}

	@Configuration(proxyBeanMethods = false)
	@Conditional(TestPropertyListCondition.class)
	static class TestConfig {

		@Bean
		String foo() {
			return "foo";
		}

	}

	static class TestPropertyListCondition extends OnPropertyListCondition {

		TestPropertyListCondition() {
			super("spring.test.my-list", () -> ConditionMessage.forCondition("test"));
		}

	}

}
