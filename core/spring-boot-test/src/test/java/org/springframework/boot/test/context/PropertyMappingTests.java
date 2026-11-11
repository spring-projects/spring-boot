/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.context;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link PropertyMapping @PropertyMapping} annotations.
 *
 * @author Phillip Webb
 */
@ExtendWith(SpringExtension.class)
@ExampleMapping(exampleProperty = "abc")
class PropertyMappingTests {

	@Autowired
	private Environment environment;

	@Test
	void hasProperty() {
		assertThat(this.environment.getProperty("example-property")).isEqualTo("abc");
	}

}
