/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.webflux.test.autoconfigure;

import java.util.Locale;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link WebFluxTest @WebFluxTest} and {@link MessageSource}
 * auto-configuration.
 *
 * @author Stephane Nicoll
 */
@WebFluxTest
@TestPropertySource(properties = "spring.messages.basename=web-test-messages")
class WebFluxTestMessageSourceIntegrationTests {

	@Autowired
	private ApplicationContext context;

	@Test
	void messageSourceHasBeenAutoConfigured() {
		assertThat(this.context.getMessage("a", null, Locale.ENGLISH)).isEqualTo("alpha");
	}

}
