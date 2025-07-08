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

package org.springframework.boot.data.cassandra.autoconfigure;

import java.util.Collections;

import com.datastax.oss.driver.api.core.CqlSession;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.cassandra.autoconfigure.CassandraAutoConfiguration;
import org.springframework.boot.data.cassandra.domain.city.City;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.data.cassandra.core.convert.CassandraConverter;
import org.springframework.data.cassandra.core.convert.CassandraCustomConversions;
import org.springframework.data.cassandra.core.cql.CqlTemplate;
import org.springframework.data.cassandra.core.mapping.CassandraMappingContext;
import org.springframework.data.cassandra.core.mapping.SimpleUserTypeResolver;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CassandraDataAutoConfiguration}.
 *
 * @author Eddú Meléndez
 * @author Mark Paluch
 * @author Stephane Nicoll
 */
class CassandraDataAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withPropertyValues("spring.cassandra.keyspaceName=boot_test")
		.withUserConfiguration(CassandraMockConfiguration.class)
		.withConfiguration(
				AutoConfigurations.of(CassandraAutoConfiguration.class, CassandraDataAutoConfiguration.class));

	@Test
	void cqlTemplateExists() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(CqlTemplate.class));
	}

	@Test
	void templateExists() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(CassandraTemplate.class));
	}

	@Test
	void templateUsesCqlTemplate() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(CassandraTemplate.class);
			assertThat(context.getBean(CassandraTemplate.class).getCqlOperations())
				.isSameAs(context.getBean(CqlTemplate.class));
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

	@Test
	void codecRegistryShouldBeSet() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(CassandraConverter.class);
			assertThat(context.getBean(CassandraConverter.class).getCodecRegistry())
				.isSameAs(context.getBean(CassandraMockConfiguration.class).codecRegistry);
		});
	}

	@Test
	void defaultConversions() {
		this.contextRunner.run((context) -> {
			CassandraTemplate template = context.getBean(CassandraTemplate.class);
			assertThat(template.getConverter().getConversionService().canConvert(Person.class, String.class)).isFalse();
		});
	}

	@Test
	void customConversions() {
		this.contextRunner.withUserConfiguration(CustomConversionConfig.class).run((context) -> {
			CassandraTemplate template = context.getBean(CassandraTemplate.class);
			assertThat(template.getConverter().getConversionService().canConvert(Person.class, String.class)).isTrue();
		});
	}

	@Test
	void clusterDoesNotExist() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				CassandraDataAutoConfiguration.class)) {
			assertThat(context.getBeansOfType(CqlSession.class)).isEmpty();
		}
	}

	@Configuration(proxyBeanMethods = false)
	@EntityScan("org.springframework.boot.data.cassandra.domain.city")
	static class EntityScanConfig {

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomConversionConfig {

		@Bean
		CassandraCustomConversions myCassandraCustomConversions() {
			return new CassandraCustomConversions(Collections.singletonList(new MyConverter()));
		}

	}

	static class MyConverter implements Converter<Person, String> {

		@Override
		public String convert(Person o) {
			return null;
		}

	}

	static class Person {

	}

}
