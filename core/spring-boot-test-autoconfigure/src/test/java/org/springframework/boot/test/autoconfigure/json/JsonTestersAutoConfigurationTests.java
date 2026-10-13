/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.autoconfigure.json;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.ReflectionHintsPredicates;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.aot.test.generate.TestGenerationContext;
import org.springframework.boot.test.json.BasicJsonTester;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.aot.ApplicationContextAotGenerator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JsonTestersAutoConfiguration}.
 *
 * @author Andy Wilkinson
 */
class JsonTestersAutoConfigurationTests {

	@Test
	void basicJsonTesterHintsAreContributed() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
			TestPropertyValues.of("spring.test.jsontesters.enabled=true").applyTo(context);
			context.register(JsonTestersAutoConfiguration.class);
			TestGenerationContext generationContext = new TestGenerationContext();
			new ApplicationContextAotGenerator().processAheadOfTime(context, generationContext);
			RuntimeHints runtimeHints = generationContext.getRuntimeHints();
			ReflectionHintsPredicates reflection = RuntimeHintsPredicates.reflection();
			assertThat(reflection.onType(BasicJsonTester.class)).accepts(runtimeHints);
		}
	}

}
