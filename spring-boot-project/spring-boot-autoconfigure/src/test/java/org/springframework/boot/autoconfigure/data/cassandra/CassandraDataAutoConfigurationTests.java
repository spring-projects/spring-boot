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

import java.util.Collections;

import com.datastax.oss.driver.api.core.CqlSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.cassandra.CassandraAutoConfiguration;
import org.springframework.boot.autoconfigure.data.cassandra.city.City;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.data.cassandra.core.convert.CassandraConverter;
import org.springframework.data.cassandra.core.convert.CassandraCustomConversions;
import org.springframework.data.cassandra.core.mapping.CassandraMappingContext;
import org.springframework.data.cassandra.core.mapping.SimpleUserTypeResolver;
import org.springframework.data.domain.ManagedTypes;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ObjectUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CassandraDataAutoConfiguration}.
 *
 * @author Eddú Meléndez
 * @author Mark Paluch
 * @author Stephane Nicoll
 */
class CassandraDataAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@AfterEach
	void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	void templateExists() {
		load(CassandraMockConfiguration.class);
		assertThat(this.context.getBeanNamesForType(CassandraTemplate.class)).hasSize(1);
	}

	@Test
	void entityScanShouldSetManagedTypes() {
		load(EntityScanConfig.class);
		CassandraMappingContext mappingContext = this.context.getBean(CassandraMappingContext.class);
		ManagedTypes managedTypes = (ManagedTypes) ReflectionTestUtils.getField(mappingContext, "managedTypes");
		assertThat(managedTypes.toList()).containsOnly(City.class);
	}

	@Test
	void userTypeResolverShouldBeSet() {
		load();
		CassandraConverter cassandraConverter = this.context.getBean(CassandraConverter.class);
		assertThat(cassandraConverter).extracting("userTypeResolver").isInstanceOf(SimpleUserTypeResolver.class);
	}

	@Test
	void codecRegistryShouldBeSet() {
		load();
		CassandraConverter cassandraConverter = this.context.getBean(CassandraConverter.class);
		assertThat(cassandraConverter.getCodecRegistry())
				.isSameAs(this.context.getBean(CassandraMockConfiguration.class).codecRegistry);
	}

	@Test
	void defaultConversions() {
		load();
		CassandraTemplate template = this.context.getBean(CassandraTemplate.class);
		assertThat(template.getConverter().getConversionService().canConvert(Person.class, String.class)).isFalse();
	}

	@Test
	void customConversions() {
		load(CustomConversionConfig.class);
		CassandraTemplate template = this.context.getBean(CassandraTemplate.class);
		assertThat(template.getConverter().getConversionService().canConvert(Person.class, String.class)).isTrue();
	}

	@Test
	void clusterDoesNotExist() {
		this.context = new AnnotationConfigApplicationContext(CassandraDataAutoConfiguration.class);
		assertThat(this.context.getBeansOfType(CqlSession.class)).isEmpty();
	}

	void load(Class<?>... config) {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		TestPropertyValues.of("spring.data.cassandra.keyspaceName:boot_test").applyTo(ctx);
		if (!ObjectUtils.isEmpty(config)) {
			ctx.register(config);
		}
		ctx.register(CassandraMockConfiguration.class, CassandraAutoConfiguration.class,
				CassandraDataAutoConfiguration.class);
		ctx.refresh();
		this.context = ctx;
	}

	@Configuration(proxyBeanMethods = false)
	@EntityScan("org.springframework.boot.autoconfigure.data.cassandra.city")
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
