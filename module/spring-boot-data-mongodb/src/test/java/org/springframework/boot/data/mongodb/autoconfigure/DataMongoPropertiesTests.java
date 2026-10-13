/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.mongodb.autoconfigure;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions.BigDecimalRepresentation;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions.MongoConverterConfigurationAdapter;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DataMongoProperties}.
 *
 * @author Andy Wilkinson
 */
class DataMongoPropertiesTests {

	@Test
	void canBindAutoIndexCreation() {
		new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(DataMongoAutoConfiguration.class))
			.withPropertyValues("spring.data.mongodb.auto-index-creation=true")
			.run((context) -> {
				DataMongoProperties properties = context.getBean(DataMongoProperties.class);
				assertThat(properties.isAutoIndexCreation()).isTrue();
			});
	}

	@Test
	void defaultBigDecimalRepresentationIsAlignedWithSpringData() {
		BigDecimalRepresentation springDataDefault = springDataDefaultBigDecimalRepresentation();
		BigDecimalRepresentation springBootDefault = new DataMongoProperties().getRepresentation().getBigDecimal();
		assertThat(springBootDefault).isEqualTo(springDataDefault);
	}

	private BigDecimalRepresentation springDataDefaultBigDecimalRepresentation() {
		Object field = ReflectionTestUtils.getField(new MongoConverterConfigurationAdapter(), "bigDecimals");
		assertThat(field).isNotNull();
		return (BigDecimalRepresentation) field;
	}

}
