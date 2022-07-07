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

package org.springframework.boot.autoconfigure.data.cassandra;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.cassandra.CassandraAutoConfiguration;
import org.springframework.boot.autoconfigure.data.cassandra.city.City;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.core.ReactiveCassandraTemplate;
import org.springframework.data.cassandra.core.convert.CassandraConverter;
import org.springframework.data.cassandra.core.mapping.CassandraMappingContext;
import org.springframework.data.cassandra.core.mapping.SimpleUserTypeResolver;
import org.springframework.data.domain.ManagedTypes;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CassandraReactiveDataAutoConfiguration}.
 *
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @author Mark Paluch
 */
class CassandraReactiveDataAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@AfterEach
	void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	void templateExists() {
		load("spring.data.cassandra.keyspaceName:boot_test");
		assertThat(this.context.getBeanNamesForType(ReactiveCassandraTemplate.class)).hasSize(1);
	}

	@Test
	void entityScanShouldSetManagedTypes() {
		load(EntityScanConfig.class, "spring.data.cassandra.keyspaceName:boot_test");
		CassandraMappingContext mappingContext = this.context.getBean(CassandraMappingContext.class);
		ManagedTypes managedTypes = (ManagedTypes) ReflectionTestUtils.getField(mappingContext, "managedTypes");
		assertThat(managedTypes.toList()).containsOnly(City.class);
	}

	@Test
	void userTypeResolverShouldBeSet() {
		load("spring.data.cassandra.keyspaceName:boot_test");
		CassandraConverter cassandraConverter = this.context.getBean(CassandraConverter.class);
		assertThat(cassandraConverter).extracting("userTypeResolver").isInstanceOf(SimpleUserTypeResolver.class);
	}

	private void load(String... environment) {
		load(null, environment);
	}

	private void load(Class<?> config, String... environment) {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		TestPropertyValues.of(environment).applyTo(ctx);
		if (config != null) {
			ctx.register(config);
		}
		ctx.register(CassandraMockConfiguration.class, CassandraAutoConfiguration.class,
				CassandraDataAutoConfiguration.class, CassandraReactiveDataAutoConfiguration.class);
		ctx.refresh();
		this.context = ctx;
	}

	@Configuration(proxyBeanMethods = false)
	@EntityScan("org.springframework.boot.autoconfigure.data.cassandra.city")
	static class EntityScanConfig {

	}

}
