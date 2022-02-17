/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.test.autoconfigure;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.PostConstruct;
import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link SpringBootDependencyInjectionTestExecutionListener}.
 *
 * @author Phillip Webb
 */
@SpringBootTest
class SpringBootDependencyInjectionTestExecutionListenerPostConstructIntegrationTests {

	private List<String> calls = new ArrayList<>();

	@PostConstruct
	void postConstruct() {
		StringWriter writer = new StringWriter();
		new RuntimeException().printStackTrace(new PrintWriter(writer));
		this.calls.add(writer.toString());
	}

	@Test
	void postConstructShouldBeInvokedOnlyOnce() {
		// gh-6874
		assertThat(this.calls).hasSize(1);
	}

	@Configuration(proxyBeanMethods = false)
	static class Config {

	}

}
