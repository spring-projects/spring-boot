/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.cassandra.autoconfigure;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.cassandra.autoconfigure.CassandraAutoConfiguration;
import org.springframework.boot.data.cassandra.domain.city.City;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.core.ReactiveCassandraTemplate;
import org.springframework.data.cassandra.core.convert.CassandraConverter;
import org.springframework.data.cassandra.core.cql.ReactiveCqlTemplate;
import org.springframework.data.cassandra.core.mapping.CassandraMappingContext;
import org.springframework.data.cassandra.core.mapping.SimpleUserTypeResolver;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DataCassandraReactiveAutoConfiguration}.
 *
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @author Mark Paluch
 */
class DataCassandraReactiveAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withPropertyValues("spring.cassandra.keyspaceName=boot_test")
		.withUserConfiguration(CassandraMockConfiguration.class)
		.withConfiguration(AutoConfigurations.of(CassandraAutoConfiguration.class, DataCassandraAutoConfiguration.class,
				DataCassandraReactiveAutoConfiguration.class));

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
	@EntityScan("org.springframework.boot.data.cassandra.domain.city")
	static class EntityScanConfig {

	}

}
