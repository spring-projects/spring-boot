/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jackson2;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AutoConfigureJson} with Jackson 2.
 *
 * @author Moritz Halbritter
 * @deprecated since 4.0.0 for removal in 4.3.0 in favor of Jackson 3
 */
@AutoConfigureJson
@ExtendWith(SpringExtension.class)
@Deprecated(since = "4.0.0", forRemoval = true)
class AutoConfigureJsonTests {

	@Autowired
	private ObjectProvider<ObjectMapper> objectMapper;

	@Test
	void shouldAutowireObjectMapper() {
		assertThat(this.objectMapper.getIfAvailable()).isNotNull();
	}

}
