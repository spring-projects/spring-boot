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

package org.springframework.boot.jdbc.autoconfigure;

import java.util.Optional;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.boot.jdbc.autoconfigure.DataSourcePoolMetadataProvidersConfiguration.HikariDataSourcePoolMetadataRuntimeHints;
import org.springframework.boot.jdbc.autoconfigure.DataSourcePoolMetadataProvidersConfiguration.HikariPoolDataSourceMetadataProviderConfiguration;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.annotation.MergedAnnotations;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HikariDataSourcePoolMetadataRuntimeHints}.
 *
 * @author Andy Wilkinson
 */
class HikariDataSourcePoolMetadataRuntimeHintsTests {

	@Test
	@SuppressWarnings("rawtypes")
	void importsRegistrar() {
		Optional<Class[]> imported = MergedAnnotations.from(HikariPoolDataSourceMetadataProviderConfiguration.class)
			.get(ImportRuntimeHints.class)
			.getValue("value", Class[].class);
		assertThat(imported).hasValue(new Class[] { HikariDataSourcePoolMetadataRuntimeHints.class });
	}

	@Test
	void registersHints() {
		RuntimeHints runtimeHints = new RuntimeHints();
		new HikariDataSourcePoolMetadataRuntimeHints().registerHints(runtimeHints, getClass().getClassLoader());
		assertThat(HikariDataSource.class).hasDeclaredFields("pool");
		assertThat(RuntimeHintsPredicates.reflection().onFieldAccess(HikariDataSource.class, "pool"))
			.accepts(runtimeHints);
	}

}
