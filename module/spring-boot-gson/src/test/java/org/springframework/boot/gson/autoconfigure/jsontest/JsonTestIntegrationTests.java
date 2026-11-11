/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.gson.autoconfigure.jsontest;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.gson.autoconfigure.jsontest.app.ExampleBasicObject;
import org.springframework.boot.gson.autoconfigure.jsontest.app.ExampleJsonApplication;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.GsonTester;
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
	private GsonTester<ExampleBasicObject> gsonJson;

	@Test
	void gson() throws Exception {
		ExampleBasicObject object = new ExampleBasicObject();
		object.setValue("spring");
		assertThat(this.gsonJson.write(object)).isEqualToJson("example.json");
	}

}
