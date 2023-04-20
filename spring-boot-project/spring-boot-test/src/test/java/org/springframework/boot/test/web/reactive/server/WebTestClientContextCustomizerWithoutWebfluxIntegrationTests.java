/*
 * Copyright 2012-2021 the original author or authors.
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

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testsupport.classpath.ClassPathExclusions;
import org.springframework.test.context.ContextCustomizer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebTestClientContextCustomizerFactory} when spring webflux is not on
 * the classpath.
 *
 * @author Tobias Gesellchen
 * @author Stephane Nicoll
 */
@ClassPathExclusions("spring-webflux*.jar")
class WebTestClientContextCustomizerWithoutWebfluxIntegrationTests {

	@Test
	void customizerIsNotCreatedWithoutWebClient() {
		WebTestClientContextCustomizerFactory contextCustomizerFactory = new WebTestClientContextCustomizerFactory();
		ContextCustomizer contextCustomizer = contextCustomizerFactory.createContextCustomizer(TestClass.class,
				Collections.emptyList());
		assertThat(contextCustomizer).isNull();
	}

	@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
	private static class TestClass {

	}

}
