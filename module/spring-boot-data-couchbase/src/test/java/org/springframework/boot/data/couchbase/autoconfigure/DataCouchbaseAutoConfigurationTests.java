/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.couchbase.autoconfigure;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.couchbase.autoconfigure.CouchbaseAutoConfiguration;
import org.springframework.boot.couchbase.autoconfigure.CouchbaseProperties;
import org.springframework.boot.data.couchbase.domain.city.City;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.couchbase.config.BeanNames;
import org.springframework.data.couchbase.core.CouchbaseTemplate;
import org.springframework.data.couchbase.core.convert.CouchbaseCustomConversions;
import org.springframework.data.couchbase.core.convert.DefaultCouchbaseTypeMapper;
import org.springframework.data.couchbase.core.convert.MappingCouchbaseConverter;
import org.springframework.data.couchbase.core.mapping.CouchbaseMappingContext;
import org.springframework.data.couchbase.core.mapping.event.ValidatingCouchbaseEventListener;
import org.springframework.data.domain.ManagedTypes;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DataCouchbaseAutoConfiguration}.
 *
 * @author Stephane Nicoll
 */
class DataCouchbaseAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class, CouchbaseAutoConfiguration.class,
				DataCouchbaseAutoConfiguration.class));

	@Test
	void disabledIfCouchbaseIsNotConfigured() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(CouchbaseTemplate.class));
	}

	@Test
	void validatorIsPresent() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(ValidatingCouchbaseEventListener.class));
	}

	@Test
	void entityScanShouldSetInitialEntitySet() {
		this.contextRunner.withUserConfiguration(EntityScanConfig.class).run((context) -> {
			CouchbaseMappingContext mappingContext = context.getBean(CouchbaseMappingContext.class);
			ManagedTypes managedTypes = (ManagedTypes) ReflectionTestUtils.getField(mappingContext, "managedTypes");
			assertThat(managedTypes).isNotNull();
			assertThat(managedTypes.toList()).containsOnly(City.class);
		});
	}

	@Test
	void typeKeyDefault() {
		this.contextRunner.withUserConfiguration(CouchbaseMockConfiguration.class)
			.run((context) -> assertThat(context.getBean(MappingCouchbaseConverter.class).getTypeKey())
				.isEqualTo(DefaultCouchbaseTypeMapper.DEFAULT_TYPE_KEY));
	}

	@Test
	void typeKeyCanBeCustomized() {
		this.contextRunner.withUserConfiguration(CouchbaseMockConfiguration.class)
			.withPropertyValues("spring.data.couchbase.type-key=_custom")
			.run((context) -> assertThat(context.getBean(MappingCouchbaseConverter.class).getTypeKey())
				.isEqualTo("_custom"));
	}

	@Test
	void customConversions() {
		this.contextRunner.withUserConfiguration(CustomConversionsConfig.class).run((context) -> {
			CouchbaseTemplate template = context.getBean(CouchbaseTemplate.class);
			assertThat(
					template.getConverter().getConversionService().canConvert(CouchbaseProperties.class, Boolean.class))
				.isTrue();
		});
	}

	@Configuration(proxyBeanMethods = false)
	@Import(CouchbaseMockConfiguration.class)
	static class CustomConversionsConfig {

		@Bean(BeanNames.COUCHBASE_CUSTOM_CONVERSIONS)
		CouchbaseCustomConversions myCustomConversions() {
			return new CouchbaseCustomConversions(Collections.singletonList(new MyConverter()));
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EntityScan(basePackageClasses = City.class)
	@Import(CouchbaseMockConfiguration.class)
	static class EntityScanConfig {

	}

	static class MyConverter implements Converter<CouchbaseProperties, Boolean> {

		@Override
		public Boolean convert(CouchbaseProperties value) {
			return true;
		}

	}

}
