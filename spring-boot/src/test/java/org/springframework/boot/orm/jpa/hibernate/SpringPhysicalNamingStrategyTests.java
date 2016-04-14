/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.service.ServiceRegistry;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SpringPhysicalNamingStrategy}.
 *
 * @author Phillip Webb
 */
public class SpringPhysicalNamingStrategyTests {

	private Metadata metadata;

	@Before
	public void setup() throws Exception {
		MetadataSources metadataSources = new MetadataSources();
		metadataSources.addAnnotatedClass(TelephoneNumber.class);
		StandardServiceRegistry serviceRegistry = getServiceRegistry(metadataSources);
		this.metadata = metadataSources.getMetadataBuilder(serviceRegistry)
				.applyPhysicalNamingStrategy(new SpringPhysicalNamingStrategy()).build();
	}

	private StandardServiceRegistry getServiceRegistry(MetadataSources metadataSources) {
		ServiceRegistry registry = metadataSources.getServiceRegistry();
		return new StandardServiceRegistryBuilder((BootstrapServiceRegistry) registry)
				.applySetting(AvailableSettings.DIALECT, H2Dialect.class).build();
	}

	@Test
	public void tableNameShouldBeLowercaseUnderscore() throws Exception {
		PersistentClass binding = this.metadata
				.getEntityBinding(TelephoneNumber.class.getName());
		assertThat(binding.getTable().getQuotedName()).isEqualTo("telephone_number");
	}

}
