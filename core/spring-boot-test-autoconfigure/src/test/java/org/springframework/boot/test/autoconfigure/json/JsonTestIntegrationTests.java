/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.autoconfigure.json;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.json.test.autoconfigure.app.ExampleJsonApplication;
import org.springframework.boot.test.json.BasicJsonTester;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link JsonTest @JsonTest}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author Eddú Meléndez
 */
@JsonTest
@ContextConfiguration(classes = ExampleJsonApplication.class)
class JsonTestIntegrationTests {

	@Autowired
	private BasicJsonTester basicJson;

	@Test
	void basicJson() {
		assertThat(this.basicJson.from("{\"a\":\"b\"}")).hasJsonPathStringValue("@.a");
	}

}
