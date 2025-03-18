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

package org.springframework.boot.autoconfigure.sql.init;

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
