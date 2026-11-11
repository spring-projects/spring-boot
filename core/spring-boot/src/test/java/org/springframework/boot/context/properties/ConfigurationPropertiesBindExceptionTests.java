/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.properties;

import org.junit.jupiter.api.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Component;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConfigurationPropertiesBindException}.
 *
 * @author Phillip Webb
 */
class ConfigurationPropertiesBindExceptionTests {

	@Test
	void createFromBeanHasDetails() {
		ApplicationContext applicationContext = new AnnotationConfigApplicationContext(Example.class);
		ConfigurationPropertiesBean bean = ConfigurationPropertiesBean.get(applicationContext,
				applicationContext.getBean(Example.class), "example");
		assertThat(bean).isNotNull();
		ConfigurationPropertiesBindException exception = new ConfigurationPropertiesBindException(bean,
				new IllegalStateException());
		assertThat(exception.getMessage()).isEqualTo("Error creating bean with name 'example': "
				+ "Could not bind properties to 'ConfigurationPropertiesBindExceptionTests.Example' : "
				+ "prefix=, ignoreInvalidFields=false, ignoreUnknownFields=true");
		assertThat(exception.getBeanType()).isEqualTo(Example.class);
		assertThat(exception.getBeanName()).isEqualTo("example");
		assertThat(exception.getAnnotation()).isInstanceOf(ConfigurationProperties.class);
		assertThat(exception.getCause()).isInstanceOf(IllegalStateException.class);
	}

	@Component("example")
	@ConfigurationProperties
	static class Example {

	}

}
