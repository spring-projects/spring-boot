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

package org.springframework.boot.jpa;

import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

import javax.sql.DataSource;

import jakarta.persistence.spi.PersistenceProvider;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import org.springframework.orm.jpa.persistenceunit.PersistenceUnitPostProcessor;
import org.springframework.orm.jpa.vendor.AbstractJpaVendorAdapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link EntityManagerFactoryBuilder}.
 *
 * @author Stephane Nicoll
 */
class EntityManagerFactoryBuilderTests {

	@Test
	void setPersistenceUnitPostProcessorsWhenEmpty() {
		EntityManagerFactoryBuilder builder = createEmptyBuilder();
		PersistenceUnitPostProcessor postProcessor = mock();
		PersistenceUnitPostProcessor postProcessor2 = mock();
		builder.setPersistenceUnitPostProcessors(postProcessor, postProcessor2);
		assertThat(builder).extracting("persistenceUnitPostProcessors")
			.asInstanceOf(InstanceOfAssertFactories.LIST)
			.containsExactly(postProcessor, postProcessor2);
	}

	@Test
	void addPersistenceUnitPostProcessorsWhenEmpty() {
		EntityManagerFactoryBuilder builder = createEmptyBuilder();
		PersistenceUnitPostProcessor postProcessor = mock();
		PersistenceUnitPostProcessor postProcessor2 = mock();
		builder.addPersistenceUnitPostProcessors(postProcessor, postProcessor2);
		assertThat(builder).extracting("persistenceUnitPostProcessors")
			.asInstanceOf(InstanceOfAssertFactories.LIST)
			.containsExactly(postProcessor, postProcessor2);
	}

	@Test
	void setPersistenceUnitPostProcessorsWhenNotEmpty() {
		EntityManagerFactoryBuilder builder = createEmptyBuilder();
		PersistenceUnitPostProcessor postProcessor = mock();
		builder.addPersistenceUnitPostProcessors(postProcessor);
		PersistenceUnitPostProcessor postProcessor2 = mock();
		PersistenceUnitPostProcessor postProcessor3 = mock();
		builder.setPersistenceUnitPostProcessors(postProcessor2, postProcessor3);
		assertThat(builder).extracting("persistenceUnitPostProcessors")
			.asInstanceOf(InstanceOfAssertFactories.LIST)
			.containsExactly(postProcessor2, postProcessor3);
	}

	@Test
	void addPersistenceUnitPostProcessorsWhenNotEmpty() {
		EntityManagerFactoryBuilder builder = createEmptyBuilder();
		PersistenceUnitPostProcessor postProcessor = mock();
		builder.addPersistenceUnitPostProcessors(postProcessor);
		PersistenceUnitPostProcessor postProcessor2 = mock();
		builder.addPersistenceUnitPostProcessors(postProcessor2);
		assertThat(builder).extracting("persistenceUnitPostProcessors")
			.asInstanceOf(InstanceOfAssertFactories.LIST)
			.containsExactly(postProcessor, postProcessor2);
	}

	private EntityManagerFactoryBuilder createEmptyBuilder() {
		Function<DataSource, Map<String, ?>> jpaPropertiesFactory = (dataSource) -> Collections.emptyMap();
		return new EntityManagerFactoryBuilder(new TestJpaVendorAdapter(), jpaPropertiesFactory, null);
	}

	static class TestJpaVendorAdapter extends AbstractJpaVendorAdapter {

		@Override
		public PersistenceProvider getPersistenceProvider() {
			return mock();
		}

	}

}
