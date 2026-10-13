/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jsonb.autoconfigure;

import jakarta.json.bind.Jsonb;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testsupport.classpath.ClassPathExclusions;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JsonbAutoConfiguration} when there is no provider available.
 *
 * @author Andy Wilkinson
 */
@ClassPathExclusions("yasson-*.jar")
class JsonbAutoConfigurationWithNoProviderTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(JsonbAutoConfiguration.class));

	@Test
	void jsonbBacksOffWhenThereIsNoProvider() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(Jsonb.class));
	}

}
