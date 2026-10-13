/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure.condition;

import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link SpringBootCondition}.
 *
 * @author Phillip Webb
 */
@SuppressWarnings("resource")
class SpringBootConditionTests {

	@Test
	void sensibleClassException() {
		assertThatIllegalStateException().isThrownBy(() -> new AnnotationConfigApplicationContext(ErrorOnClass.class))
			.withMessageContaining("Error processing condition on " + ErrorOnClass.class.getName());
	}

	@Test
	void sensibleMethodException() {
		assertThatIllegalStateException().isThrownBy(() -> new AnnotationConfigApplicationContext(ErrorOnMethod.class))
			.withMessageContaining("Error processing condition on " + ErrorOnMethod.class.getName() + ".myBean");
	}

	@Configuration(proxyBeanMethods = false)
	@Conditional(AlwaysThrowsCondition.class)
	static class ErrorOnClass {

	}

	@Configuration(proxyBeanMethods = false)
	static class ErrorOnMethod {

		@Bean
		@Conditional(AlwaysThrowsCondition.class)
		String myBean() {
			return "bean";
		}

	}

	static class AlwaysThrowsCondition extends SpringBootCondition {

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
			throw new RuntimeException("Oh no!");
		}

	}

}
