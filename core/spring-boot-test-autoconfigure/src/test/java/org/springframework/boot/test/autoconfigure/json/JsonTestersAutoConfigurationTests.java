/*
 * Copyright 2012-present the original author or authors.
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
