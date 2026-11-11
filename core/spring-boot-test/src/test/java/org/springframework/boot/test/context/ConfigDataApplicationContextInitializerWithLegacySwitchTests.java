/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.context;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConfigDataApplicationContextInitializer}.
 *
 * @author Phillip Webb
 */
@ExtendWith(SpringExtension.class)
@DirtiesContext
@TestPropertySource(properties = "spring.config.use-legacy-processing=true")
@ContextConfiguration(classes = ConfigDataApplicationContextInitializerWithLegacySwitchTests.Config.class,
		initializers = ConfigDataApplicationContextInitializer.class)
class ConfigDataApplicationContextInitializerWithLegacySwitchTests {

	@Autowired
	private Environment environment;

	@Test
	void initializerPopulatesEnvironment() {
		assertThat(this.environment.getProperty("foo")).isEqualTo("bucket");
	}

	@Configuration(proxyBeanMethods = false)
	static class Config {

	}

}
