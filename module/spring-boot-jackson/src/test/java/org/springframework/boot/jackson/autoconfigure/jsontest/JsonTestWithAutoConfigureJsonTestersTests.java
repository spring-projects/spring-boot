/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jackson.autoconfigure.jsontest;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jackson.autoconfigure.jsontest.app.ExampleBasicObject;
import org.springframework.boot.jackson.autoconfigure.jsontest.app.ExampleJsonApplication;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link JsonTest @JsonTest} with
 * {@link AutoConfigureJsonTesters @AutoConfigureJsonTesters}.
 *
 * @author Phillip Webb
 */
@JsonTest
@AutoConfigureJsonTesters(enabled = false)
@ContextConfiguration(classes = ExampleJsonApplication.class)
class JsonTestWithAutoConfigureJsonTestersTests {

	@Autowired(required = false)
	private JacksonTester<ExampleBasicObject> jacksonTester;

	@Test
	void jackson() {
		assertThat(this.jacksonTester).isNull();
	}

}
