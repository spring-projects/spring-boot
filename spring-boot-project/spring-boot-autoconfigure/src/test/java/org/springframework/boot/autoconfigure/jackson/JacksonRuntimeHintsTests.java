package org.springframework.boot.autoconfigure.jackson;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import org.junit.jupiter.api.Test;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.ReflectionHintsPredicates;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class JacksonRuntimeHintsTests {

	@Test
	void shouldRegisterHints() {
		shouldRegisterFieldHintsFor(PropertyNamingStrategies.class,
				"LOWER_CAMEL_CASE", "UPPER_CAMEL_CASE",
				"SNAKE_CASE", "UPPER_SNAKE_CASE",
				"LOWER_CASE", "KEBAB_CASE", "LOWER_DOT_CASE");
	}

	@Test
	void shouldRegisterJackson_2_12_pre_Hints() {
		shouldRegisterFieldHintsFor(PropertyNamingStrategy.class,
				"LOWER_CAMEL_CASE", "UPPER_CAMEL_CASE",
				"SNAKE_CASE",
				"LOWER_CASE", "KEBAB_CASE", "LOWER_DOT_CASE");
	}

	private void shouldRegisterFieldHintsFor(Class<?> clazz, String... fieldNames) {
		RuntimeHints hints = new RuntimeHints();
		new JacksonRuntimeHints().registerHints(hints, getClass().getClassLoader());
		ReflectionHintsPredicates reflection = RuntimeHintsPredicates.reflection();
		Stream.of(fieldNames)
				.map(name -> reflection.onField(clazz, name))
				.forEach(predicate -> assertThat(predicate).accepts(hints));
	}

}
