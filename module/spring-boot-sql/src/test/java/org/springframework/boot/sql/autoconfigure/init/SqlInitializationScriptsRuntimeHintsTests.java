/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.sql.autoconfigure.init;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SqlInitializationScriptsRuntimeHints}.
 *
 * @author Moritz Halbritter
 */
class SqlInitializationScriptsRuntimeHintsTests {

	@Test
	void shouldRegisterSchemaHints() {
		RuntimeHints hints = new RuntimeHints();
		new SqlInitializationScriptsRuntimeHints().registerHints(hints, getClass().getClassLoader());
		assertThat(RuntimeHintsPredicates.resource().forResource("schema.sql")).accepts(hints);
		assertThat(RuntimeHintsPredicates.resource().forResource("schema-all.sql")).accepts(hints);
		assertThat(RuntimeHintsPredicates.resource().forResource("schema-mysql.sql")).accepts(hints);
		assertThat(RuntimeHintsPredicates.resource().forResource("schema-postgres.sql")).accepts(hints);
		assertThat(RuntimeHintsPredicates.resource().forResource("schema-oracle.sql")).accepts(hints);
	}

	@Test
	void shouldRegisterDataHints() {
		RuntimeHints hints = new RuntimeHints();
		new SqlInitializationScriptsRuntimeHints().registerHints(hints, getClass().getClassLoader());
		assertThat(RuntimeHintsPredicates.resource().forResource("data.sql")).accepts(hints);
		assertThat(RuntimeHintsPredicates.resource().forResource("data-all.sql")).accepts(hints);
		assertThat(RuntimeHintsPredicates.resource().forResource("data-mysql.sql")).accepts(hints);
		assertThat(RuntimeHintsPredicates.resource().forResource("data-postgres.sql")).accepts(hints);
		assertThat(RuntimeHintsPredicates.resource().forResource("data-oracle.sql")).accepts(hints);
	}

}
