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

package org.springframework.boot.data.r2dbc.autoconfigure;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.TestAutoConfigurationPackage;
import org.springframework.boot.data.r2dbc.domain.city.City;
import org.springframework.boot.r2dbc.autoconfigure.R2dbcAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.domain.ManagedTypes;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.mapping.R2dbcMappingContext;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DataR2dbcAutoConfiguration}.
 *
 * @author Mark Paluch
 */
class DataR2dbcAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(R2dbcAutoConfiguration.class, DataR2dbcAutoConfiguration.class));

	@Test
	void r2dbcEntityTemplateIsConfigured() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(R2dbcEntityTemplate.class));
	}

	@Test
	void entityScanShouldSetManagedTypes() {
		this.contextRunner.withUserConfiguration(TestConfiguration.class).run((context) -> {
			R2dbcMappingContext mappingContext = context.getBean(R2dbcMappingContext.class);
			ManagedTypes managedTypes = (ManagedTypes) ReflectionTestUtils.getField(mappingContext, "managedTypes");
			assertThat(managedTypes).isNotNull();
			assertThat(managedTypes.toList()).containsOnly(City.class);
		});
	}

	@TestAutoConfigurationPackage(City.class)
	static class TestConfiguration {

	}

}
