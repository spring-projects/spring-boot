/*
 * Copyright 2012-2025 the original author or authors.
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

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.cassandra.CassandraAutoConfiguration;
import org.springframework.boot.autoconfigure.data.cassandra.city.City;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.core.ReactiveCassandraTemplate;
import org.springframework.data.cassandra.core.convert.CassandraConverter;
import org.springframework.data.cassandra.core.cql.ReactiveCqlTemplate;
import org.springframework.data.cassandra.core.mapping.CassandraMappingContext;
import org.springframework.data.cassandra.core.mapping.SimpleUserTypeResolver;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CassandraReactiveDataAutoConfiguration}.
 *
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @author Mark Paluch
 */
class CassandraReactiveDataAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withPropertyValues("spring.cassandra.keyspaceName=boot_test")
		.withUserConfiguration(CassandraMockConfiguration.class)
		.withConfiguration(AutoConfigurations.of(CassandraAutoConfiguration.class, CassandraDataAutoConfiguration.class,
				CassandraReactiveDataAutoConfiguration.class));

	@Test
	void reactiveCqlTemplateExists() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(ReactiveCqlTemplate.class));
	}

	@Test
	void templateExists() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(ReactiveCassandraTemplate.class));
	}

	@Test
	void templateUsesReactiveCqlTemplate() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(ReactiveCassandraTemplate.class);
			assertThat(context.getBean(ReactiveCassandraTemplate.class).getReactiveCqlOperations())
				.isSameAs(context.getBean(ReactiveCqlTemplate.class));
		});
	}

	@Test
	void entityScanShouldSetManagedTypes() {
		this.contextRunner.withUserConfiguration(EntityScanConfig.class).run((context) -> {
			assertThat(context).hasSingleBean(CassandraMappingContext.class);
			CassandraMappingContext mappingContext = context.getBean(CassandraMappingContext.class);
			assertThat(mappingContext.getManagedTypes()).singleElement()
				.satisfies((typeInformation) -> assertThat(typeInformation.getType()).isEqualTo(City.class));
		});
	}

	@Test
	void userTypeResolverShouldBeSet() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(CassandraConverter.class);
			assertThat(context.getBean(CassandraConverter.class)).extracting("userTypeResolver")
				.isInstanceOf(SimpleUserTypeResolver.class);
		});
	}

	@Configuration(proxyBeanMethods = false)
	@EntityScan("org.springframework.boot.autoconfigure.data.cassandra.city")
	static class EntityScanConfig {

	}

}
