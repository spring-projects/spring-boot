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

package org.springframework.boot.test.autoconfigure.json;

import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.ReflectionHintsPredicates;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.aot.test.generate.TestGenerationContext;
import org.springframework.boot.autoconfigure.gson.GsonAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.jsonb.JsonbAutoConfiguration;
import org.springframework.boot.test.json.BasicJsonTester;
import org.springframework.boot.test.json.GsonTester;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonbTester;
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
	void withNoMarshallersOnlyBasicJsonTesterHintsAreContributed() {
		jsonTesters((runtimeHints) -> {
			ReflectionHintsPredicates reflection = RuntimeHintsPredicates.reflection();
			assertThat(reflection.onType(BasicJsonTester.class)).accepts(runtimeHints);
			assertThat(reflection.onType(JacksonTester.class).negate()).accepts(runtimeHints);
			assertThat(reflection.onType(JsonbTester.class).negate()).accepts(runtimeHints);
			assertThat(reflection.onType(GsonTester.class).negate()).accepts(runtimeHints);
		});
	}

	@Test
	void withObjectMapperBeanJacksonTesterHintsAreContributed() {
		jsonTestersWith(JacksonAutoConfiguration.class, (runtimeHints) -> {
			ReflectionHintsPredicates reflection = RuntimeHintsPredicates.reflection();
			assertThat(reflection.onType(BasicJsonTester.class)).accepts(runtimeHints);
			assertThat(reflection.onType(JacksonTester.class)).accepts(runtimeHints);
			assertThat(reflection.onType(JsonbTester.class).negate()).accepts(runtimeHints);
			assertThat(reflection.onType(GsonTester.class).negate()).accepts(runtimeHints);
		});
	}

	@Test
	void withGsonBeanGsonTesterHintsAreContributed() {
		jsonTestersWith(GsonAutoConfiguration.class, (runtimeHints) -> {
			ReflectionHintsPredicates reflection = RuntimeHintsPredicates.reflection();
			assertThat(reflection.onType(BasicJsonTester.class)).accepts(runtimeHints);
			assertThat(reflection.onType(JacksonTester.class).negate()).accepts(runtimeHints);
			assertThat(reflection.onType(JsonbTester.class).negate()).accepts(runtimeHints);
			assertThat(reflection.onType(GsonTester.class)).accepts(runtimeHints);
		});
	}

	@Test
	void withJsonbBeanJsonbTesterHintsAreContributed() {
		jsonTestersWith(JsonbAutoConfiguration.class, (runtimeHints) -> {
			ReflectionHintsPredicates reflection = RuntimeHintsPredicates.reflection();
			assertThat(reflection.onType(BasicJsonTester.class)).accepts(runtimeHints);
			assertThat(reflection.onType(JacksonTester.class).negate()).accepts(runtimeHints);
			assertThat(reflection.onType(JsonbTester.class)).accepts(runtimeHints);
			assertThat(reflection.onType(GsonTester.class).negate()).accepts(runtimeHints);
		});
	}

	private void jsonTesters(Consumer<RuntimeHints> hintsConsumer) {
		jsonTestersWith(null, hintsConsumer);
	}

	private void jsonTestersWith(Class<?> configuration, Consumer<RuntimeHints> hintsConsumer) {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
			TestPropertyValues.of("spring.test.jsontesters.enabled=true").applyTo(context);
			if (configuration != null) {
				context.register(configuration);
			}
			context.register(JsonTestersAutoConfiguration.class);
			TestGenerationContext generationContext = new TestGenerationContext();
			new ApplicationContextAotGenerator().processAheadOfTime(context, generationContext);
			hintsConsumer.accept(generationContext.getRuntimeHints());
		}
	}

}
