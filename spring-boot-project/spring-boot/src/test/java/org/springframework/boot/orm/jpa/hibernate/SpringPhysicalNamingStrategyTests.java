/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.orm.jpa.hibernate;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.mapping.PersistentClass;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SpringPhysicalNamingStrategy}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class SpringPhysicalNamingStrategyTests {

	private Metadata metadata;

	private MetadataSources metadataSources;

	@BeforeEach
	public void setup() {
		this.metadataSources = new MetadataSources(createServiceRegistry());
		this.metadataSources.addAnnotatedClass(TelephoneNumber.class);
		this.metadata = this.metadataSources.getMetadataBuilder()
				.applyPhysicalNamingStrategy(new SpringPhysicalNamingStrategy()).build();
	}

	private StandardServiceRegistry createServiceRegistry() {
		return new StandardServiceRegistryBuilder().applySetting(AvailableSettings.DIALECT, H2Dialect.class).build();
	}

	@Test
	void tableNameShouldBeLowercaseUnderscore() {
		PersistentClass binding = this.metadata.getEntityBinding(TelephoneNumber.class.getName());
		assertThat(binding.getTable().getQuotedName()).isEqualTo("telephone_number");
	}

	@Test
	void tableNameShouldNotBeLowerCaseIfCaseSensitive() {
		this.metadata = this.metadataSources.getMetadataBuilder()
				.applyPhysicalNamingStrategy(new TestSpringPhysicalNamingStrategy()).build();
		PersistentClass binding = this.metadata.getEntityBinding(TelephoneNumber.class.getName());
		assertThat(binding.getTable().getQuotedName()).isEqualTo("Telephone_Number");
	}

	private class TestSpringPhysicalNamingStrategy extends SpringPhysicalNamingStrategy {

		@Override
		protected boolean isCaseInsensitive(JdbcEnvironment jdbcEnvironment) {
			return false;
		}

	}

}
