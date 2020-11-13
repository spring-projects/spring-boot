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
package org.springframework.boot.test.web.reactive.server;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextCustomizer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebTestClientContextCustomizerFactory} when spring webflux is on the
 * classpath.
 *
 * @author Tobias Gesellchen
 */
class WebTestClientContextCustomizerFactoryWithWebfluxTest {

	WebTestClientContextCustomizerFactory contextCustomizerFactory;

	@BeforeEach
	void setup() {
		this.contextCustomizerFactory = new WebTestClientContextCustomizerFactory();
	}

	@Test
	void createContextCustomizer() {
		ContextCustomizer contextCustomizer = this.contextCustomizerFactory.createContextCustomizer(TestClass.class,
				Collections.emptyList());
		assertThat(contextCustomizer).isNotNull();
	}

	@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
	static class TestClass {

	}

}
