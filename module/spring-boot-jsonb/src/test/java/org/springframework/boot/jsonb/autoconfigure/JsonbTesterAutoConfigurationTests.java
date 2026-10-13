/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jsonb.autoconfigure;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.predicate.ReflectionHintsPredicates;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.aot.test.generate.TestGenerationContext;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.autoconfigure.json.JsonTestersAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.json.JsonbTester;
import org.springframework.context.aot.ApplicationContextAotGenerator;
import org.springframework.context.support.GenericApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JsonbTesterTestAutoConfiguration}.
 *
 * @author Andy Wilkinson
 */
class JsonbTesterAutoConfigurationTests {

	private final ApplicationContextRunner runner = new ApplicationContextRunner().withConfiguration(AutoConfigurations
		.of(JsonTestersAutoConfiguration.class, JsonbAutoConfiguration.class, JsonbTesterTestAutoConfiguration.class));

	@Test
	void hintsAreContributed() {
		this.runner.withPropertyValues("spring.test.jsontesters.enabled=true").prepare((context) -> {
			TestGenerationContext generationContext = new TestGenerationContext();
			new ApplicationContextAotGenerator().processAheadOfTime(
					(GenericApplicationContext) context.getSourceApplicationContext(), generationContext);
			ReflectionHintsPredicates hints = RuntimeHintsPredicates.reflection();
			assertThat(hints.onType(JsonbTester.class)).accepts(generationContext.getRuntimeHints());
		});
	}

}
