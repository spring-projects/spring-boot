/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.context;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest.UseMainMethod;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SpringBootTest} when using {@link UseMainMethod#ALWAYS} and setting
 * properties.
 *
 * @author Phillip Webb
 */
@SpringBootTest(properties = "test=123", useMainMethod = UseMainMethod.ALWAYS)
class SpringBootTestUseMainMethodWithPropertiesTests {

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void propertyIsSet() {
		assertThat(this.applicationContext.getEnvironment().getProperty("test")).isEqualTo("123");
	}

	@SpringBootConfiguration(proxyBeanMethods = false)
	public static class ConfigWithMain {

		public static void main(String[] args) {
			new SpringApplication(ConfigWithMain.class).run();
		}

	}

}
