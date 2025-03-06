/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.mustache;

import java.util.Collections;

import com.samskivert.mustache.Mustache;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration Tests for {@link MustacheAutoConfiguration} outside of a web application.
 *
 * @author Dave Syer
 */
@DirtiesContext
@SpringBootTest(webEnvironment = WebEnvironment.NONE)
class MustacheStandaloneIntegrationTests {

	@Autowired
	private Mustache.Compiler compiler;

	@Test
	void directCompilation() {
		assertThat(this.compiler.compile("Hello: {{world}}").execute(Collections.singletonMap("world", "World")))
			.isEqualTo("Hello: World");
	}

	@Configuration(proxyBeanMethods = false)
	@Import({ MustacheAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class })
	static class Application {

	}

}
