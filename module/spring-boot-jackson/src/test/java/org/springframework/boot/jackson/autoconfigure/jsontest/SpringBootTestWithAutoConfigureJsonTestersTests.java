/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jackson.autoconfigure.jsontest;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jackson.autoconfigure.jsontest.app.ExampleBasicObject;
import org.springframework.boot.jackson.autoconfigure.jsontest.app.ExampleJsonApplication;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link SpringBootTest @SpringBootTest} with
 * {@link AutoConfigureJsonTesters @AutoConfigureJsonTesters}.
 *
 * @author Andy Wilkinson
 */
@SpringBootTest
@AutoConfigureJsonTesters
@ContextConfiguration(classes = ExampleJsonApplication.class)
class SpringBootTestWithAutoConfigureJsonTestersTests {

	@Autowired
	private JacksonTester<ExampleBasicObject> jacksonTester;

	@Test
	void contextLoads() {
		assertThat(this.jacksonTester).isNotNull();
	}

}
