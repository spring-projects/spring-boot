/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webtestclient.autoconfigure;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.http.codec.autoconfigure.CodecsAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.autoconfigure.AutoConfigurationImportedCondition.importedAutoConfiguration;

/**
 * Integration tests for {@link AutoConfigureWebTestClient @AutoConfigureWebTestClient}.
 *
 * @author Andy Wilkinson
 */
@SpringBootTest
@AutoConfigureWebTestClient
class AutoConfigureWebTestClientIntegrationTests {

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void codecsAutoConfigurationIsImported() {
		assertThat(this.applicationContext).has(importedAutoConfiguration(CodecsAutoConfiguration.class));
	}

	@SpringBootConfiguration
	static class TestConfiguration {

	}

}
