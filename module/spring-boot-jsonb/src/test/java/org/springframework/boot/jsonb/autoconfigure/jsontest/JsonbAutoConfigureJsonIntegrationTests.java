/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jsonb.autoconfigure.jsontest;

import jakarta.json.bind.Jsonb;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Integration tests for {@link AutoConfigureJson @AutoConfigureJson} with JSON-B.
 *
 * @author Andy Wilkinson
 */
@AutoConfigureJson
@ExtendWith(SpringExtension.class)
class JsonbAutoConfigureJsonIntegrationTests {

	@Autowired
	private ApplicationContext context;

	@Test
	void jsonbIsAvailable() {
		assertThatNoException().isThrownBy(() -> this.context.getBean(Jsonb.class));
	}

}
