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
	void defaultUUidRepresentationIsAlignedWithSpringData() {
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
