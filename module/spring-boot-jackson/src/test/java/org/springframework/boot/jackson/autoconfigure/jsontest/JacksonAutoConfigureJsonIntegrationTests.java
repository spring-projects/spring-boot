/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jackson.autoconfigure.jsontest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Integration tests for {@link AutoConfigureJson @AutoConfigureJson} with Jackson.
 *
 * @author Andy Wilkinson
 */
@AutoConfigureJson
@ExtendWith(SpringExtension.class)
class JacksonAutoConfigureJsonIntegrationTests {

	@Autowired
	private ApplicationContext context;

	@Test
	void jsonMapperIsAvailable() {
		assertThatNoException().isThrownBy(() -> this.context.getBean(JsonMapper.class));
	}

}
