/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.rsocket.context;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LocalRSocketServerPort @LocalRSocketServerPort}.
 *
 * @author Verónica Vásquez
 * @author Eddú Meléndez
 */
@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = "local.rsocket.server.port=8181")
class LocalRSocketServerPortTests {

	@Value("${local.rsocket.server.port}")
	private String fromValue;

	@LocalRSocketServerPort
	private String fromAnnotation;

	@Test
	void testLocalRSocketServerPortAnnotation() {
		assertThat(this.fromAnnotation).isNotNull().isEqualTo(this.fromValue);
	}

	@Configuration(proxyBeanMethods = false)
	static class Config {

	}

}
