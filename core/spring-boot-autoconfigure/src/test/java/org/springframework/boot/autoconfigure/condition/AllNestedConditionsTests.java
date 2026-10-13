/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure.condition;

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AllNestedConditions}.
 */
class AllNestedConditionsTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

	@Test
	void neither() {
		this.contextRunner.withUserConfiguration(Config.class).run(match(false));
	}

	@Test
	void propertyA() {
		this.contextRunner.withUserConfiguration(Config.class).withPropertyValues("a:a").run(match(false));
	}

	@Test
	void propertyB() {
		this.contextRunner.withUserConfiguration(Config.class).withPropertyValues("b:b").run(match(false));
	}

	@Test
	void both() {
		this.contextRunner.withUserConfiguration(Config.class).withPropertyValues("a:a", "b:b").run(match(true));
	}

	private ContextConsumer<AssertableApplicationContext> match(boolean expected) {
		return (context) -> {
			if (expected) {
				assertThat(context).hasBean("myBean");
			}
			else {
				assertThat(context).doesNotHaveBean("myBean");
			}
		};
	}

	@Configuration(proxyBeanMethods = false)
	@Conditional(OnPropertyAAndBCondition.class)
	static class Config {

		@Bean
		String myBean() {
			return "myBean";
		}

	}

	static class OnPropertyAAndBCondition extends AllNestedConditions {

		OnPropertyAAndBCondition() {
			super(ConfigurationPhase.PARSE_CONFIGURATION);
		}

		@ConditionalOnProperty("a")
		static class HasPropertyA {

		}

		@ConditionalOnProperty("b")
		static class HasPropertyB {

		}

		@Conditional(NonSpringBootCondition.class)
		static class SubclassC {

		}

	}

	static class NonSpringBootCondition implements Condition {

		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
			return true;
		}

	}

}
