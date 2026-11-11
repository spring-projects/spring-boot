/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.elasticsearch.test.autoconfigure;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the {@link DataElasticsearchTest#properties properties} attribute of
 * {@link DataElasticsearchTest @DataElasticsearchTest}.
 *
 * @author Eddú Meléndez
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
@DataElasticsearchTest(properties = "spring.profiles.active=test")
@Testcontainers(disabledWithoutDocker = true)
class DataElasticsearchTestPropertiesIntegrationTests {

	@Container
	@ServiceConnection
	static final ElasticsearchContainer elasticsearch = TestImage.ELASTICSEARCH.container();

	@Autowired
	private Environment environment;

	@Test
	void environmentWithNewProfile() {
		assertThat(this.environment.getActiveProfiles()).containsExactly("test");
	}

	@Nested
	class NestedTests {

		@Autowired
		private Environment innerEnvironment;

		@Test
		void propertiesFromEnclosingClassAffectNestedTests() {
			assertThat(DataElasticsearchTestPropertiesIntegrationTests.this.environment.getActiveProfiles())
				.containsExactly("test");
			assertThat(this.innerEnvironment.getActiveProfiles()).containsExactly("test");
		}

	}

}
