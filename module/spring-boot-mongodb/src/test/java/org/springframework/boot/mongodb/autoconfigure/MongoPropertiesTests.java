/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.mongodb.autoconfigure;

import org.bson.UuidRepresentation;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.mongodb.config.MongoConfigurationSupport;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MongoProperties}.
 *
 * @author Andy Wilkinson
 */
class MongoPropertiesTests {

	@Test
	void defaultUuidRepresentationIsAlignedWithSpringData() {
		UuidRepresentation springDataDefault = springDataDefaultUuidRepresentation();
		UuidRepresentation springBootDefault = new MongoProperties().getRepresentation().getUuid();
		assertThat(springBootDefault).isEqualTo(springDataDefault);
	}

	@Test
	void canBindCharArrayPassword() {
		new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(MongoAutoConfiguration.class))
			.withPropertyValues("spring.mongodb.password:word")
			.run((context) -> {
				MongoProperties properties = context.getBean(MongoProperties.class);
				assertThat(properties.getPassword()).isEqualTo("word".toCharArray());
			});
	}

	@Test
	void sslIsNotEnabledWhenBundleIsEmpty() {
		MongoProperties properties = new MongoProperties();
		properties.getSsl().setBundle("");
		assertThat(properties.getSsl().isEnabled()).isFalse();
	}

	private UuidRepresentation springDataDefaultUuidRepresentation() {
		return new MongoConfigurationSupport() {

			@Override
			protected String getDatabaseName() {
				return "test";
			}

			UuidRepresentation defaultUuidRepresentation() {
				return mongoClientSettings().getUuidRepresentation();
			}

		}.defaultUuidRepresentation();
	}

}
